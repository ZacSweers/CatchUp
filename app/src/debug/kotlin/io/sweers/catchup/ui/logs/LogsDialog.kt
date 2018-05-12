/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.logs

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.util.maybeStartChooser
import java.io.File

class LogsDialog(context: Context, private val lumberYard: LumberYard) : AlertDialog(context) {
  private val adapter: LogAdapter = LogAdapter(context)

  private var disposables: CompositeDisposable? = null

  init {
    val listView = ListView(context)
    listView.transcriptMode = ListView.TRANSCRIPT_MODE_NORMAL
    listView.adapter = adapter

    setTitle("Logs")
    setView(listView)
    setButton(DialogInterface.BUTTON_NEGATIVE, "Close") { _, _ ->
      // NO-OP.
    }
    setButton(DialogInterface.BUTTON_POSITIVE, "Share") { _, _ -> share() }
  }

  override fun onStart() {
    super.onStart()

    adapter.setLogs(lumberYard.bufferedLogs())

    disposables = CompositeDisposable().apply {
      add(lumberYard.logs()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(adapter))
    }
  }

  override fun onStop() {
    super.onStop()
    disposables?.dispose()
  }

  private fun share() {
    lumberYard.save()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(object : SingleObserver<File> {
          override fun onSubscribe(d: Disposable) {

          }

          override fun onSuccess(file: File) {
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_STREAM, file.toUri())
            context.maybeStartChooser(sendIntent)
          }

          override fun onError(e: Throwable) {
            Toast.makeText(context, "Couldn't save the logs for sharing.", Toast.LENGTH_SHORT)
                .show()
          }
        })
  }
}
