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
import catchup.app.data.LumberYard.Entry
import catchup.app.util.BackgroundAppCoroutineScope
import catchup.util.RingBuffer
import catchup.util.io.AtomicFile
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.ImmutableList
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
import me.saket.filesize.FileSize.Companion.megabytes
import okio.BufferedSink
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.buffer
import timber.log.Timber

sealed class LumberYard {
  private var closed = false
  protected open val bufferSize: Int = BUFFER_SIZE
  internal open val clock: Clock = Clock.System
  internal open val timeZone: TimeZone = TimeZone.currentSystemDefault()

  /**
   * Keep last [BUFFER_SIZE] logs in memory for the bugsnag trace tab and debug screen.
   *
   * TODO would be nice to not maintain this separately from [DiskLumberYard.logChannel], but shrug
   */
  private val bufferedLogs = RingBuffer<Entry>(BUFFER_SIZE)

  fun tryAddEntry(entry: Entry) {
    bufferedLogs.push(entry)
    protectedTryAddEntry(entry)
  }

  suspend fun addEntry(entry: Entry) {
    bufferedLogs.push(entry)
    protectedAddEntry(entry)
  }

  protected open fun protectedTryAddEntry(entry: Entry) {}

  protected open suspend fun protectedAddEntry(entry: Entry) {}

  fun bufferedLogs(): ImmutableList<Entry> = bufferedLogs.toImmutableList()

  fun clear() {
    bufferedLogs.clear()
  }

  /** Flushes the current logs to disk, if applicable. */
  open suspend fun flush() {}

  /** Tries to flush the current logs to disk, if applicable. */
  open fun tryFlush() {}

  suspend fun closeAndJoin() {
    protectedCloseAndJoin()
    closed = true
  }

  protected open suspend fun protectedCloseAndJoin() {}

  data class Entry(val time: LocalDateTime, val level: Int, val tag: String?, val message: String) {
    fun prettyPrint(): String {
      return "%s %22s %s %s"
        .format(
          displayTime,
          tag ?: "",
          displayLevel,
          // Indent newlines to match the original indentation.
          message.replace("\\n".toRegex(), "\n                         "),
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

  class Factory
  @Inject
  constructor(
    private val simpleLumberYard: Provider<SimpleLumberYard>,
    private val diskLumberYard: Provider<SimpleLumberYard>,
  ) {
    fun create(useDisk: Boolean): LumberYard {
      return if (useDisk) {
        diskLumberYard.get()
      } else {
        simpleLumberYard.get()
      }
    }
  }

  protected companion object {
    const val BUFFER_SIZE = 200
  }
}

class SimpleLumberYard
internal constructor(
  override val bufferSize: Int = BUFFER_SIZE,
  override val clock: Clock = Clock.System,
  override val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : LumberYard() {
  @Inject constructor() : this(BUFFER_SIZE)
}

// TODO
//  disable flushing in background? Requires exposing some sort of StateFlow<Boolean> on the DI
//  graph of foreground/background events
class DiskLumberYard
internal constructor(
  private val _logDir: Path,
  scope: CoroutineScope,
  private val flushInterval: Duration = FLUSH_INTERVAL,
  override val bufferSize: Int = BUFFER_SIZE,
  override val clock: Clock = Clock.System,
  override val timeZone: TimeZone = TimeZone.currentSystemDefault(),
  private val fs: FileSystem = FileSystem.SYSTEM,
  /** Using AtomicFile isn't really necessary if just appending to a file, but left as an option. */
  private val useAtomicFile: Boolean = false,
) : LumberYard() {

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
    Channel<Entry>(capacity = bufferSize, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  private val flushChannel = Channel<Unit>(Channel.CONFLATED)

  @OptIn(DelicateCoroutinesApi::class)
  private val writeJob: Job =
    scope.launch {
      try {
        while (isActive && !flushChannel.isClosedForReceive) {
          for (flush in flushChannel) {
            if (logChannel.isClosedForReceive) break
            val results = logChannel.drain(this)
            if (results.isNotEmpty()) {
              save(results)
              results.clear()
            }
          }
        }
      } catch (e: Exception) {
        Timber.e(e, "Error writing logs to disk")
      }
    }

  @OptIn(DelicateCoroutinesApi::class)
  private val flushJob: Job =
    scope.launch {
      while (isActive && !flushChannel.isClosedForSend) {
        delay(flushInterval)
        flushChannel.send(Unit)
      }
    }

  override fun protectedTryAddEntry(entry: Entry) {
    logChannel.trySend(entry)
  }

  override suspend fun protectedAddEntry(entry: Entry) {
    logChannel.send(entry)
  }

  private suspend inline fun <T> guardedIo(body: (logDir: Path) -> T): T {
    return writeMutex.withLock { body(_logDir) }
  }

  /** Save the current logs to disk. */
  override suspend fun flush() {
    flushChannel.send(Unit)
  }

  /** Tries to save the current logs to disk. */
  override fun tryFlush() {
    flushChannel.trySend(Unit)
  }

  private tailrec suspend fun save(entries: List<Entry>): Path = guardedIo { logDir ->
    if (!fs.exists(logDir)) {
      fs.createDirectories(logDir)
    }

    val output = logFiles[currentFileIndex]
    return if (!fs.exists(output) || fs.metadata(output).size!! <= MAX_LOG_FILE_SIZE) {
      val writeAction: (BufferedSink) -> Unit = { sink ->
        for (entry in entries) {
          sink.writeUtf8(entry.prettyPrint()).writeByte('\n'.code)
        }
      }
      if (useAtomicFile) {
        AtomicFile(output, fs).tryWrite(append = true, writeAction)
      } else {
        fs.appendingSink(output).buffer().use(writeAction)
      }
      output
    } else {
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

  override suspend fun protectedCloseAndJoin() {
    // Flush whatever is left in the buffer
    flush()
    flushJob.cancel()
    flushChannel.close()
    logChannel.close()
    writeJob.join()
  }

  companion object {
    internal const val LOG_EXTENSION = "log"
    private val FLUSH_INTERVAL = 5000.milliseconds
    private val MAX_LOG_FILE_SIZE = 1.megabytes.inWholeBytes
  }
}

fun LumberYard.tree(): Timber.Tree {
  return object : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      tryAddEntry(Entry(clock.now().toLocalDateTime(timeZone), priority, tag, message))
    }
  }
}

@OptIn(DelicateCoroutinesApi::class)
private suspend fun <T> Channel<T>.drain(scope: CoroutineScope) =
  with(scope) {
    val results = mutableListOf(receive())
    while (isActive && !isClosedForReceive) {
      val attempt = tryReceive()
      when (val value = attempt.getOrNull()) {
        null -> break
        else -> {
          results += value
        }
      }
    }
    results
  }
