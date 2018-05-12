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
import android.util.Log
import android.util.Log.ERROR
import android.util.Log.INFO
import android.util.Log.VERBOSE
import android.util.Log.WARN
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import butterknife.BindView
import butterknife.ButterKnife
import io.reactivex.functions.Consumer
import io.sweers.catchup.R
import io.sweers.catchup.data.LumberYard.Entry
import io.sweers.catchup.ui.BindableAdapter
import java.util.ArrayList

internal class LogAdapter(context: Context) : BindableAdapter<Entry>(context), Consumer<Entry> {
  private var logs = mutableListOf<Entry>()

  fun setLogs(logs: List<Entry>) {
    this.logs = ArrayList(logs)
  }

  @Throws(Exception::class)
  override fun accept(entry: Entry) {
    logs.add(entry)
    notifyDataSetChanged()
  }

  override fun getCount() = logs.size

  override fun getItem(position: Int) = logs[position]

  override fun getItemId(i: Int) = i.toLong()

  override fun newView(inflater: LayoutInflater, position: Int, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.debug_logs_list_item, container, false)
    val viewHolder = LogItemViewHolder(view)
    view.tag = viewHolder
    return view
  }

  override fun bindView(item: Entry, position: Int, view: View) {
    val viewHolder = view.tag as LogItemViewHolder
    viewHolder.setEntry(item)
  }

  internal class LogItemViewHolder(private val rootView: View) {
    @BindView(R.id.debug_log_level) lateinit var levelView: TextView
    @BindView(R.id.debug_log_tag) lateinit var tagView: TextView
    @BindView(R.id.debug_log_message) lateinit var messageView: TextView

    init {
      ButterKnife.bind(this, rootView)
    }

    fun setEntry(entry: Entry) {
      rootView.setBackgroundResource(backgroundForLevel(entry.level))
      levelView.text = entry.displayLevel()
      tagView.text = entry.tag
      messageView.text = entry.message
    }
  }

  companion object {

    @DrawableRes
    fun backgroundForLevel(level: Int) = when (level) {
      VERBOSE, Log.DEBUG -> R.color.debug_log_accent_debug
      INFO -> R.color.debug_log_accent_info
      WARN -> R.color.debug_log_accent_warn
      ERROR, Log.ASSERT -> R.color.debug_log_accent_error
      else -> R.color.debug_log_accent_unknown
    }
  }
}
