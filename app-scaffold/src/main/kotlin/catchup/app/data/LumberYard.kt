/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.app.data

import android.app.Application
import android.util.Log
import androidx.annotation.WorkerThread
import catchup.app.util.BackgroundAppCoroutineScope
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.util.io.AtomicFile
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import me.saket.filesize.FileSize.Companion.bytes
import me.saket.filesize.FileSize.Companion.megabytes
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.buffer
import timber.log.Timber

// TODO
//  disable flushing in background?
@SingleIn(AppScope::class)
class LumberYard(
  private val _logDir: Path,
  scope: CoroutineScope,
  private val flushInterval: Duration = FLUSH_INTERVAL,
  bufferSize: Int = BUFFER_SIZE,
  private val fs: FileSystem = FileSystem.SYSTEM,
  internal val clock: Clock = Clock.System,
  internal val timeZone: TimeZone = TimeZone.currentSystemDefault(),
  internal val debugLog: (String) -> Unit = { Log.d("LumberYard", it) },
) {

  @Inject
  constructor(
    app: Application,
    scope: BackgroundAppCoroutineScope,
  ) : this(
    _logDir =
      app.getExternalFilesDir(null)?.toOkioPath()?.resolve("logs")
        ?: throw IOException("External storage is not mounted."),
    scope = scope,
  )

  // Guard against concurrent writes
  private val writeMutex = Mutex()

  private val logFiles = Array(5) { i -> _logDir.resolve("log$i.$LOG_EXTENSION") }
  private var currentFileIndex = 0

  // Act as a ring buffer with a fixed size
  private val logChannel =
    Channel<Entry>(
      capacity = bufferSize,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  private val flushChannel = Channel<Unit>(Channel.CONFLATED)

  // TODO make this debug only, it's really just a backdoor for viewing those logs. Maybe even
  //  just make that UI read the log file contents, though wouldn't be as colorful
  private val writtenLogs = ArrayDeque<Entry>()

  @OptIn(DelicateCoroutinesApi::class)
  private val writeJob: Job =
    scope.launch {
      try {
        debugLog("Starting write job")
        while (isActive && !flushChannel.isClosedForReceive) {
          debugLog("Checking for flush")
          for (flush in flushChannel) {
            debugLog("Flush triggered")
            if (logChannel.isClosedForReceive) break
            val results = mutableListOf(logChannel.receive())
            // Drain the channel buffer
            while (isActive && !logChannel.isClosedForReceive) {
              debugLog("Draining log channel")
              val attempt = logChannel.tryReceive()
              when (val value = attempt.getOrNull()) {
                null -> {
                  debugLog("No more logs to flush, breaking")
                  break
                }
                else -> {
                  debugLog("Adding log - ${value.prettyPrint()}")
                  results += value
                }
              }
            }
            debugLog("Done draining log channel, saving logs to disk")
            if (results.isNotEmpty()) {
              debugLog("Saving logs to disk")
              save(results)
              debugLog("Done saving logs to disk, clearing pending")
              results.clear()
            }
          }
        }
      } catch (e: Exception) {
        Timber.e(e, "Error writing logs to disk")
      } finally {
        debugLog("Finalizer for job")
      }
      debugLog("Exiting write job")
    }

  @OptIn(DelicateCoroutinesApi::class)
  private val flushJob: Job =
    scope.launch {
      while (isActive && !flushChannel.isClosedForSend) {
        delay(flushInterval)
        flushChannel.send(Unit)
      }
    }

  fun tryAddEntry(entry: Entry) {
    logChannel.trySend(entry)
  }

  suspend fun addEntry(entry: Entry) {
    logChannel.send(entry)
  }

  private suspend inline fun <T> guardedIo(body: (logDir: Path) -> T): T {
    return writeMutex.withLock { body(_logDir) }
  }

  /** Save the current logs to disk. */
  suspend fun flush() {
    debugLog("Flush triggered")
    flushChannel.send(Unit)
  }

  /** Tries to save the current logs to disk. */
  fun tryFlush() {
    flushChannel.trySend(Unit)
  }

  fun writtenLogs(): ImmutableList<Entry> = writtenLogs.toImmutableList()

  private tailrec suspend fun save(entries: List<Entry>): Path = guardedIo { logDir ->
    if (!fs.exists(logDir)) {
      fs.createDirectories(logDir)
    }

    val output = logFiles[currentFileIndex]
    return if (!fs.exists(output) || fs.metadata(output).size!! <= MAX_LOG_FILE_SIZE) {
      debugLog("Writing logs to $output")
      AtomicFile(output, fs).tryWrite(append = true) { sink ->
        for (entry in entries) {
          debugLog("Writing entry to disk - ${entry.prettyPrint()}")
          sink.writeUtf8(entry.prettyPrint()).writeByte('\n'.code)
          writtenLogs.add(entry)
        }
      }
      output
    } else {
      debugLog(
        "Rotating files, current file (${output.name}) of size ${fs.metadata(output).size} exceeds max size of ${MAX_LOG_FILE_SIZE.bytes.inWholeMegabytes}MB"
      )
      // Rotate files
      currentFileIndex = (currentFileIndex + 1) % logFiles.size
      save(entries)
    }
  }

  /**
   * Delete all of the log files saved to disk. Be careful not to call this before any intents have
   * finished using the file reference.
   */
  @WorkerThread
  suspend fun cleanUp(): Long = guardedIo { logDir ->
    var deleted = 0L
    fs
      .list(logDir)
      .filter { it.name.endsWith(".log") }
      .forEach {
        fs.metadata(it).size?.let { size -> deleted += size }
        fs.delete(it)
      }

    currentFileIndex = 0

    return deleted
  }

  suspend fun currentLogFileText(): String = guardedIo {
    return fs.read(logFiles[currentFileIndex]) { readUtf8() }
  }

  data class Entry(val time: LocalDateTime, val level: Int, val tag: String?, val message: String) {
    fun prettyPrint(): String {
      return "%s %22s %s %s"
        .format(
          displayTime,
          tag ?: "",
          displayLevel,
          // Indent newlines to match the original indentation.
          message.replace("\\n".toRegex(), "\n                         ")
        )
    }

    val displayTime: String
      get() = ENTRY_TIMESTAMP_FORMATTER.format(time.toJavaLocalDateTime())

    val displayLevel
      get() =
        when (level) {
          Log.VERBOSE -> "V"
          Log.DEBUG -> "D"
          Log.INFO -> "I"
          Log.WARN -> "W"
          Log.ERROR -> "E"
          Log.ASSERT -> "A"
          else -> "?"
        }

    companion object {
      private val ENTRY_TIMESTAMP_FORMATTER: DateTimeFormatter =
        DateTimeFormatterBuilder()
          .appendValue(ChronoField.HOUR_OF_DAY, 2)
          .appendLiteral(':')
          .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
          .optionalStart()
          .appendLiteral(':')
          .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
          .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true)
          .toFormatter()
    }
  }

  suspend fun closeAndJoin() {
    debugLog("Closing and joining")
    // Flush whatever is left in the buffer
    flush()
    flushJob.cancel()
    flushChannel.close()
    logChannel.close()
    debugLog("Joining write job")
    writeJob.join()
  }

  companion object {
    internal const val LOG_EXTENSION = "log"
    private const val BUFFER_SIZE = 200
    private val FLUSH_INTERVAL = 5000.milliseconds
    private val MAX_LOG_FILE_SIZE = 1.megabytes.bytes
  }
}

fun LumberYard.tree(): Timber.Tree {
  return object : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      tryAddEntry(LumberYard.Entry(clock.now().toLocalDateTime(timeZone), priority, tag, message))
    }
  }
}
