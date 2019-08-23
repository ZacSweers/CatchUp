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
package io.sweers.catchup.ui.logs

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import io.sweers.catchup.data.LumberYard
import io.sweers.catchup.service.api.temporaryScope
import io.sweers.catchup.util.maybeStartChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsDialog(context: Context, private val lumberYard: LumberYard) : AlertDialog(context) {
  private val adapter: LogAdapter = LogAdapter(context)

  private var scope = temporaryScope()

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

    scope.newScope().launch {
      lumberYard.logs()
          .collect { adapter.addEntry(it) }
    }
  }

  override fun onStop() {
    super.onStop()
    scope.cancel()
  }

  private fun share() {
    // Dialog's dismissed by this point, so we need a new scope here. We're kicking to the system, so just finish on global scope
    GlobalScope.launch {
      try {
        val file = withContext(Dispatchers.IO) { lumberYard.save() }
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_STREAM, file.toUri())
        context.maybeStartChooser(sendIntent)
      } catch (e: Exception) {
        Toast.makeText(context, "Couldn't save the logs for sharing.", Toast.LENGTH_SHORT)
            .show()
      }
    }
  }
}
