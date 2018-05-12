/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data

import android.app.Application
import androidx.annotation.WorkerThread
import android.util.Log
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import okio.BufferedSink
import okio.Okio
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.ArrayDeque
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LumberYard @Inject constructor(private val app: Application) {

  private val entries = ArrayDeque<Entry>(BUFFER_SIZE + 1)
  private val entrySubject = PublishSubject.create<Entry>()

  fun tree(): Timber.Tree {
    return object : Timber.DebugTree() {
      override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        addEntry(Entry(priority, tag, message))
      }
    }
  }

  @Synchronized private fun addEntry(entry: Entry) {
    entries.addLast(entry)
    if (entries.size > BUFFER_SIZE) {
      entries.removeFirst()
    }

    entrySubject.onNext(entry)
  }

  fun bufferedLogs() = ArrayList(entries)

  fun logs(): Observable<Entry> = entrySubject.hide()

  /**
   * Save the current logs to disk.
   */
  fun save(): Single<File> {
    return Single.create<File> { subscriber ->
      val folder = app.getExternalFilesDir(null)
      if (folder == null) {
        subscriber.onError(IOException("External storage is not mounted."))
        return@create
      }

      val fileName = ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
      val output = File(folder, fileName)

      var sink: BufferedSink? = null
      try {
        sink = Okio.buffer(Okio.sink(output))
        val entries1 = bufferedLogs()
        for (entry in entries1) {
          sink!!.writeUtf8(entry.prettyPrint()).writeByte('\n'.toInt())
        }
        // need to close before emiting file to the subscriber, because when subscriber receives
        // data in the same thread the file may be truncated
        sink!!.close()
        sink = null

        subscriber.onSuccess(output)
      } catch (e: IOException) {
        subscriber.onError(e)
      } finally {
        if (sink != null) {
          try {
            sink.close()
          } catch (e: IOException) {
            subscriber.onError(e)
          }

        }
      }
    }
  }

  /**
   * Delete all of the log files saved to disk. Be careful not to call this before any intents have
   * finished using the file reference.
   */
  @WorkerThread
  fun cleanUp(): Long {
    val folder = app.getExternalFilesDir(null)
    val initialSize = folder.length()
    folder?.listFiles()?.asSequence()?.filter {
      it.name.endsWith(".log")
    }?.forEach { it.delete() }
    return initialSize - folder.length()
  }

  data class Entry(val level: Int, val tag: String?, val message: String) {
    fun prettyPrint(): String {
      return String.format("%22s %s %s", tag ?: "CATCHUP", displayLevel(),
          // Indent newlines to match the original indentation.
          message.replace("\\n".toRegex(), "\n                         "))
    }

    fun displayLevel() = when (level) {
      Log.VERBOSE -> "V"
      Log.DEBUG -> "D"
      Log.INFO -> "I"
      Log.WARN -> "W"
      Log.ERROR -> "E"
      Log.ASSERT -> "A"
      else -> "?"
    }
  }

  companion object {
    private val BUFFER_SIZE = 200
  }
}
