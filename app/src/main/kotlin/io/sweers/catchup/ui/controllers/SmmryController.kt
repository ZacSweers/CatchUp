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

package io.sweers.catchup.ui.controllers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import android.content.Context
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.widget.NestedScrollView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.Unbinder
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.SimpleColorFilter
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.squareup.moshi.Moshi
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Lazy
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.CatchUpDatabase
import io.sweers.catchup.data.smmry.SmmryService
import io.sweers.catchup.data.smmry.model.ApiRejection
import io.sweers.catchup.data.smmry.model.IncorrectVariables
import io.sweers.catchup.data.smmry.model.InternalError
import io.sweers.catchup.data.smmry.model.SmmryRequestBuilder
import io.sweers.catchup.data.smmry.model.SmmryResponse
import io.sweers.catchup.data.smmry.model.SmmryResponseFactory
import io.sweers.catchup.data.smmry.model.Success
import io.sweers.catchup.data.smmry.model.SummarizationError
import io.sweers.catchup.data.smmry.model.UnknownErrorCode
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.rx.observers.adapter.SingleObserverAdapter
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.ui.base.ServiceController
import io.sweers.catchup.ui.controllers.SmmryController.Module.ForSmmry
import io.sweers.catchup.ui.widget.ElasticDragDismissFrameLayout
import io.sweers.catchup.ui.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback
import io.sweers.catchup.util.e
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.isGone
import io.sweers.catchup.util.isVisible
import io.sweers.catchup.util.show
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Qualifier

/**
 * Overlay controller for displaying Smmry API results.
 */
class SmmryController : ButterKnifeController {

  companion object {

    private val ID_TITLE = "smmrycontroller.title"
    private val ID_URL = "smmrycontroller.url"
    private val ID_TEXT = "smmrycontroller.text"
    private val ID_ACCENT = "smmrycontroller.accent"
    private val ID_LOADED = "smmrycontroller.loaded"

    /**
     * Really shallow sanity check
     */
    fun canSummarize(url: String, text: String? = null): Boolean {
      text?.let {
        if (it.isEmpty()) {
          return false
        } else if (it.length < 50) {
          return false
        }
      }

      if (url.endsWith(".png")
          || url.endsWith(".gifv")
          || url.endsWith(".jpg")
          || url.endsWith(".jpeg")) {
        return false
      }

      HttpUrl.parse(url)?.let {
        it.host().let {
          if (it.contains("imgur")
              || it.contains("streamable")
              || it.contains("gfycat")
              || it.contains("i.reddit")
              || it.contains("v.reddit")
              || it.contains("youtube")
              || it.contains("youtu.be"))
            return false
        }
      }

      return true
    }

    fun <T> showFor(controller: ServiceController,
        url: String,
        inputTitle: String,
        text: String? = null) = Consumer<T> { _ ->
      controller.router
          .pushController(RouterTransaction.with(SmmryController(url,
              controller.serviceThemeColor,
              inputTitle,
              text))
              .pushChangeHandler(VerticalChangeHandler(false))
              .popChangeHandler(VerticalChangeHandler()))
    }
  }

  @Inject lateinit var smmryService: SmmryService
  @field:ForSmmry @Inject lateinit var moshi: Moshi
  @Inject lateinit var smmryDao: SmmryDao

  @BindView(R.id.loading_view) lateinit var loadingView: View
  @BindView(R.id.smmry_loading) lateinit var lottieView: LottieAnimationView
  @BindView(R.id.content_container) lateinit var content: NestedScrollView
  @BindView(R.id.tags) lateinit var tags: TextView
  @BindView(R.id.title) lateinit var title: TextView
  @BindView(R.id.summary) lateinit var summary: TextView
  @BindView(R.id.drag_dismiss_layout)
  lateinit var dragDismissFrameLayout: ElasticDragDismissFrameLayout

  private lateinit var url: String
  @ColorInt private var accentColor: Int = 0
  private lateinit var inputTitle: String
  private var text: String? = null
  private var alreadyLoaded = false

  private val dragDismissListener = object : ElasticDragDismissCallback() {
    override fun onDragDismissed() {
      router.popController(this@SmmryController)
      dragDismissFrameLayout.removeListener(this)
    }
  }

  constructor(args: Bundle) : super(args)

