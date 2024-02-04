/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.util.io

import android.util.Log
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import okio.BufferedSink
import okio.BufferedSource
import okio.FileHandle
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.jetbrains.annotations.VisibleForTesting

/**
 * Helper class for performing atomic operations on a file by writing to a new file and renaming it
 * into the place of the original file after the write has successfully completed.
 *
 * Atomic file guarantees file integrity by ensuring that a file has been completely written and
 * sync'd to disk before renaming it to the original file. Previously this is done by renaming the
 * original file to a backup file beforehand, but this approach couldn't handle the case where the
 * file is created for the first time. This class will also handle the backup file created by the
 * old implementation properly.
 *
 * Atomic file does not confer any file locking semantics. Do not use this class when the file may
 * be accessed or modified concurrently by multiple threads or processes. The caller is responsible
 * for ensuring appropriate mutual exclusion invariants whenever it accesses the file.
 */
@Suppress("LogNotTimber")
class AtomicFile
/**
 * Create a new AtomicFile for a file located at the given File path. The new file created when
 * writing will be the same file path with ".new" appended.
 */
constructor(
  /**
   * Return the path to the base file. You should not generally use this, as the data at that path
   * may not be valid.
   */
  val baseFile: Path,
  private val fs: FileSystem = FileSystem.SYSTEM,
  private val setPosixPermissions: Boolean = true,
  private val logError: ((String, Throwable?) -> Unit) = { message, throwable ->
    Log.e(LOG_TAG, message, throwable)
  },
) {
  private val newName = "$baseFile.new".toPath()

  /** Delete the atomic file. This deletes both the base and new files. */
  fun delete() {
    fs.delete(baseFile)
    fs.delete(newName)
  }

  /**
   * Start a new write operation on the file. This returns a [FileHandle] to which you can write the
   * new file data. The existing file is replaced with the new data. You *must not* directly close
   * the given [FileHandle]; instead call either [finishWrite] or [failWrite].
   *
   * Note that if another thread is currently performing a write, this will simply replace whatever
   * that thread is writing with the new file being written by this thread, and when the other
   * thread finishes the write the new write operation will no longer be safe (or will be lost). You
   * must do your own threading protection for access to AtomicFile.
   *
   * Note that when you call [FileHandle.sink] or [FileHandle.appendingSink] on the returned handle,
   * you _must_ close it before calling [finishWrite] or [failWrite].
   */
  @VisibleForTesting
  internal fun startWrite(append: Boolean = false): FileHandle {
    return try {
      // If we're appending, copy the existing file comments over first
      if (append && fs.exists(baseFile)) {
        fs.copy(baseFile, newName)
      }
      fs
        .openReadWrite(newName)
        // Okio doesn't truncate opening a file for writing, so we need to resize the file instead
        // to replicate this. https://github.com/square/okio/issues/514
        .also {
          if (!append) {
            it.resize(0L)
          }
        }
    } catch (e: IOException) {
      val parent = newName.parent ?: error("Couldn't find a parent directory for $newName")
      fs.createDirectories(parent)
      if (!fs.exists(parent)) {
        throw IOException("Failed to create directory for $newName")
      }
      if (setPosixPermissions) {
        // Set perms to 00771
        Files.setPosixFilePermissions(
          parent.toNioPath(),
          setOf(
            // Owner
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            // Group
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
            // Other
            PosixFilePermission.OTHERS_EXECUTE,
          ),
        )
      }
      try {
        fs
          .openReadWrite(newName)
          // Okio doesn't truncate opening a file for writing, so we need to resize the file instead
          // to replicate this. https://github.com/square/okio/issues/514
          .also {
            if (!append) {
              it.resize(0L)
            }
          }
      } catch (e2: IOException) {
        throw IOException("Failed to create new file $newName", e2)
      }
    }
  }

  /**
   * Perform an fsync/flush on the given [FileHandle]. The handle at this point must be flushed but
   * not yet closed.
   */
  private fun sync(handle: FileHandle): Boolean {
    try {
      handle.flush()
      return true
    } catch (_: IOException) {}
    return false
  }

  /**
   * Call when you have successfully finished writing to the handle returned by [startWrite]. This
   * will close, sync, and commit the new data. The next attempt to read the atomic file will return
   * the new file handle.
   */
  @VisibleForTesting
  internal fun finishWrite(handle: FileHandle?) {
    if (handle == null) {
      return
    }
    if (!sync(handle)) {
      logError("Failed to sync file handle", null)
    }
    try {
      handle.close()
    } catch (e: IOException) {
      logError("Failed to close file handle", e)
    }
    rename(newName, baseFile)
  }

  /**
   * Call when you have failed for some reason at writing to the handle returned by [startWrite].
   * This will close the current write handle, and delete the new file.
   */
  @VisibleForTesting
  internal fun failWrite(handle: FileHandle?) {
    if (handle == null) {
      return
    }
    if (!sync(handle)) {
      logError("Failed to sync file handle", null)
    }
    try {
      handle.close()
    } catch (e: IOException) {
      logError("Failed to close file handle", e)
    }
    fs.delete(newName)
  }

  /**
   * Open the atomic file for reading. You should call close() on the [FileHandle] when you are done
   * reading from it.
   *
   * You must do your own threading protection for access to AtomicFile.
   */
  // TODO just return a sink instead?
  @VisibleForTesting
  private fun openRead(): FileHandle {
    // It was okay to call openRead() between startWrite() and finishWrite() for the first time
    // (because there is no backup file), where openRead() would open the file being written,
    // which makes no sense, but finishWrite() would still persist the write properly. For all
    // subsequent writes, if openRead() was called in between, it would see a backup file and
    // delete the file being written, the same behavior as our new implementation. So we only
    // need a special case for the first write, and don't delete the new file in this case so
    // that finishWrite() can still work.
    if (fs.exists(newName) && fs.exists(baseFile)) {
      fs.delete(newName)
    }
    return fs.openReadOnly(baseFile)
  }

  fun <T> read(block: BufferedSource.() -> T): T {
    return openRead().use { it.source().buffer().use(block) }
  }

  /** @return whether the original file exists. */
  fun exists(): Boolean {
    return fs.exists(baseFile)
  }

  /**
   * Returns the last modified time of the atomic file in milliseconds since epoch. Returns zero if
   * the file does not exist or an I/O error is encountered.
   */
  val lastModifiedTime: Long
    get() = fs.metadataOrNull(baseFile)?.lastModifiedAtMillis ?: 0

  /**
   * Gets the entire content of this file as a byte array.
   *
   * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
   */
  fun readByteArray(): ByteArray {
    return read { readByteArray() }
  }

  /**
   * Perform the write operations inside [block] on this file. If [block] throws an exception the
   * write will be failed. Otherwise the write will be applied atomically to the file.
   */
  fun tryWrite(append: Boolean = false, block: (BufferedSink) -> Unit) {
    val handle = startWrite(append)
    val sink = if (append) handle.appendingSink() else handle.sink()
    var success = false
    try {
      sink.buffer().use {
        block(it)
        success = true
      }
    } catch (t: Throwable) {
      throw propagate(t)
    } finally {
      // Both of the below call close()
      if (success) {
        finishWrite(handle)
      } else {
        failWrite(handle)
      }
    }
  }

  private fun propagate(t: Throwable): Throwable {
    if (t is Error) return t
    if (t is RuntimeException) return t
    if (t is IOException) return t
    return RuntimeException(t)
  }

  override fun toString(): String {
    return "AtomicFile[$baseFile]"
  }

  private fun rename(source: Path, target: Path) {
    // We used to delete the target file before rename, but that isn't atomic, and the rename()
    // syscall should atomically replace the target file. However in the case where the target
    // file is a directory, a simple rename() won't work. We need to delete the file in this
    // case because there are callers who erroneously called mBaseName.mkdirs() (instead of
    // mBaseName.getParentFile().mkdirs()) before creating the AtomicFile, and it worked
    // regardless, so this deletion became some kind of API.
    if (fs.metadataOrNull(target)?.isDirectory == true) {
      fs.deleteRecursively(target)
      if (fs.exists(target)) {
        logError("Failed to delete file which is a directory $target", null)
      }
    }
    fs.atomicMove(source, target)
  }

  companion object {
    const val LOG_TAG = "AtomicFile"
  }
}

/** Sets the content of this file as an [array] of bytes. */
fun AtomicFile.writeBytes(array: ByteArray) {
  tryWrite { it.write(array) }
}

/**
 * Sets the content of this file as [text] encoded using UTF-8 or specified [charset]. If this file
 * exists, it becomes overwritten.
 */
fun AtomicFile.writeText(text: String, charset: Charset = Charsets.UTF_8) {
  writeBytes(text.toByteArray(charset))
}

/**
 * Gets the entire content of this file as a String using UTF-8 or specified [charset].
 *
 * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
 */
fun AtomicFile.readText(charset: Charset = Charsets.UTF_8): String {
  return readByteArray().toString(charset)
}
