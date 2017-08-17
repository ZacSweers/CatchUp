/*
 * Copyright (c) 201()7 Zac Sweers
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

package io.sweers.catchup.ui.activity

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.annotation.Px
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.NavUtils
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.Palette
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.RxViewHolder
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindDimen
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo.cache.http.HttpCache
import com.apollographql.apollo.cache.http.HttpCachePolicy
import com.apollographql.apollo.cache.http.HttpCacheStore
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.internal.util.ApolloLogger
import com.apollographql.apollo.rx2.Rx2Apollo
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.jakewharton.rxbinding2.support.design.widget.RxAppBarLayout
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.R.layout
import io.sweers.catchup.data.AuthInterceptor
import io.sweers.catchup.data.HttpUrlApolloAdapter
import io.sweers.catchup.data.ISO8601InstantApolloAdapter
import io.sweers.catchup.data.github.ProjectOwnersByIdsQuery
import io.sweers.catchup.data.github.RepositoriesByIdsQuery
import io.sweers.catchup.data.github.RepositoriesByIdsQuery.AsRepository
import io.sweers.catchup.data.github.RepositoryByNameAndOwnerQuery
import io.sweers.catchup.data.github.type.CustomType
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.injection.qualifiers.ApplicationContext
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.StickyHeaders
import io.sweers.catchup.ui.StickyHeadersLinearLayoutManager
import io.sweers.catchup.ui.activity.AboutModule.ForAbout
import io.sweers.catchup.ui.activity.ScrollDirection.UP
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.ui.base.CatchUpItemViewHolder
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import io.sweers.catchup.util.dp2px
import io.sweers.catchup.util.e
import io.sweers.catchup.util.fastOutSlowInInterpolator
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.makeGone
import io.sweers.catchup.util.orderedSwatches
import io.sweers.catchup.util.setLightStatusBar
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okio.Okio
import org.threeten.bp.Instant
import javax.inject.Inject
import javax.inject.Qualifier

class AboutActivity : BaseActivity() {

  @Inject internal lateinit var customTab: CustomTabActivityHelper
  @BindView(R.id.controller_container) internal lateinit var container: ViewGroup

  private lateinit var router: Router

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    customTab.doOnDestroy { connectionCallback = null }
    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_about, viewGroup)

    ButterKnife.bind(this).doOnDestroy { unbind() }
    router = Conductor.attachRouter(this, container, savedInstanceState)
    if (!router.hasRootController()) {
      router.setRoot(RouterTransaction.with(AboutController()))
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      NavUtils.navigateUpFromSameTask(this)
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onStart() {
    super.onStart()
    customTab.bindCustomTabsService(this)
  }

  override fun onStop() {
    customTab.unbindCustomTabsService(this)
    super.onStop()
  }

  override fun onBackPressed() {
    if (!router.handleBack()) {
      super.onBackPressed()
    }
  }

  @dagger.Module
  object Module {

    @Provides
    @JvmStatic
    @PerActivity
    internal fun provideCustomTabActivityHelper(): CustomTabActivityHelper {
      return CustomTabActivityHelper()
    }
  }
}

class AboutController : ButterKnifeController() {

  @BindDimen(R.dimen.avatar)
  @JvmField
  var dimenSize: Int = 0
  @BindView(R.id.toolbar) lateinit var toolbar: Toolbar
  @BindView(R.id.progress) lateinit var progressBar: ProgressBar
  @BindView(R.id.list) lateinit var recyclerView: RecyclerView
  @BindView(R.id.appbarlayout) lateinit var appBarLayout: AppBarLayout
  @BindView(R.id.banner_container) lateinit var bannerContainer: View
  @BindView(R.id.banner_icon) lateinit var bannerIcon: ImageView
  @BindView(R.id.banner_title) lateinit var title: TextView
  @BindView(R.id.banner_text) lateinit var aboutText: TextView

  @Inject lateinit var apolloClient: ApolloClient

  @Inject
  @ForAbout
  lateinit var moshi: Moshi

  @Inject internal lateinit var customTab: CustomTabActivityHelper

  private val adapter = Adapter()

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_about, container, false)
  }

  override fun bind(view: View) = ButterKnife.bind(this, view)

  override fun onViewBound(view: View) {
    super.onViewBound(view)
    with(activity as AppCompatActivity) {
      if (!isInNightMode()) {
        toolbar.setLightStatusBar()
      }
      setSupportActionBar(toolbar)
      supportActionBar?.run {
        setDisplayHomeAsUpEnabled(true)
        setDisplayShowTitleEnabled(false)
      }
    }

    bannerIcon.setOnClickListener {
      appBarLayout.setExpanded(false, true)
    }

    recyclerView.adapter = adapter
    recyclerView.layoutManager = StickyHeadersLinearLayoutManager<Adapter>(view.context)
    recyclerView.itemAnimator = FadeInUpAnimator(OvershootInterpolator(1f)).apply {
      addDuration = 300
      removeDuration = 300
    }

    val parallaxMultiplier
        = (bannerContainer.layoutParams as CollapsingToolbarLayout.LayoutParams).parallaxMultiplier
    appBarLayout.post {
      // Wait till things are measured
      val interpolator = FastOutSlowInInterpolator()

      // TODO This all currently assumes left/right and not start/end
      // X is pretty simple, we just want to end up 72dp to the end of start
      @Px val translatableHeight = appBarLayout.measuredHeight - toolbar.measuredHeight
      @Px val titleX = title.x
      @Px val desiredTitleX = toolbar.resources.dp2px(72f)
      @Px val xDelta = titleX - desiredTitleX

      // Y values are a bit trickier - these need to figure out where they would be on the larger
      // plane, so we calculate it upfront by predicting where it would land after collapse is done.
      // This requires knowing the parallax multiplier and adjusting for the parent plane rather
      // than the relative plane of the internal LL. Once we know the predicted global Y, easy to
      // calculate desired delta from there.
      @Px val titleY = title.y
      @Px val desiredTitleY = (toolbar.measuredHeight - title.measuredHeight) / 2
      @Px val predictedFinalY = titleY - (translatableHeight * parallaxMultiplier)
      @Px val yDelta = desiredTitleY - predictedFinalY

      /*
       * Here we want to get the appbar offset changes paired with the direction it's moving and
       * using RxBinding's great `offsetChanges` API to make an rx Observable of this. The naive
       * approach would be to spin off a single stream and buffer two events with a direction
       * mapping, but this would come at the cost of effectively sampling the stream to 50% of
       * emissions. To get around this, share the offsetChanges and split into two observables.
       * 1 - "offsetChanges" - just emits offset changes downstream. The final stream will mostly
       * emit at this one's cadence.
       * 2 - "directionChanges", which does the buffering of two and emits "scroll direction" enums,
       * starting with UP (since our appbar starts expanded), and distinctUntilChanged() to minimize
       * noise.
       *
       * With these two observables, we'll combine them with the `combineLatest` factory (SAM
       * overload here provided by RxKotlin) and a function that simply pairs the offsets with
       * the direction. This gives us a nice stream of (offset, direction) emissions.
       */
      val offsetChanges = RxAppBarLayout.offsetChanges(appBarLayout).share()
      val directionChanges = offsetChanges
          .buffer(2, 1) // Buffer in pairs to compare the previous, skip the first
          .filter { it[1] != it[0] } // We don't care about no changes. Shouldn't happen, but just in case
          .map { ScrollDirection.resolve(it[1], it[0]) }  // Map to a direction
          .startWith(UP)
          .distinctUntilChanged() // Only emit when it changes

      Observables
          .combineLatest(offsetChanges, directionChanges) { offset, direction ->
            Pair(offset, direction)
          }
          .subscribe { (offset, _) ->
            // Note: Direction is unused for now but left because this was neat
            val percentage = Math.abs(offset).toFloat() / translatableHeight

            // Force versions outside boundaries to be safe
            if (percentage > 0.75F) {
              bannerIcon.alpha = 0F
              aboutText.alpha = 0F
            }
            if (percentage < 0.5F) {
              title.translationX = 0F
              title.translationY = 0F
            }
            if (percentage < 0.75F) {
              // We want to accelerate fading to be the first 75% of the translation, so adjust
              // accordingly below and use the new calculated percentage for our interpolation
              val adjustedPercentage = 1 - (percentage * 1.33F)
              val interpolation = interpolator.getInterpolation(adjustedPercentage)
              bannerIcon.alpha = interpolation
              aboutText.alpha = interpolation
            }
            if (percentage > 0.50F) {
              // Start translating about halfway through (to give a staggered effect next to the alpha
              // so they have time to fade out sufficiently). From here we just set translation offsets
              // to adjust the position naturally to give the appearance of settling in to the right
              // place.
              val adjustedPercentage = (1 - percentage) * 2F
              val interpolation = interpolator.getInterpolation(adjustedPercentage)
              title.translationX = -(xDelta - (interpolation * xDelta))
              title.translationY = yDelta - (interpolation * yDelta)
            }
          }
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
            progressBar.makeGone()
          }
          .subscribe { data, error ->
            if (data != null) {
              adapter.setItems(data)
            } else {
              // TODO Show a better error
              e(error) { "Could not load open source licenses." }
              Snackbar.make(recyclerView, "Could not load open source licenses.",
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
                  Pair(id(), owner().id())
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
                  .map { it.data()!!.nodes().map { it.asRepository()!! } },
              // Fetch the users by their IDs
              Rx2Apollo.from(apolloClient.query(ProjectOwnersByIdsQuery(it.values.distinct()))
                  .httpCachePolicy(HttpCachePolicy.CACHE_FIRST))
                  .flatMapIterable { it.data()!!.nodes() }
                  // Reduce into a map of the owner ID -> display name
                  .reduce(mutableMapOf()) { map, node ->
                    map.apply {
                      val (id, name) = node.asOrganization()?.let {
                        Pair(it.id(), it.name() ?: it.login())
                      } ?: with(node.asUser()!!) {
                        Pair(id(), name() ?: login())
                      }
                      map.put(id, name)
                    }
                  },
              // Map the repos and their corresponding user display name into a list of their pairs
              BiFunction { nodes: List<AsRepository>, userIdToNameMap: Map<String, String> ->
                nodes.map {
                  Pair(it, userIdToNameMap[it.owner().id()]!!)
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
            collector.add(OssItemHeader(
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

  private inner class Adapter : RecyclerView.Adapter<ViewHolder>(),
      StickyHeaders, StickyHeaders.ViewSetup {

    private val items = mutableListOf<OssBaseItem>()

    override fun isStickyHeader(position: Int) = items[position] is OssItemHeader

    override fun setupStickyHeaderView(stickyHeader: View) {
      stickyHeader.animate()
          .z(stickyHeader.resources.dp2px(4f))
          .setInterpolator(fastOutSlowInInterpolator)
          .setDuration(200)
          .start()
    }

    override fun teardownStickyHeaderView(stickyHeader: View) {
      stickyHeader.animate()
          .z(0f)
          .setInterpolator(fastOutSlowInInterpolator)
          .setDuration(200)
          .start()
    }

    fun setItems(newItems: List<OssBaseItem>) {
      items.addAll(newItems)
      notifyItemRangeInserted(0, newItems.size)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val item = items[position]
      when (item) {
        is OssItemHeader -> (holder as HeaderHolder).run {
          Glide.with(itemView)
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

                override fun onGenerated(palette: Palette) {
                  palette.orderedSwatches { it.hsl[2] < 0.5F }?.let {
                    holder.title.setTextColor(it.rgb)
                  }
                }
              })
          title.text = item.name
        }
        is OssItem -> (holder as CatchUpItemViewHolder).apply {
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
                Completable.fromAction {
                  val context = itemView.context
                  customTab.openCustomTab(context,
                      customTab.customTabIntent
                          .setStartAnimations(context, R.anim.slide_up, R.anim.inset)
                          .setExitAnimations(context, R.anim.outset, R.anim.slide_down)
                          .setToolbarColor(tag.textColors.defaultColor)
                          .build(),
                      Uri.parse(item.clickUrl))
                }
              }
              .autoDisposeWith(holder)
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
}

private enum class ScrollDirection {
  UP, DOWN;

  companion object {
    fun resolve(current: Int, prev: Int): ScrollDirection {
      return if (current > prev) {
        DOWN
      } else {
        UP
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
  @BindView(R.id.icon) lateinit var icon: ImageView
  @BindView(R.id.title) lateinit var title: TextView
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
    val description: String?
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
              license = license,
              clickUrl = clickUrl!!,
              description = description)
        }
      }
    }
  }
}

@PerController
@Subcomponent(modules = arrayOf(AboutModule::class))
interface AboutComponent : AndroidInjector<AboutController> {

  @Subcomponent.Builder
  abstract class Builder : AndroidInjector.Builder<AboutController>()
}

@Module(subcomponents = arrayOf(AboutComponent::class))
abstract class AboutControllerBindingModule {

  @Binds
  @IntoMap
  @ControllerKey(AboutController::class)
  internal abstract fun bindAboutControllerInjectorFactory(
      builder: AboutComponent.Builder): AndroidInjector.Factory<out Controller>
}

@dagger.Module
internal object AboutModule {

  private val SERVER_URL = "https://api.github.com/graphql"

  @Qualifier
  private annotation class InternalApi

  @Qualifier
  annotation class ForAbout

  @Provides
  @JvmStatic
  @PerController
  @ForAbout
  internal fun provideAboutMoshi(moshi: Moshi): Moshi {
    return moshi.newBuilder()
        .add(OssItem::class.java, OssItem.adapter())
        .add(OssGitHubEntry::class.java, OssGitHubEntry.adapter())
        .build()
  }

  @Provides
  @JvmStatic
  @PerController
  internal fun provideHttpCacheStore(@ApplicationContext context: Context): HttpCacheStore {
    return DiskLruHttpCacheStore(context.cacheDir, 1_000_000)
  }

  @Provides
  @JvmStatic
  @PerController
  internal fun provideHttpCache(httpCacheStore: HttpCacheStore): HttpCache {
    return HttpCache(httpCacheStore, ApolloLogger(Optional.absent()))
  }

  @Provides
  @InternalApi
  @JvmStatic
  @PerController
  internal fun provideGitHubOkHttpClient(
      client: OkHttpClient,
      httpCache: HttpCache): OkHttpClient {
    return client.newBuilder()
        .addInterceptor(httpCache.interceptor())
        .addInterceptor(AuthInterceptor.create("token", BuildConfig.GITHUB_DEVELOPER_TOKEN))
        .build()
  }

  @Provides
  @JvmStatic
  @PerController
  internal fun provideCacheKeyResolver(): CacheKeyResolver {
    return object : CacheKeyResolver() {
      private val formatter = { id: String ->
        if (id.isEmpty()) {
          CacheKey.NO_KEY
        } else {
          CacheKey.from(id)
        }
      }

      override fun fromFieldRecordSet(field: ResponseField,
          objectSource: Map<String, Any>): CacheKey {
        // Most objects use id
        objectSource["id"].let {
          return when (it) {
            is String -> formatter(it)
            else -> CacheKey.NO_KEY
          }
        }
      }

      override fun fromFieldArguments(field: ResponseField,
          variables: Operation.Variables): CacheKey {
        return CacheKey.NO_KEY
      }
    }
  }

  @Provides
  @JvmStatic
  @PerController
  internal fun provideNormalizedCacheFactory(
      @ApplicationContext context: Context): NormalizedCacheFactory<*> {
    val apolloSqlHelper = ApolloSqlHelper(context, "aboutdb")
    return LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION,
        SqlNormalizedCacheFactory(apolloSqlHelper))
  }

  @Provides
  @JvmStatic
  @PerController
  internal fun provideApolloClient(@InternalApi client: Lazy<OkHttpClient>,
      cacheFactory: NormalizedCacheFactory<*>,
      resolver: CacheKeyResolver,
      httpCacheStore: HttpCacheStore): ApolloClient {
    return ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .httpCacheStore(httpCacheStore)
        .okHttpClient(client.get())
//          .callFactory { client.get().newCall(it) }
        .normalizedCache(cacheFactory, resolver)
        .addCustomTypeAdapter<Instant>(CustomType.DATETIME, ISO8601InstantApolloAdapter())
        .addCustomTypeAdapter<HttpUrl>(CustomType.URI, HttpUrlApolloAdapter())
        .build()
  }
}
