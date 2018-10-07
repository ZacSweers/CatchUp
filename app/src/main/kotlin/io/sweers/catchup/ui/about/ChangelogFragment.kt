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

package io.sweers.catchup.ui.about

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.rx2.Rx2Apollo
import com.google.android.material.snackbar.Snackbar
import com.uber.autodispose.autoDisposable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.R
import io.sweers.catchup.R.layout
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.github.RepoReleasesQuery
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.base.CatchUpItemViewHolder
import io.sweers.catchup.ui.base.InjectableBaseFragment
import io.sweers.catchup.util.e
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.w
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator
import kotterknife.bindView
import org.threeten.bp.Instant
import java.io.IOException
import javax.inject.Inject

class ChangelogFragment : InjectableBaseFragment(), Scrollable {

  @Inject
  lateinit var apolloClient: ApolloClient
  @Inject
  internal lateinit var linkManager: LinkManager

  private val progressBar by bindView<ProgressBar>(R.id.progress)
  private val recyclerView by bindView<RecyclerView>(R.id.list)

  private lateinit var layoutManager: LinearLayoutManager
  private lateinit var adapter: ChangelogAdapter

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_changelog, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    adapter = ChangelogAdapter()
    recyclerView.adapter = adapter
    layoutManager = LinearLayoutManager(view.context)
    recyclerView.layoutManager = layoutManager
    var pendingRvState: Parcelable? = null
    if (savedInstanceState == null) {
      recyclerView.itemAnimator = FadeInUpAnimator(OvershootInterpolator(1f)).apply {
        addDuration = 300
        removeDuration = 300
      }
    } else {
      pendingRvState = savedInstanceState.getParcelable("changeloglm")
    }

    requestItems()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally {
          progressBar.hide()
        }
        .autoDisposable(this)
        .subscribe { data, error ->
          if (data != null) {
            adapter.setItems(data)
            pendingRvState?.let(layoutManager::onRestoreInstanceState)
          } else {
            // TODO Show a better error
            if (error is IOException) {
              w(error) { "Could not load changelog." }
            } else {
              e(error) { "Could not load changelog." }
            }
            Snackbar.make(recyclerView,
                R.string.changelog_error,
                Snackbar.LENGTH_SHORT)
                .show()
          }
        }
  }

  private fun requestItems(): Single<List<ChangeLogItem>> {
    return Rx2Apollo.from(apolloClient.query(RepoReleasesQuery()))
        .flatMapIterable { it.data()!!.repository()!!.releases().nodes() }
        .map {
          with(it) {
            ChangeLogItem(
                name = name()!!,
                timestamp = publishedAt()!!,
                tag = tag()!!.name(),
                sha = tag()!!.target().abbreviatedOid(),
                url = url().toString(),
                description = description()!!
            )
          }
        }
        .toList()
  }

  override fun onRequestScrollToTop() {
    if (layoutManager.findFirstVisibleItemPosition() > 50) {
      recyclerView.scrollToPosition(0)
    } else {
      recyclerView.smoothScrollToPosition(0)
    }
  }

  private inner class ChangelogAdapter : Adapter<CatchUpItemViewHolder>() {

    private val items = mutableListOf<ChangeLogItem>()

    fun setItems(newItems: List<ChangeLogItem>) {
      items.addAll(newItems)
      notifyItemRangeInserted(0, newItems.size)
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatchUpItemViewHolder {
      return CatchUpItemViewHolder(LayoutInflater.from(parent.context)
          .inflate(layout.list_item_general, parent, false))
    }

    override fun onBindViewHolder(holder: CatchUpItemViewHolder, position: Int) {
      val item = items[position]
      holder.apply {
        title(item.name)
        tag(item.tag)
        timestamp(item.timestamp)
        source(item.sha)
        author(null)
        hideMark()
        holder.container.setOnClickListener {
          linkManager.openUrl(UrlMeta(item.url, 0, itemView.context))
        }
      }
    }
  }
}

private data class ChangeLogItem(
    val name: String,
    val timestamp: Instant,
    val tag: String,
    val sha: String,
    val url: String,
    val description: String
)
