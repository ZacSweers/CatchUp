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

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.RxViewHolder
import butterknife.BindDimen
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.rx2.Rx2Apollo
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.uber.autodispose.kotlin.autoDisposable
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.GlideApp
import io.sweers.catchup.R
import io.sweers.catchup.R.layout
import io.sweers.catchup.data.LinkManager
import io.sweers.catchup.data.github.ProjectOwnersByIdsQuery
import io.sweers.catchup.data.github.ProjectOwnersByIdsQuery.AsOrganization
import io.sweers.catchup.data.github.ProjectOwnersByIdsQuery.AsUser
import io.sweers.catchup.data.github.RepositoriesByIdsQuery
import io.sweers.catchup.data.github.RepositoriesByIdsQuery.AsRepository
import io.sweers.catchup.data.github.RepositoryByNameAndOwnerQuery
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.service.api.UrlMeta
import io.sweers.catchup.ui.Scrollable
import io.sweers.catchup.ui.StickyHeaders
import io.sweers.catchup.ui.StickyHeadersLinearLayoutManager
import io.sweers.catchup.ui.about.LicensesModule.ForLicenses
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.ui.base.CatchUpItemViewHolder
import io.sweers.catchup.util.UiUtil
import io.sweers.catchup.util.dp2px
import io.sweers.catchup.util.findSwatch
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.luminosity
import io.sweers.catchup.util.w
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator
import okio.Okio
import javax.inject.Inject
import javax.inject.Qualifier

/**
 * A controller that displays oss licenses.
 */
class LicensesController : ButterKnifeController(), Scrollable {

  @Inject
  lateinit var apolloClient: ApolloClient

  @Inject
  @field:ForLicenses
  lateinit var moshi: Moshi

  @Inject
  internal lateinit var linkManager: LinkManager

  @BindDimen(R.dimen.avatar)
  @JvmField
  var dimenSize: Int = 0
  @BindView(R.id.progress)
  lateinit var progressBar: ProgressBar
  @BindView(R.id.list)
  lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView

  private val adapter = Adapter()
  private lateinit var layoutManager: StickyHeadersLinearLayoutManager<Adapter>

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View =
      inflater.inflate(R.layout.controller_licenses, container, false)

  override fun bind(view: View) = ButterKnife.bind(this, view)

