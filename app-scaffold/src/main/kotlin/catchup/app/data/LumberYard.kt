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
import catchup.di.AppScope
import catchup.di.SingleIn
import com.squareup.anvil.annotations.optional.ForScope
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.buffer
import timber.log.Timber

@SingleIn(AppScope::class)
class LumberYard(
  private val _logDir: Path,
  scope: CoroutineScope,
  private val flushInterval: Duration = FLUSH_INTERVAL,
  bufferSize: Int = BUFFER_SIZE,
  private val fs: FileSystem = FileSystem.SYSTEM,
  internal val clock: Clock = Clock.System,
  internal val timeZone: TimeZone = TimeZone.currentSystemDefault(),
  private val createFileName: (LocalDateTime) -> String = {
    ISO_LOCAL_DATE_TIME.format(it.toJavaLocalDateTime()) + ".$LOG_EXTENSION"
  }
) {

  @Inject
  constructor(
    app: Application,
    @ForScope(AppScope::class) scope: CoroutineScope,
  ) : this(
    _logDir =
      app.getExternalFilesDir(null)?.toOkioPath()?.resolve("logs")
        ?: throw IOException("External storage is not mounted."),
    scope = scope,
  )

  // Guard against concurrent writes
  private val writeMutex = Mutex()

  // Act as a ring buffer with a fixed size
  private val logChannel =
    Channel<Entry>(
      capacity = bufferSize,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  // TODO what context does this run on?
  @OptIn(DelicateCoroutinesApi::class)
  private val writeJob: Job =
    scope.launch {
      val pendingLogs = mutableListOf<Entry>()
      while (isActive && !logChannel.isClosedForReceive) {
        withTimeoutOrNull(flushInterval) {
          for (item in logChannel) {
            pendingLogs.add(item)
          }
        }
        if (pendingLogs.isNotEmpty()) {
          save(pendingLogs)
          pendingLogs.clear()
        }
      }
    }

  fun tryAddEntry(entry: Entry) {
    logChannel.trySend(entry)
  }

  suspend fun addEntry(entry: Entry) {
    logChannel.send(entry)
  }

  private suspend inline fun <T> guardedIo(body: (logDir: Path) -> T): T {
    return writeMutex.withLock {
      body(_logDir)
    }
  }

  /** Save the current logs to disk. */
  @WorkerThread
  suspend fun save(entries: List<Entry>): Path =
    guardedIo { logDir ->
      if (!fs.exists(logDir)) {
        fs.createDirectories(logDir)
      }
      val output =
        logDir.resolve(createFileName(clock.now().toLocalDateTime(timeZone)))

      fs.sink(output).buffer().use { sink ->
        for (entry in entries) {
          sink.writeUtf8(entry.prettyPrint()).writeByte('\n'.code)
        }
      }
      return output
    }

  /**
   * Delete all of the log files saved to disk. Be careful not to call this before any intents have
   * finished using the file reference.
   */
  @WorkerThread
  suspend fun cleanUp(): Long =
    guardedIo { logDir ->
      var deleted = 0L
      fs
        .list(logDir)
        .filter { it.name.endsWith(".log") }
        .forEach {
          fs.metadata(it).size?.let { size -> deleted += size }
          fs.delete(it)
        }

      return deleted
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
    logChannel.close()
    writeJob.join()
  }

  companion object {
    internal const val LOG_EXTENSION = "log"
    private const val BUFFER_SIZE = 200
    private val FLUSH_INTERVAL = 2000L.milliseconds
  }
}

fun LumberYard.tree(): Timber.Tree {
  return object : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
      tryAddEntry(
        LumberYard.Entry(
          clock.now().toLocalDateTime(timeZone),
          priority,
          tag,
          message
        )
      )
    }
  }
}