  constructor(url: String, @ColorInt accentColor: Int, inputTitle: String, text: String? = null) {
    this.url = url
    this.accentColor = accentColor
    this.inputTitle = inputTitle
    this.text = text
  }

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(ID_TITLE, inputTitle)
    outState.putString(ID_URL, url)
    outState.putString(ID_TEXT, text)
    outState.putInt(ID_ACCENT, accentColor)
    outState.putBoolean(ID_LOADED, alreadyLoaded)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    inputTitle = savedInstanceState.getString(ID_TITLE)!!
    url = savedInstanceState.getString(ID_URL)!!
    text = savedInstanceState.getString(ID_TEXT)
    accentColor = savedInstanceState.getInt(ID_ACCENT)
    alreadyLoaded = savedInstanceState.getBoolean(ID_LOADED, false)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_smmry, container, false)
  }

  override fun onViewBound(view: View) {
    super.onViewBound(view)
    content.isNestedScrollingEnabled = true
    if (!alreadyLoaded) {
      val colorFilter = SimpleColorFilter(accentColor)
      lottieView.addColorFilter(colorFilter)
    } else {
      loadingView.hide()
      content.show()
    }
  }

  override fun onAttach(view: View) {
    super.onAttach(view)
    dragDismissFrameLayout.addListener(dragDismissListener)
    Maybe.concatArray(tryRequestFromStorage(), getRequestSingle().toMaybe())
        .firstOrError()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .autoDisposeWith(this)
        .subscribe(object : SingleObserverAdapter<SmmryResponse>() {
          override fun onSuccess(value: SmmryResponse) {
            alreadyLoaded = true
            when (value) {
              is Success -> {
                showSummary(value)
              }
              else -> {
                val message = when (value) {
                  is InternalError -> "Smmry internal error - ${value.message}"
                  is IncorrectVariables -> "Smmry invalid input - ${value.message}"
                  is ApiRejection -> "Smmry API error - ${value.message}"
                  is SummarizationError -> "Smmry summarization error - ${value.message}"
                  is UnknownErrorCode -> "Unknown error :("
                  else -> TODO("Placeholder because I've already checked for this")
                }
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                router.popController(this@SmmryController)
              }
            }
          }

          override fun onError(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") error: Throwable) {
            Toast.makeText(activity, "Unknown error", Toast.LENGTH_SHORT).show()
            e(error) { "Unknown error in smmry load" }
            router.popController(this@SmmryController)
          }
        })
  }

  private fun tryRequestFromStorage(): Maybe<SmmryResponse> {
    return smmryDao.getItem(url)
        .map { moshi.adapter(SmmryResponse::class.java).fromJson(it.json)!! }
        .onErrorComplete { exception ->
          e(exception) { "Error loading smmry cache." }
          true
        }
  }

  private fun getRequestSingle(): Single<SmmryResponse> {
    return text?.let {
      smmryService.summarizeText(SmmryRequestBuilder.forText()
          .withBreak(true)
          .keywordCount(5)
          .sentenceCount(5)
          .build(),
          it)
    } ?: smmryService.summarizeUrl(SmmryRequestBuilder.forUrl(url)
        .withBreak(true)
        .keywordCount(5)
        .sentenceCount(5)
        .build()
    )
        .doOnSuccess {
          if (it !is UnknownErrorCode) {
            Completable
                .fromAction {
                  smmryDao.putItem(SmmryStorageEntry(url,
                      moshi.adapter(SmmryResponse::class.java).toJson(it)))
                }
                .blockingAwait()
          }
        }
  }

  override fun onDetach(view: View) {
    dragDismissFrameLayout.removeListener(dragDismissListener)
    super.onDetach(view)
  }

  private fun showSummary(smmry: Success) {
    if (smmry.keywords() != null) {
      tags.setTextColor(accentColor)
      tags.text = TextUtils.join("  —  ",
          Observable.fromIterable(smmry.keywords()!!)
              .map { s ->
                s.trim { it <= ' ' }
                    .toUpperCase()
              }
              .toList()
              .blockingGet())
      tags.show()
    } else {
      tags.hide()
    }
    var smmryTitle = smmry.title()
    if (TextUtils.isEmpty(smmryTitle)) {
      smmryTitle = inputTitle
    }
    title.text = smmryTitle
    summary.text = smmry.content()
        .replace("[BREAK]", "\n\n")
    if (loadingView.isVisible()) {
      loadingView.animate()
          .alpha(0f)
          .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
              loadingView.hide()
              loadingView.animate()
                  .setListener(null)
            }
          })
    }
    if (content.isGone()) {
      content.alpha = 0f
      content.show()
      content.animate()
          .alpha(1f)
    }
  }

  override fun bind(view: View): Unbinder {
    return SmmryController_ViewBinding(this, view)
  }

  @PerController
  @Subcomponent(modules = arrayOf(Module::class))
  interface Component : AndroidInjector<SmmryController> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<SmmryController>()
  }

  @dagger.Module
  object Module {

    @Qualifier
    annotation class ForSmmry

    @Provides
    @JvmStatic
    @ForSmmry
    @PerController
    internal fun provideSmmryMoshi(moshi: Moshi): Moshi {
      return moshi.newBuilder()
          .add(SmmryResponseFactory.getInstance())
          .build()
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideSmmryService(client: Lazy<OkHttpClient>,
        @ForSmmry moshi: Moshi,
        rxJavaCallAdapterFactory: RxJava2CallAdapterFactory): SmmryService {
      return Retrofit.Builder().baseUrl(SmmryService.ENDPOINT)
          .callFactory { request ->
            client.get()
                .newCall(request)
          }
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
          .create(SmmryService::class.java)
    }

    @Provides
    @JvmStatic
    @PerController
    internal fun provideServiceDao(catchUpDatabase: CatchUpDatabase): SmmryDao {
      return catchUpDatabase.smmryDao()
    }
  }
}

private const val TABLE = "smmryEntries"

@Entity(tableName = TABLE)
data class SmmryStorageEntry(
    @PrimaryKey val url: String,
    val json: String
)

@Dao
interface SmmryDao {

  @Query("SELECT * FROM $TABLE WHERE url = :url")
  fun getItem(url: String): Maybe<SmmryStorageEntry>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun putItem(item: SmmryStorageEntry)

  @Query("DELETE FROM $TABLE")
  fun nukeItems()
}