  override fun onViewBound(view: View) {
    super.onViewBound(view)
    recyclerView.adapter = adapter
    layoutManager = StickyHeadersLinearLayoutManager(view.context)
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
              w(error) { "Could not load open source licenses." }
              Snackbar.make(recyclerView, R.string.licenses_error,
                  Snackbar.LENGTH_SHORT).show()
            }
          }
    }
  }

  /**
   * I give you: the most over-engineered OSS licenses section ever.
   */
  private fun requestItems(): Single<List<OssBaseItem>> {
    return Single
        .fromCallable {
          // Start with a fetch of our github entries from assets
          moshi.adapter<List<OssGitHubEntry>>(
              Types.newParameterizedType(List::class.java, OssGitHubEntry::class.java))
              .fromJson(Okio.buffer(Okio.source(resources!!.assets.open("licenses_github.json"))))
        }
        .flattenAsObservable { it }
        .map { RepositoryByNameAndOwnerQuery(it.owner, it.name) }
        .flatMapSingle {
          // Fetch repos, send down a map of the ids to owner ids
          Rx2Apollo.from(apolloClient.query(it)
              .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
              .firstOrError()
              .map {
                with(it.data()!!.repository()!!) {
                  id() to owner().id()
                }
              }
              .subscribeOn(Schedulers.io())
        }
        .distinct()
        .reduce(mutableMapOf()) { map: MutableMap<String, String>, (first, second) ->
          map.apply {
            put(first, second)
          }
        }
        .flatMap {
          Single.zip(
              // Fetch the repositories by their IDs, map down to its
              Rx2Apollo.from(apolloClient.query(RepositoriesByIdsQuery(it.keys.toList()))
                  .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
                  .firstOrError()
                  .map { it.data()!!.nodes()!!.map { it as AsRepository } },
              // Fetch the users by their IDs
              Rx2Apollo.from(apolloClient.query(ProjectOwnersByIdsQuery(it.values.distinct()))
                  .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
                  .flatMapIterable { it.data()!!.nodes() }
                  // Reduce into a map of the owner ID -> display name
                  .reduce(mutableMapOf()) { map, node ->
                    map.apply {
                      val (id, name) = when (node) {
                        is AsOrganization -> with(node) { id() to (name() ?: login()) }
                        is AsUser -> with(node) { id() to (name() ?: login()) }
                        else -> throw IllegalStateException("Unrecognized node type: $node")
                      }
                      map[id] = name
                    }
                  },
              // Map the repos and their corresponding user display name into a list of their pairs
              BiFunction { nodes: List<AsRepository>, userIdToNameMap: Map<String, String> ->
                nodes.map {
                  it to userIdToNameMap[it.owner().id()]!!
                }
              })
        }
        .flattenAsObservable { it }
        .map { (repo, ownerName) ->
          OssItem(
              avatarUrl = repo.owner().avatarUrl().toString(),
              author = ownerName,
              name = repo.name(),
              clickUrl = repo.url().toString(),
              license = repo.licenseInfo()?.name(),
              description = repo.description()
          )
        }
        .startWith(
            Single
                .fromCallable {
                  moshi.adapter<List<OssItem>>(
                      Types.newParameterizedType(List::class.java, OssItem::class.java))
                      .fromJson(
                          Okio.buffer(Okio.source(resources!!.assets.open("licenses_mixins.json"))))
                }
                .flattenAsObservable { it }
        )
        .groupBy { it.author }
        .sorted { o1, o2 -> o1.key!!.compareTo(o2.key!!) }
        .concatMapEager { it.sorted { o1, o2 -> o1.name.compareTo(o2.name) } }
        .toList()
        .map {
          val collector = mutableListOf<OssBaseItem>()
          with(it[0]) {
            collector.add(io.sweers.catchup.ui.about.OssItemHeader(
                name = author,
                avatarUrl = avatarUrl
            ))
          }
          it.fold(it[0].author) { lastAuthor, currentItem ->
            if (currentItem.author != lastAuthor) {
              collector.add(OssItemHeader(
                  name = currentItem.author,
                  avatarUrl = currentItem.avatarUrl
              ))
            }
            collector.add(currentItem)
            currentItem.author
          }
          collector
        }
  }

  override fun onRequestScrollToTop() {
    if (layoutManager.findFirstVisibleItemPosition() > 50) {
      recyclerView.scrollToPosition(0)
    } else {
      recyclerView.smoothScrollToPosition(0)
    }
  }

  private inner class Adapter : RecyclerView.Adapter<ViewHolder>(),
      StickyHeaders, StickyHeaders.ViewSetup {

    private val items = mutableListOf<OssBaseItem>()
    private val headerColorThresholdFun = { swatch: Swatch ->
      if (activity?.isInNightMode() == true) {
        swatch.luminosity > 0.6F
      } else {
        swatch.luminosity < 0.5F
      }
    }

    private val defaultHeaderTextColor by lazy {
      if (activity?.isInNightMode() == true) Color.WHITE else Color.BLACK
    }

    override fun isStickyHeader(position: Int) = items[position] is OssItemHeader

    override fun setupStickyHeaderView(stickyHeader: View) {
      stickyHeader.animate()
          .z(stickyHeader.resources.dp2px(4f))
          .setInterpolator(UiUtil.fastOutSlowInInterpolator)
          .setDuration(200)
          .start()
    }

    override fun teardownStickyHeaderView(stickyHeader: View) {
      with(stickyHeader) {
        findViewById<TextView>(R.id.title).setTextColor(defaultHeaderTextColor)
        animate()
            .z(0f)
            .setInterpolator(UiUtil.fastOutSlowInInterpolator)
            .setDuration(200)
            .start()
      }
    }

    fun setItems(newItems: List<OssBaseItem>) {
      items.addAll(newItems)
      notifyItemRangeInserted(0, newItems.size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val item = items[position]
      when (item) {
        is OssItemHeader -> (holder as HeaderHolder).run {
          GlideApp.with(itemView)
              .load(item.avatarUrl)
              .apply(RequestOptions()
                  .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                  .circleCrop()
                  .override(dimenSize, dimenSize))
              .transition(DrawableTransitionOptions.withCrossFade())
              .into(object : DrawableImageViewTarget(icon), Palette.PaletteAsyncListener {
                override fun onResourceReady(resource: Drawable,
                    transition: Transition<in Drawable>?) {
                  super.onResourceReady(resource, transition)
                  if (resource is BitmapDrawable) {
                    Palette.from(resource.bitmap)
                        .clearFilters()
                        .generate(this)
                  }
                }

                override fun onGenerated(palette: Palette?) {
                  holder.title.setTextColor(
                      palette?.findSwatch(headerColorThresholdFun)?.rgb ?: defaultHeaderTextColor)
                }
              })
          title.text = item.name
        }
        is OssItem -> (holder as CatchUpItemViewHolder).apply {
          // Set the title top margin to 0dp as layout_goneMarginTop doesn't work.
          // See https://issuetracker.google.com/issues/68768935
          setTitleTopMargin(holder)
          title(
              "${item.name}${if (!item.description.isNullOrEmpty()) " — ${item.description}" else ""}")
          score(null)
          timestamp(null)
          author(item.license)
          source(null)
          tag(null)
          hideComments()
          itemClicks()
              .flatMapCompletable {
                // Search up to the first sticky header position
                // Maybe someday we should just return the groups rather than flattening
                // but this was neat to write in kotlin
                val accentColor = (holder.adapterPosition downTo 0)
                    .find { isStickyHeader(it) }
                    ?.let {
                      (recyclerView.findViewHolderForAdapterPosition(it)
                          as HeaderHolder)
                          .title.textColors.defaultColor
                    } ?: 0
                val context = itemView.context
                return@flatMapCompletable linkManager.openUrl(
                    UrlMeta(item.clickUrl, accentColor,
                        context))
              }
              .autoDisposable(holder)
              .subscribe()
        }
      }
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      return if (viewType == 0) {
        HeaderHolder(LayoutInflater.from(parent.context)
            .inflate(layout.about_header_item, parent, false))
      } else {
        CatchUpItemViewHolder(LayoutInflater.from(parent.context)
            .inflate(layout.list_item_general, parent, false))
      }
    }

    override fun getItemViewType(position: Int) = items[position].itemType()
  }

  private fun setTitleTopMargin(holder: CatchUpItemViewHolder) = with(holder) {
    with(title) {
      val lp = layoutParams as ViewGroup.MarginLayoutParams
      if (lp.topMargin != 0) {
        layoutParams = lp.apply { this.topMargin = 0 }
      }
    }
  }
}

private data class OssGitHubEntry(val owner: String, val name: String) {
  companion object {
    fun adapter(): JsonAdapter<OssGitHubEntry> {
      return object : JsonAdapter<OssGitHubEntry>() {
        override fun toJson(writer: JsonWriter, value: OssGitHubEntry?) {
          TODO("not implemented")
        }

        override fun fromJson(reader: JsonReader): OssGitHubEntry {
          reader.beginObject()
          // Ugly - these would preferably be lateinit
          var owner: String? = null
          var name: String? = null
          while (reader.hasNext()) {
            when (reader.nextName()) {
              "owner" -> {
                owner = reader.nextString()
              }
              "name" -> {
                name = reader.nextString()
              }
            }
          }
          reader.endObject()
          return OssGitHubEntry(owner!!, name!!)
        }
      }
    }
  }
}

private class HeaderHolder(view: View) : RxViewHolder(view) {
  @BindView(R.id.icon)
  lateinit var icon: ImageView
  @BindView(R.id.title)
  lateinit var title: TextView
  private var unbinder: Unbinder? = null

  init {
    unbinder?.unbind()
    unbinder = ButterKnife.bind(this, itemView)
  }
}

private sealed class OssBaseItem {
  abstract fun itemType(): Int
}

private data class OssItemHeader(
    val avatarUrl: String,
    val name: String
) : OssBaseItem() {
  override fun itemType() = 0
}

private data class OssItem(
    val avatarUrl: String,
    val author: String,
    val name: String,
    val license: String?,
    val clickUrl: String,
    val description: String?,
    val authorUrl: String? = null
) : OssBaseItem() {

  override fun itemType() = 1

  companion object {
    fun adapter(): JsonAdapter<OssItem> {
      return object : JsonAdapter<OssItem>() {
        override fun toJson(writer: JsonWriter, value: OssItem?) {
          TODO("not implemented")
        }

        override fun fromJson(reader: JsonReader): OssItem {
          reader.beginObject()
          // Ugly - these would preferably be lateinit
          var author: String? = null
          var authorUrl: String? = null
          var name: String? = null
          var avatarUrl: String? = null
          var clickUrl: String? = null

          // Actual nullable props
          var license: String? = null
          var description: String? = null
          while (reader.hasNext()) {
            when (reader.nextName()) {
              "author" -> {
                author = reader.nextString()
              }
              "authorUrl" -> {
                authorUrl = reader.nextString()
              }
              "name" -> {
                name = reader.nextString()
              }
              "avatarUrl" -> {
                avatarUrl = reader.nextString()
              }
              "license" -> {
                license = reader.nextString()
              }
              "clickUrl" -> {
                clickUrl = reader.nextString()
              }
              "description" -> {
                description = reader.nextString()
              }
            }
          }
          reader.endObject()
          return OssItem(author = author!!,
              name = name!!,
              avatarUrl = avatarUrl!!,
              authorUrl = authorUrl,
              license = license,
              clickUrl = clickUrl!!,
              description = description)
        }
      }
    }
  }
}

@PerController
@Subcomponent(modules = [LicensesModule::class])
interface LicensesComponent : AndroidInjector<LicensesController> {

  @Subcomponent.Builder
  abstract class Builder : AndroidInjector.Builder<LicensesController>()
}

@dagger.Module
internal object LicensesModule {

  @Qualifier
  annotation class ForLicenses

  @Provides
  @JvmStatic
  @PerController
  @ForLicenses
  internal fun provideAboutMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder()
        .add(OssItem::class.java,
            OssItem.adapter())
        .add(OssGitHubEntry::class.java,
            OssGitHubEntry.adapter())
        .build()
  }
}
