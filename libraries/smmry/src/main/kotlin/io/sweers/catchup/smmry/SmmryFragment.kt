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
package io.sweers.catchup.smmry

import android.annotation.SuppressLint
import android.graphics.ColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import io.sweers.catchup.base.ui.InjectableBaseFragment
import io.sweers.catchup.service.api.ScrollableContent
import io.sweers.catchup.service.api.SummarizationInfo
import io.sweers.catchup.service.api.SummarizationType
import io.sweers.catchup.service.api.SummarizationType.NONE
import io.sweers.catchup.service.api.SummarizationType.TEXT
import io.sweers.catchup.service.api.SummarizationType.URL
import io.sweers.catchup.smmry.SmmryModule.ForSmmry
import io.sweers.catchup.smmry.model.ApiRejection
import io.sweers.catchup.smmry.model.IncorrectVariables
import io.sweers.catchup.smmry.model.InternalError
import io.sweers.catchup.smmry.model.SmmryDao
import io.sweers.catchup.smmry.model.SmmryRequestBuilder
import io.sweers.catchup.smmry.model.SmmryResponse
import io.sweers.catchup.smmry.model.SmmryStorageEntry
import io.sweers.catchup.smmry.model.Success
import io.sweers.catchup.smmry.model.SummarizationError
import io.sweers.catchup.smmry.model.UnknownErrorCode
import io.sweers.catchup.util.hide
import io.sweers.catchup.util.show
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotterknife.bindView
import java.util.Locale
import javax.inject.Inject

/**
 * Overlay fragment for displaying Smmry API results.
 */
class SmmryFragment : InjectableBaseFragment(), ScrollableContent {

  companion object {
    private const val ID_TITLE = "smmryfragment.title"
    private const val ID_ID = "smmryfragment.id"
    private const val ID_VALUE = "smmryfragment.value"
    private const val ID_TYPE = "smmryfragment.type"
    private const val ID_ACCENT = "smmryfragment.accent"
    private const val ID_LOADED = "smmryfragment.loaded"

    fun newInstance(
      id: String,
      @ColorInt accentColor: Int,
      inputTitle: String,
      info: SummarizationInfo
    ): SmmryFragment {
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
  private val lottieView by bindView<LottieAnimationView>(R.id.smmry_loading)
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

  override fun inflateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.fragment_smmry, container, false)
  }

  @SuppressLint("RestrictedApi") // False positive
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    content.isNestedScrollingEnabled = true
    title.text = inputTitle
    if (!alreadyLoaded) {
      loadingView.show()
      lottieView.addValueCallback(KeyPath("**"),
          LottieProperty.COLOR_FILTER,
          LottieValueCallback<ColorFilter>(SimpleColorFilter(accentColor)))
    } else {
      loadingView.hide()
    }
    viewLifecycleOwner.lifecycleScope.launch {
      val response = tryRequestFromStorage() ?: fetchFromNetwork()
      if (!isActive) {
        // TODO why is this necessary? One of the above suspends are not cooperating with isActive()
        return@launch
      }
      alreadyLoaded = true
      loadingView.hide(animate = true)
      when (response) {
        is Success -> {
          showSummary(response)
        }
        else -> {
          val message = when (response) {
            is InternalError -> "Smmry internal error - ${response.message}"
            is IncorrectVariables -> "Smmry invalid input - ${response.message}"
            is ApiRejection -> "Smmry API error - ${response.message}"
            is SummarizationError -> "Smmry summarization error - ${response.message}"
            UnknownErrorCode -> getString(io.sweers.catchup.base.ui.R.string.unknown_issue)
            else -> throw UnsupportedOperationException("Success is already covered above")
          }
          summary.text = message
        }
      }
    }
  }

  override fun canScrollVertically(directionInt: Int): Boolean {
    return content.canScrollVertically(directionInt)
  }

  private suspend fun tryRequestFromStorage() = smmryDao.getItem(id)?.let {
    moshi.adapter(SmmryResponse::class.java).fromJson(it.json) ?: throw JsonDataException(
        "Could not parse entry")
  }

  private suspend fun fetchFromNetwork(): SmmryResponse {
    val response: SmmryResponse = try {
      when (info.type) {
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
        NONE -> Success.just(inputTitle, info.value)
      }
    } catch (error: Exception) {
      // We should indicate when something's network related
      UnknownErrorCode
    }
    if (response != UnknownErrorCode) {
      smmryDao.putItem(SmmryStorageEntry(
          url = id,
          json = moshi.adapter(SmmryResponse::class.java).toJson(response)))
    }
    return response
  }

  @SuppressLint("SetTextI18n")
  private fun showSummary(smmry: Success) {
    if (smmry.keywords != null) {
      tags.setTextColor(accentColor)
      tags.text = smmry.keywords.joinToString("  —  ") { s ->
        s.trim { it <= ' ' }.toUpperCase(Locale.getDefault())
      }
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
