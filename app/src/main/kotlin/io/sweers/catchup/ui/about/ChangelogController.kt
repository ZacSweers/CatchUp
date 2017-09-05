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

package io.sweers.catchup.ui.about

import android.content.Context
import android.net.Uri
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import butterknife.BindView
import butterknife.ButterKnife
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.rx2.Rx2Apollo
import com.bluelinelabs.conductor.Controller
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.R
import io.sweers.catchup.R.layout
import io.sweers.catchup.data.github.RepoReleasesQuery
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.ui.base.CatchUpItemViewHolder
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import io.sweers.catchup.util.e
import io.sweers.catchup.util.hide
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator
import org.threeten.bp.Instant
import javax.inject.Inject

class ChangelogController : ButterKnifeController(), Scrollable {

  @Inject lateinit var apolloClient: ApolloClient
  @Inject internal lateinit var customTab: CustomTabActivityHelper

  @BindView(R.id.progress) lateinit var progressBar: ProgressBar
  @BindView(R.id.list) lateinit var recyclerView: RecyclerView

  private lateinit var layoutManager: LinearLayoutManager
  private val adapter = ChangelogAdapter()

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_changelog, container, false)
  }

  override fun bind(view: View) = ButterKnife.bind(this, view)

  override fun onViewBound(view: View) {
    super.onViewBound(view)
    recyclerView.adapter = adapter
    layoutManager = LinearLayoutManager(view.context)
    recyclerView.layoutManager = layoutManager
    recyclerView.itemAnimator = FadeInUpAnimator(OvershootInterpolator(1f)).apply {
      addDuration = 300
      removeDuration = 300
    }
  }

  override fun onAttach(view: View) {
    super.onAttach(view)
    if (adapter.itemCount == 0) {
      // Weird hack to avoid adding more unnecessarily. I'm not sure how to leave transient state
      // during onPause in Conductor
      requestItems()
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doFinally {
            progressBar.hide()
          }
          .subscribe { data, error ->
            if (data != null) {
              adapter.setItems(data)
            } else {
              // TODO Show a better error
              e(error) { "Could not load changelog." }
              Snackbar.make(recyclerView, "Could not load changelog.",
                  Snackbar.LENGTH_SHORT).show()
            }
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
        hideComments()
        itemClicks()
            .flatMapCompletable {
              Completable.fromAction {
                val context = itemView.context
                customTab.openCustomTab(context,
                    customTab.customTabIntent
                        .setStartAnimations(context, R.anim.slide_up, R.anim.inset)
                        .setExitAnimations(context, R.anim.outset, R.anim.slide_down)
                        .build(),
                    Uri.parse(item.url))
              }
            }
            .autoDisposeWith(holder)
            .subscribe()
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

@PerController
@Subcomponent
interface ChangelogComponent : AndroidInjector<ChangelogController> {

  @Subcomponent.Builder
  abstract class Builder : AndroidInjector.Builder<ChangelogController>()
}

@Module(subcomponents = arrayOf(ChangelogComponent::class))
abstract class ChangelogControllerBindingModule {

  @Binds
  @IntoMap
  @ControllerKey(ChangelogController::class)
  internal abstract fun bindChangelogControllerInjectorFactory(
      builder: ChangelogComponent.Builder): AndroidInjector.Factory<out Controller>
}
