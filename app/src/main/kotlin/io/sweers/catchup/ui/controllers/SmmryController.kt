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

package io.sweers.catchup.ui.controllers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import butterknife.BindView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.squareup.moshi.Moshi
import com.uber.autodispose.kotlin.autoDisposable
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
import io.sweers.catchup.service.api.Service
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.SummarizationType
import io.sweers.catchup.service.api.SummarizationType.NONE
import io.sweers.catchup.service.api.SummarizationType.TEXT
import io.sweers.catchup.service.api.SummarizationType.URL
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.ui.controllers.SmmryController.Module.ForSmmry
import io.sweers.catchup.ui.widget.ElasticDragDismissFrameLayout
import io.sweers.catchup.ui.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback
import io.sweers.catchup.util.e
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.show
import io.sweers.catchup.util.w
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Qualifier

/**
 * Overlay controller for displaying Smmry API results.
 */
class SmmryController : ButterKnifeController {

  companion object {

    private const val ID_TITLE = "smmrycontroller.title"
    private const val ID_ID = "smmrycontroller.id"
    private const val ID_VALUE = "smmrycontroller.value"
    private const val ID_TYPE = "smmrycontroller.type"
    private const val ID_ACCENT = "smmrycontroller.accent"
    private const val ID_LOADED = "smmrycontroller.loaded"

    fun <T> showFor(controller: Controller,
        service: Service,
        title: String,
        id: String,
        info: SummarizationInfo) = Consumer<T> { _ ->
      controller.router
          .pushController(RouterTransaction.with(SmmryController(id,
              ContextCompat.getColor(controller.activity!!, service.meta().themeColor),
              title,
              info))
              .pushChangeHandler(VerticalChangeHandler(false))
              .popChangeHandler(VerticalChangeHandler()))
    }
  }

  @Inject
  lateinit var smmryService: SmmryService
  @field:ForSmmry
  @Inject
  lateinit var moshi: Moshi
  @Inject
  lateinit var smmryDao: SmmryDao

  @BindView(R.id.loading_view)
  lateinit var loadingView: View
  @BindView(R.id.smmry_loading)
  lateinit var lottieView: LottieAnimationView
  @BindView(R.id.content_container)
  lateinit var content: NestedScrollView
  @BindView(R.id.tags)
  lateinit var tags: TextView
  @BindView(R.id.title)
  lateinit var title: TextView
  @BindView(R.id.summary)
  lateinit var summary: TextView
  @BindView(R.id.drag_dismiss_layout)
  lateinit var dragDismissFrameLayout: ElasticDragDismissFrameLayout

  private lateinit var id: String
  private lateinit var info: SummarizationInfo
  @ColorInt
  private var accentColor: Int = 0
  private lateinit var inputTitle: String
  private var alreadyLoaded = false

  private val dragDismissListener = object : ElasticDragDismissCallback() {
    override fun onDragDismissed() {
      router.popController(this@SmmryController)
      dragDismissFrameLayout.removeListener(this)
    }
  }

  constructor(args: Bundle) : super(args)

  constructor(id: String, @ColorInt accentColor: Int, inputTitle: String, info: SummarizationInfo) {
    this.id = id
    this.accentColor = accentColor
    this.inputTitle = inputTitle
    this.info = info
  }

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(ID_TITLE, inputTitle)
    outState.putString(ID_ID, id)
    outState.putString(ID_VALUE, info.value)
    outState.putString(ID_TYPE, info.type.name)
    outState.putInt(ID_ACCENT, accentColor)
    outState.putBoolean(ID_LOADED, alreadyLoaded)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    inputTitle = savedInstanceState.getString(ID_TITLE)!!
    id = savedInstanceState.getString(ID_ID)!!
    val value = savedInstanceState.getString(ID_VALUE)!!
    val type = SummarizationType.valueOf(savedInstanceState.getString(ID_TYPE)!!)
    info = SummarizationInfo(value, type)
    accentColor = savedInstanceState.getInt(ID_ACCENT)
    alreadyLoaded = savedInstanceState.getBoolean(ID_LOADED, false)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View =
      inflater.inflate(R.layout.controller_smmry, container, false)

  @SuppressLint("RestrictedApi") // False positive
  override fun onViewBound(view: View) {
    super.onViewBound(view)
    content.isNestedScrollingEnabled = true
    if (!alreadyLoaded) {
      lottieView.addValueCallback<ColorFilter>(KeyPath("**"),
          LottieProperty.COLOR_FILTER,
          LottieValueCallback<ColorFilter>(SimpleColorFilter(accentColor)))
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
        .autoDisposable(this)
        .subscribe({ smmryResponse ->
          alreadyLoaded = true
          when (smmryResponse) {
            is Success -> {
              showSummary(smmryResponse)
            }
            else -> {
              val message = when (smmryResponse) {
                is InternalError -> "Smmry internal error - ${smmryResponse.message}"
                is IncorrectVariables -> "Smmry invalid input - ${smmryResponse.message}"
                is ApiRejection -> "Smmry API error - ${smmryResponse.message}"
                is SummarizationError -> "Smmry summarization error - ${smmryResponse.message}"
                UnknownErrorCode -> "Unknown error :("
                else -> TODO("Placeholder because I've already checked for this")
              }
              Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
              router.popController(this@SmmryController)
            }
          }
        }, { error ->
          Toast.makeText(activity, R.string.unknown_issue, Toast.LENGTH_SHORT).show()
          if (error is IOException) {
            w(error) { "Unknown error in smmry load" }
          } else {
            e(error) { "Unknown error in smmry load" }
          }
          router.popController(this@SmmryController)
        })
  }

  private fun tryRequestFromStorage(): Maybe<SmmryResponse> {
    return smmryDao.getItem(id)
        .map { moshi.adapter(SmmryResponse::class.java).fromJson(it.json)!! }
        .onErrorComplete { exception ->
          e(exception) { "Error loading smmry cache." }
          true
        }
  }

  private fun getRequestSingle(): Single<SmmryResponse> {
    val summarizer: Single<SmmryResponse> = when (info.type) {
      TEXT -> smmryService.summarizeText(SmmryRequestBuilder.forText()
          .withBreak(true)
          .keywordCount(5)
          .sentenceCount(5)
          .build(),
          info.value)
      URL -> smmryService.summarizeUrl(SmmryRequestBuilder.forUrl(info.value)
          .withBreak(true)
          .keywordCount(5)
          .sentenceCount(5)
          .build())
      NONE -> Single.just(Success.just(inputTitle, info.value))
    }
    return summarizer.doOnSuccess {
      if (it != UnknownErrorCode) {
        Completable
            .fromAction {
              smmryDao.putItem(SmmryStorageEntry(id,
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
    if (smmry.keywords != null) {
      tags.setTextColor(accentColor)
      tags.text = TextUtils.join("  —  ",
          Observable.fromIterable(smmry.keywords)
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
    var smmryTitle = smmry.title
    if (TextUtils.isEmpty(smmryTitle)) {
      smmryTitle = inputTitle
    }
    title.text = smmryTitle
    summary.text = smmry.content
        .replace("[BREAK]", "\n\n")
    if (loadingView.isVisible) {
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
    if (content.isGone) {
      content.alpha = 0f
      content.show()
      content.animate()
          .alpha(1f)
    }
  }

  override fun bind(view: View) = SmmryController_ViewBinding(this, view)

  @PerController
  @Subcomponent(modules = [Module::class])
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
    internal fun provideServiceDao(catchUpDatabase: CatchUpDatabase) = catchUpDatabase.smmryDao()
  }
}

private const val TABLE = "smmryEntries"

@Keep
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
