package catchup.gemoji

import androidx.annotation.RestrictTo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import timber.log.Timber

/**
 * Utility class for in-process and multi-process key-based lock mechanism for safely doing
 * synchronized operations.
 *
 * Acquiring the lock will be quick if no other thread or process has a lock with the same key. But
 * if the lock is already held then acquiring it will block, until the other thread or process
 * releases the lock. Note that the key and lock directory must be the same to achieve
 * synchronization.
 *
 * Locking is done via two levels:
 * 1. Thread locking within the same JVM process is done via a map of String key to ReentrantLock
 *    objects.
 * 2. Multi-process locking is done via a lock file whose name contains the key and FileLock
 *    objects.
 *
 * Creates a lock with `name` and using `lockDir` as the directory for the lock files.
 *
 * @param name the name of this lock.
 * @param lockDir the directory where the lock files will be located.
 * @param processLock whether to use file for process level locking or not by default. The behaviour
 *   can be overridden via the [lock] method.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ProcessLock(name: String, lockDir: File, private val processLock: Boolean) {
  private val lockFile: File = File(lockDir, "$name.lck")
  private val threadLock: Lock = getThreadLock(lockFile.absolutePath)
  private var lockChannel: FileChannel? = null

  /**
   * Attempts to grab the lock, blocking if already held by another thread or process.
   *
   * @param [processLock] whether to use file for process level locking or not.
   */
  fun lock(processLock: Boolean = this.processLock) {
    threadLock.lock()
    if (processLock) {
      try {
        // Verify parent dir
        val parentDir = lockFile.parentFile
        parentDir?.mkdirs()
        lockChannel = FileOutputStream(lockFile).channel.apply { lock() }
      } catch (e: IOException) {
        lockChannel = null
        Timber.tag(TAG).w(e, "Unable to grab file lock.")
      }
    }
  }

  /** Releases the lock. */
  fun unlock() {
    try {
      lockChannel?.close()
    } catch (ignored: IOException) {}
    threadLock.unlock()
  }

  companion object {
    private const val TAG = "SupportSQLiteLock"
    // in-process lock map
    private val threadLocksMap: MutableMap<String, Lock> = HashMap()

    private fun getThreadLock(key: String): Lock =
      synchronized(threadLocksMap) {
        return threadLocksMap.getOrPut(key) { ReentrantLock() }
      }
  }
}
