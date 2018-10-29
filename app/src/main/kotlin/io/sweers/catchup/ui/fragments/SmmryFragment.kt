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

package io.sweers.catchup.ui.fragments

import android.annotation.SuppressLint
import android.graphics.ColorFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.widget.NestedScrollView
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.squareup.moshi.Moshi
import com.uber.autodispose.autoDisposable
import dagger.Lazy
import dagger.Provides
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.BuildConfig
import io.sweers.catchup.R
import io.sweers.catchup.data.smmry.SmmryModule.ForSmmry
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
import io.sweers.catchup.injection.scopes.PerFragment
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.SummarizationType
import io.sweers.catchup.service.api.SummarizationType.NONE
import io.sweers.catchup.service.api.SummarizationType.TEXT
import io.sweers.catchup.service.api.SummarizationType.URL
import io.sweers.catchup.ui.base.InjectableBaseFragment
import io.sweers.catchup.ui.fragments.SmmryFragment.Module.ForSmmry
import io.sweers.catchup.util.e
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.show
import io.sweers.catchup.util.w
import kotterknife.bindView
import javax.inject.Inject

/**
 * Overlay fragment for displaying Smmry API results.
 */
class SmmryFragment : InjectableBaseFragment() {

  companion object {
    private const val ID_TITLE = "smmryfragment.title"
    private const val ID_ID = "smmryfragment.id"
    private const val ID_VALUE = "smmryfragment.value"
    private const val ID_TYPE = "smmryfragment.type"
    private const val ID_ACCENT = "smmryfragment.accent"
    private const val ID_LOADED = "smmryfragment.loaded"

    fun newInstance(id: String, @ColorInt accentColor: Int, inputTitle: String,
        info: SummarizationInfo): SmmryFragment {
      return SmmryFragment().apply {
        arguments = bundleOf(
            ID_ID to id,
            ID_ACCENT to accentColor,
            ID_TITLE to inputTitle,
            ID_VALUE to info.value,
            ID_TYPE to info.type.name
        )
      }
    }
  }

  @Inject
  lateinit var smmryService: SmmryService
  @field:ForSmmry
  @Inject
  lateinit var moshi: Moshi
  @Inject
  lateinit var smmryDao: SmmryDao

  private val loadingView by bindView<View>(R.id.loading_view)
  private val lottieView by bindView<LottieAnimationView>(R.id.smmry_loading) {
    it.useHardwareAcceleration(true)
  }
  private val content by bindView<NestedScrollView>(R.id.content_container)
  private val tags by bindView<TextView>(R.id.tags)
  private val title by bindView<TextView>(R.id.title)
  private val summary by bindView<TextView>(R.id.summary)

  private lateinit var id: String
  private lateinit var info: SummarizationInfo
  @ColorInt
  private var accentColor: Int = 0
  private lateinit var inputTitle: String
  private var alreadyLoaded = false

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.run {
      putString(ID_TITLE, inputTitle)
      putString(ID_ID, id)
      putString(ID_VALUE, info.value)
      putString(ID_TYPE, info.type.name)
      putInt(ID_ACCENT, accentColor)
      putBoolean(ID_LOADED, alreadyLoaded)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (savedInstanceState ?: arguments)?.run {
      inputTitle = getString(ID_TITLE)!!
      id = getString(ID_ID)!!
      val value = getString(ID_VALUE)!!
      val type = SummarizationType.valueOf(getString(ID_TYPE)!!)
      info = SummarizationInfo(value, type)
      accentColor = getInt(ID_ACCENT)
      alreadyLoaded = getBoolean(ID_LOADED, false)
    }
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_smmry, container, false)
  }

  @SuppressLint("RestrictedApi") // False positive
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    content.isNestedScrollingEnabled = true
    title.text = inputTitle
    if (!alreadyLoaded) {
      loadingView.show()
      lottieView.addValueCallback<ColorFilter>(KeyPath("**"),
          LottieProperty.COLOR_FILTER,
          LottieValueCallback<ColorFilter>(SimpleColorFilter(accentColor)))
    } else {
      loadingView.hide()
    }
    Maybe.concatArray(tryRequestFromStorage(), getRequestSingle().toMaybe())
        .firstOrError()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .autoDisposable(this)
        .subscribe({ smmryResponse ->
          alreadyLoaded = true
          loadingView.hide(true)
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
                else -> TODO("Give me sealed whens!")
              }
              summary.text = message
            }
          }
        }, { error ->
          loadingView.hide(true)
          summary.setText(R.string.unknown_issue)
          if (error is IOException) {
            w(error) { "Unknown error in smmry load" }
          } else {
            e(error) { "Unknown error in smmry load" }
          }
        })
  }

  fun canScrollVertically(directionInt: Int): Boolean {
    return content.canScrollVertically(directionInt)
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
        smmryDao.putItem(SmmryStorageEntry(
            url = id,
            json = moshi.adapter(SmmryResponse::class.java).toJson(it)))
            .blockingAwait()
      }
    }
  }

  @SuppressLint("SetTextI18n")
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
    summary.text = smmry.title.takeIf { it.isNotEmpty() }?.let { "$it\n\n" }.orEmpty() + smmry.content
        .replace("[BREAK]", "\n\n")
    if (content.isGone) {
      content.show(true)
    }
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
  fun putItem(item: SmmryStorageEntry): Completable

  @Query("DELETE FROM $TABLE")
  fun nukeItems()
}
