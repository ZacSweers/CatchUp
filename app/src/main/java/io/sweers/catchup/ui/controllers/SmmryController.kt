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
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Subcomponent
import dagger.android.AndroidInjector
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.sweers.catchup.R
import io.sweers.catchup.data.smmry.SmmryService
import io.sweers.catchup.data.smmry.model.SmmryRequestBuilder
import io.sweers.catchup.data.smmry.model.SmmryResponse
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.rx.observers.adapter.SingleObserverAdapter
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.ui.base.ServiceController
import io.sweers.catchup.ui.widget.ElasticDragDismissFrameLayout
import io.sweers.catchup.ui.widget.ElasticDragDismissFrameLayout.ElasticDragDismissCallback
import javax.inject.Inject

/**
 * Overlay controller for displaying Smmry API results.
 */
class SmmryController : ButterKnifeController {

  @Inject lateinit var smmryService: SmmryService

  @BindView(R.id.loading_view) lateinit var loadingView: View
  @BindView(R.id.progress) lateinit var lottieView: LottieAnimationView
  @BindView(R.id.content_container) lateinit var content: NestedScrollView
  @BindView(R.id.tags) lateinit var tags: TextView
  @BindView(R.id.title) lateinit var title: TextView
  @BindView(R.id.summary) lateinit var summary: TextView
  @BindView(R.id.drag_dismiss_layout)
  lateinit var dragDismissFrameLayout: ElasticDragDismissFrameLayout

  private lateinit var url: String
  @ColorInt private var accentColor: Int = 0
  private lateinit var fallbackTitle: String

  private val dragDismissListener = object : ElasticDragDismissCallback() {
    override fun onDragDismissed() {
      router.popController(this@SmmryController)
      dragDismissFrameLayout.removeListener(this)
    }
  }

  constructor(args: Bundle) : super(args)

  constructor(url: String, @ColorInt accentColor: Int, fallbackTitle: String) {
    this.url = url
    this.accentColor = accentColor
    this.fallbackTitle = fallbackTitle.trim { it <= ' ' }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(ID_TITLE, fallbackTitle)
    outState.putString(ID_URL, url)
    outState.putInt(ID_ACCENT, accentColor)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    fallbackTitle = savedInstanceState.getString(ID_TITLE)
    url = savedInstanceState.getString(ID_URL)
    accentColor = savedInstanceState.getInt(ID_ACCENT)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_smmry, container, false)
  }

  override fun onViewBound(view: View) {
    super.onViewBound(view)
    val colorFilter = SimpleColorFilter(accentColor)
    lottieView.addColorFilter(colorFilter)
  }

  override fun onAttach(view: View) {
    ConductorInjection.inject(this)
    super.onAttach(view)
    dragDismissFrameLayout.addListener(dragDismissListener)
    smmryService.summarizeUrl(SmmryRequestBuilder.forUrl(url)
        .withBreak(true)
        .keywordCount(5)
        .sentenceCount(5)
        .build())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .autoDisposeWith(this)
        .subscribe(object : SingleObserverAdapter<SmmryResponse>() {
          override fun onSuccess(value: SmmryResponse) {
            if (value.apiMessage() != null) {
              Toast.makeText(activity,
                  "Smmry Error: " + value.errorCode() + " - " + value.apiMessage(),
                  Toast.LENGTH_LONG)
                  .show()
              router.popController(this@SmmryController)
            } else {
              showSummary(value)
            }
          }

          override fun onError(e: Throwable) {
            Toast.makeText(activity, "API error", Toast.LENGTH_SHORT)
                .show()
            router.popController(this@SmmryController)
          }
        })
  }

  override fun onDetach(view: View) {
    dragDismissFrameLayout.removeListener(dragDismissListener)
    super.onDetach(view)
  }

  private fun showSummary(smmry: SmmryResponse) {
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
      tags.visibility = View.VISIBLE
    } else {
      tags.visibility = View.GONE
    }
    var smmryTitle = smmry.title()
    if (TextUtils.isEmpty(smmryTitle)) {
      smmryTitle = fallbackTitle
    }
    title.text = smmryTitle
    summary.text = smmry.content()!!
        .replace("[BREAK]", "\n\n")
    loadingView.animate()
        .alpha(0f)
        .setListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            loadingView.visibility = View.GONE
            loadingView.animate()
                .setListener(null)
          }
        })
    content.isNestedScrollingEnabled = true
    content.alpha = 0f
    content.visibility = View.VISIBLE
    content.animate()
        .alpha(1f)
  }

  override fun bind(view: View): Unbinder {
    return SmmryController_ViewBinding(this, view)
  }

  @Subcomponent
  interface Component : AndroidInjector<SmmryController> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<SmmryController>()
  }

  companion object {

    private val ID_TITLE = "smmrycontroller.title"
    private val ID_URL = "smmrycontroller.url"
    private val ID_ACCENT = "smmrycontroller.accent"

    fun <T> showFor(controller: ServiceController,
        url: String,
        fallbackTitle: String) = Consumer<T> { _ ->
      // TODO Optimize this
      // Exclude images
      // Summarize reddit selftexts
      controller.router
          .pushController(RouterTransaction.with(SmmryController(url,
              controller.serviceThemeColor,
              fallbackTitle))
              .pushChangeHandler(VerticalChangeHandler(false))
              .popChangeHandler(VerticalChangeHandler()))
    }
  }
}
