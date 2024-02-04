package catchup.gemoji

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import timber.log.Timber

/**
 * An open helper that will copy & open a pre-populated database if it doesn't exists in internal
 * storage.
 */
@Suppress("BanSynchronizedMethods")
internal class SQLiteCopyOpenHelper(
  private val context: Context,
  private val copyFromAssetPath: String,
  private val databaseVersion: Int,
  val delegate: SupportSQLiteOpenHelper,
) : SupportSQLiteOpenHelper {
  private var verified = false

  override val databaseName: String?
    get() = delegate.databaseName

  override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
    delegate.setWriteAheadLoggingEnabled(enabled)
  }

  override val writableDatabase: SupportSQLiteDatabase
    get() {
      if (!verified) {
        verifyDatabaseFile()
        verified = true
      }
      return delegate.writableDatabase
    }

  override val readableDatabase: SupportSQLiteDatabase
    get() {
      if (!verified) {
        verifyDatabaseFile()
        verified = true
      }
      return delegate.readableDatabase
    }

  @Synchronized
  override fun close() {
    delegate.close()
    verified = false
  }

  private fun verifyDatabaseFile() {
    val name = checkNotNull(databaseName) { "Database name was not set yet" }
    val databaseFile = context.getDatabasePath(name)
    val copyLock = ProcessLock(name, context.filesDir, true)
    try {
      // Acquire a copy lock, this lock works across threads and processes, preventing
      // concurrent copy attempts from occurring.
      copyLock.lock()
      if (!databaseFile.exists()) {
        try {
          // No database file found, copy and be done.
          copyDatabaseFile(databaseFile)
          return
        } catch (e: IOException) {
          throw RuntimeException("Unable to copy database file.", e)
        }
      }

      // A database file is present, check if we need to re-copy it.
      val currentVersion =
        try {
          readVersion(databaseFile)
        } catch (e: IOException) {
          Timber.w(e, "Unable to read database version.")
          return
        }
      if (currentVersion == databaseVersion) {
        return
      }
      if (isMigrationRequired(currentVersion, databaseVersion)) {
        // From the current version to the desired version a migration is required, i.e.
        // we won't be performing a copy destructive migration.
        return
      }
      if (context.deleteDatabase(name)) {
        try {
          copyDatabaseFile(databaseFile)
        } catch (e: IOException) {
          // We are more forgiving copying a database on a destructive migration since
          // there is already a database file that can be opened.
          Timber.w(e, "Unable to copy database file.")
        }
      } else {
        Timber.w("Failed to delete database file ($name) for a copy destructive migration.")
      }
    } finally {
      copyLock.unlock()
    }
  }

  private fun copyDatabaseFile(destinationFile: File) {
    val input: ReadableByteChannel = Channels.newChannel(context.assets.open(copyFromAssetPath))

    // An intermediate file is used so that we never end up with a half-copied database file
    // in the internal directory.
    val intermediateFile = File.createTempFile("room-copy-helper", ".tmp", context.cacheDir)
    intermediateFile.deleteOnExit()
    val output = FileOutputStream(intermediateFile).channel
    copy(input, output)
    val parent = destinationFile.parentFile
    if (parent != null && !parent.exists() && !parent.mkdirs()) {
      throw IOException("Failed to create directories for ${destinationFile.absolutePath}")
    }

    // Temporarily open intermediate database file using FrameworkSQLiteOpenHelper and dispatch
    // the open pre-packaged callback. If it fails then intermediate file won't be copied making
    // invoking pre-packaged callback a transactional operation.
    if (!intermediateFile.renameTo(destinationFile)) {
      throw IOException(
        "Failed to move intermediate file (${intermediateFile.absolutePath}) to " +
          "destination (${destinationFile.absolutePath})."
      )
    }
  }
}

/**
 * Reads the user version number out of the database header from the given file.
 *
 * @param databaseFile the database file.
 * @return the database version
 * @throws IOException if something goes wrong reading the file, such as bad database header or
 *   missing permissions.
 * @see [User Version Number](https://www.sqlite.org/fileformat.html.user_version_number).
 */
private fun readVersion(databaseFile: File): Int {
  FileInputStream(databaseFile).channel.use { input ->
    val buffer = ByteBuffer.allocate(4)
    input.tryLock(60, 4, true)
    input.position(60)
    val read = input.read(buffer)
    if (read != 4) {
      throw IOException("Bad database header, unable to read 4 bytes at offset 60")
    }
    buffer.rewind()
    return buffer.int // ByteBuffer is big-endian by default
  }
}

private fun isMigrationRequired(fromVersion: Int, toVersion: Int): Boolean {
  // Migrations are not required if its a downgrade AND destructive migration during downgrade
  // has been allowed.
  val isDowngrade = fromVersion > toVersion
  return !isDowngrade
}

/**
 * Copies data from the input channel to the output file channel.
 *
 * @param input the input channel to copy.
 * @param output the output channel to copy.
 * @throws IOException if there is an I/O error.
 */
private fun copy(input: ReadableByteChannel, output: FileChannel) {
  try {
    output.transferFrom(input, 0, Long.MAX_VALUE)
    output.force(false)
  } finally {
    input.close()
    output.close()
  }
}
