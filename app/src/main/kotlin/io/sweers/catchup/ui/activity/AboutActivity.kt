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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.Px
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.NavUtils
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.Palette
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindDimen
import butterknife.BindView
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
import com.squareup.moshi.Moshi
import com.uber.autodispose.kotlin.autoDisposeWith
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import io.reactivex.Completable
import io.sweers.catchup.R
import io.sweers.catchup.R.layout
import io.sweers.catchup.injection.ConductorInjection
import io.sweers.catchup.injection.ControllerKey
import io.sweers.catchup.injection.scopes.PerActivity
import io.sweers.catchup.injection.scopes.PerController
import io.sweers.catchup.ui.base.BaseActivity
import io.sweers.catchup.ui.base.ButterKnifeController
import io.sweers.catchup.ui.base.CatchUpItemViewHolder
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper
import io.sweers.catchup.util.dp2px
import io.sweers.catchup.util.isInNightMode
import io.sweers.catchup.util.orderedSwatches
import io.sweers.catchup.util.setLightStatusBar
import javax.inject.Inject

class AboutActivity : BaseActivity() {

  @Inject internal lateinit var customTab: CustomTabActivityHelper
  @BindView(R.id.controller_container) internal lateinit var container: ViewGroup

  private lateinit var router: Router

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    customTab.doOnDestroy { connectionCallback = null }
    val viewGroup = viewContainer.forActivity(this)
    layoutInflater.inflate(R.layout.activity_about, viewGroup)

    AboutActivity_ViewBinding(this).doOnDestroy { unbind() }
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

  @BindDimen(R.dimen.avatar) @JvmField var dimenSize: Int = 0
  @BindView(R.id.toolbar) lateinit var toolbar: Toolbar
  @BindView(R.id.list) lateinit var recyclerView: RecyclerView
  @BindView(R.id.appbarlayout) lateinit var appBarLayout: AppBarLayout
  @BindView(R.id.ctl) lateinit var collapsingToolbar: CollapsingToolbarLayout
  @BindView(R.id.banner_container) lateinit var bannerContainer: View
  @BindView(R.id.banner_icon) lateinit var icon: ImageView
  @BindView(R.id.banner_title) lateinit var title: TextView
  @BindView(R.id.banner_text) lateinit var aboutText: TextView

  @Inject internal lateinit var moshi: Moshi
  @Inject internal lateinit var customTab: CustomTabActivityHelper

  override fun onContextAvailable(context: Context) {
    ConductorInjection.inject(this)
    super.onContextAvailable(context)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_about, container, false)
  }

  override fun bind(view: View) = AboutController_ViewBinding(this, view)

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
      appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
        val percentage = Math.abs(verticalOffset).toFloat() / translatableHeight
        // Force versions outside boundaries to be safe
        if (percentage > 0.75F) {
          icon.alpha = 0F
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
          icon.alpha = interpolation
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
    // Load data
    // Maybe for now manually add json
    // Would be neat if we could just keep GitHub URLs to projects and query GraphQL
    recyclerView.layoutManager = LinearLayoutManager(view.context)
    val itemList = mutableListOf<OssItem>()
    // Dummy data
    repeat(20) {
      itemList.add(OssItem(
          avatarUrl = "https://avatars3.githubusercontent.com/u/1361086?v=4&s=460",
          author = "Zac Sweers",
          name = "Barber",
          clickUrl = "https://github.com/hzsweers/barber",
          license = "Apache V2",
          description = "A styled attributes \"injection\" library."
      ))
    }
    recyclerView.adapter = Adapter(itemList)
  }

  private inner class Adapter(
      private val items: List<OssItem>) : RecyclerView.Adapter<CatchUpItemViewHolder>() {

    private val argbEvaluator = ArgbEvaluator()

    override fun onBindViewHolder(holder: CatchUpItemViewHolder, position: Int) {
      val item = items[position]
      holder.apply {
        icon.visibility = View.VISIBLE
        Glide.with(holder.itemView)
            .load(item.avatarUrl)
            .apply(RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .circleCrop()
                .override(dimenSize, dimenSize))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(object : DrawableImageViewTarget(holder.icon), Palette.PaletteAsyncListener {
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
                palette.orderedSwatches()?.let {
                  @ColorInt val startColor = holder.tag.textColors.defaultColor
                  @ColorInt val endColor = it.rgb
                  item.textColorAnimator?.cancel()
                  ValueAnimator.ofFloat(0f, 1f)
                      .apply {
                        interpolator = FastOutSlowInInterpolator()  // TODO Use singleton
                        duration = 400
                        addListener(object : AnimatorListenerAdapter() {
                          override fun onAnimationStart(animator: Animator, isReverse: Boolean) {
                            item.textColorAnimator = animator
                          }

                          override fun onAnimationEnd(animator: Animator) {
                            removeAllUpdateListeners()
                            removeListener(this)
                            item.textColorAnimator = null
                          }
                        })
                        addUpdateListener { animator ->
                          @ColorInt val color = argbEvaluator.evaluate(
                              animator.animatedValue as Float,
                              startColor,
                              endColor) as Int
                          holder.tag.setTextColor(color)
                        }
                        start()
                      }
                }
              }
            })
        holder.tag.setTextColor(Color.BLACK)
        title("${item.name} — ${item.description}")
        score(null)
        timestamp(null)
        author(item.license)
        source(null)
        tag(item.author)
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
            .autoDisposeWith(this)
            .subscribe()
      }
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatchUpItemViewHolder {
      return CatchUpItemViewHolder(LayoutInflater.from(parent.context)
          .inflate(layout.list_item_general, parent, false))
    }
  }
}

class OssItem(
    val avatarUrl: String,
    val author: String,
    val name: String,
    val license: String,
    val clickUrl: String,
    val description: String,
    var textColorAnimator: Animator? = null
)

@PerController
@Subcomponent
interface Component : AndroidInjector<AboutController> {

  @Subcomponent.Builder
  abstract class Builder : AndroidInjector.Builder<AboutController>()
}

@Module(subcomponents = arrayOf(Component::class))
abstract class AboutControllerBindingModule {

  @Binds
  @IntoMap
  @ControllerKey(AboutController::class)
  internal abstract fun bindAboutControllerInjectorFactory(
      builder: Component.Builder): AndroidInjector.Factory<out Controller>
}
