package io.sweers.catchup.ui.about

import android.graphics.drawable.AdaptiveIconDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import dev.zacsweers.catchup.compose.CatchUpTheme
import dev.zacsweers.catchup.compose.arcLerp
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.BackPressNavButton
import io.sweers.catchup.util.UiUtil
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val FADE_PERCENT = 0.75F
private const val TITLE_TRANSLATION_PERCENT = 0.50F

// TODO
//  - Figure out why it's not settling at targetY.
//    - Possibly missing accounting for the parallax effect of the resizing column
//  - Can we use lerp() in more places?
//  - Can we use lerp() with a custom FastOutSlowIn interpolator?
//  - Clean up leftover local state vars
//  - Can all this state be hoisted?
//  - Title initially jumps multiple pixels when collapsing
@OptIn(
  ExperimentalTextApi::class,
  ExperimentalMaterial3Api::class,
)
@Composable
fun CollapsingAboutHeader(
  versionName: String,
  scrollBehavior: TopAppBarScrollBehavior,
  modifier: Modifier = Modifier,
  maxHeight: Dp = 275.0.dp,
  pinnedHeight: Dp = 56.0.dp,
) {
  var debugUiCount by remember { mutableIntStateOf(0) }
  val debugUi = debugUiCount >= 5
  val pinnedHeightPx: Float
  val maxHeightPx: Float
  val top: Int
  val topDp: Dp
  LocalDensity.current.run {
    top = WindowInsets.systemBars.getTop(this)
    topDp = top.toDp()
    pinnedHeightPx = pinnedHeight.toPx() + top
    maxHeightPx = maxHeight.toPx() + top
  }

  // Sets the app bar's height offset limit to hide just the bottom title area and keep top title
  // visible when collapsed.
  SideEffect {
    if (scrollBehavior.state.heightOffsetLimit != pinnedHeightPx - maxHeightPx) {
      scrollBehavior.state.heightOffsetLimit = pinnedHeightPx - maxHeightPx
    }
  }

  // Set up support for resizing the top app bar when vertically dragging the bar itself.
  val appBarDragModifier =
    if (!scrollBehavior.isPinned) {
      Modifier.draggable(
        orientation = Orientation.Vertical,
        state =
          rememberDraggableState { delta ->
            scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffset + delta
          },
        onDragStopped = { velocity ->
          settleAppBar(
            scrollBehavior.state,
            velocity,
            scrollBehavior.flingAnimationSpec,
            scrollBehavior.snapAnimationSpec
          )
        }
      )
    } else {
      Modifier
    }

  val collapsedFraction = scrollBehavior.state.collapsedFraction
  // Start translating about halfway through (to give a staggered effect next to the
  // alpha so they have time to fade out sufficiently). From here we just set translation
  // offsets to adjust the position naturally to give the appearance of settling in to
  // the right place.
  val adjustedPercentage =
    (collapsedFraction - TITLE_TRANSLATION_PERCENT) * (1.0F / (1.0F - TITLE_TRANSLATION_PERCENT))
  var bannerAlpha by remember { mutableFloatStateOf(1f) }
  var aboutTextAlpha by remember { mutableFloatStateOf(1f) }
  var titleLaidOut by remember { mutableStateOf(false) }
  var titleY by remember { mutableFloatStateOf(0f) }
  var titleHeight by remember { mutableIntStateOf(0) }
  var actualY by remember { mutableFloatStateOf(0f) }
  var offset by remember { mutableStateOf(IntOffset.Zero) }
  val appBarHeightPx = maxHeightPx + scrollBehavior.state.heightOffset
  val appBarHeight = LocalDensity.current.run { appBarHeightPx.toDp() }
  val appBarOverlap = (appBarHeightPx - (titleY + titleHeight)).coerceIn(-titleHeight.toFloat(), 0f)

  var targetTitleOffset by remember { mutableStateOf(IntOffset.Zero) }
  if (debugUi) {
    SideEffect {
      println("ZAC: Surface height: ${maxHeightPx + scrollBehavior.state.heightOffset}")
    }
  }

  val boxDebugBackground = if (debugUi) Modifier.background(Color.Cyan) else Modifier
  Surface(
    modifier =
      modifier
        .then(appBarDragModifier)
        .heightIn(pinnedHeight, appBarHeight)
        .then(boxDebugBackground)
  ) {
    Box(Modifier.padding(top = topDp)) {
      Column(Modifier.fillMaxWidth()) {
        if (debugUi) {
          HorizontalDivider()
        }
        if (titleLaidOut && collapsedFraction > TITLE_TRANSLATION_PERCENT) {
          val yOffset =
            arcLerp(start = IntOffset.Zero, stop = targetTitleOffset, fraction = adjustedPercentage)
              .y
          // TODO why tf does this jump the start?
          //  val xOffset = arcLerp(IntOffset.Zero, targetTitleOffset, adjustedPercentage).x
          val xOffset = lerp(IntOffset.Zero, targetTitleOffset, adjustedPercentage).x
          offset = IntOffset(xOffset, yOffset)
          if (debugUi) {
            SideEffect {
              println(
                "ZAC: offset: $offset, titleEnd: $targetTitleOffset, percentage: $adjustedPercentage / $collapsedFraction"
              )
            }
          }
        } else {
          offset = IntOffset.Zero
        }

        // Force versions outside boundaries to be safe
        if (collapsedFraction > FADE_PERCENT) {
          bannerAlpha = 0F
          aboutTextAlpha = 0F
        }
        if (collapsedFraction < FADE_PERCENT) {
          // We want to accelerate fading to be the first [FADE_PERCENT]% of the translation,
          // so adjust accordingly below and use the new calculated collapsedFraction for our
          // interpolation
          val localAdjustedPercentage = 1 - (collapsedFraction * (1.0F / FADE_PERCENT))
          val interpolation =
            UiUtil.fastOutSlowInInterpolator.getInterpolation(localAdjustedPercentage)
          bannerAlpha = interpolation
          aboutTextAlpha = interpolation
        }

        Spacer(Modifier.requiredHeight(48.dp))

        val isInPreview = LocalInspectionMode.current
        val context = LocalContext.current
        val icon =
          if (isInPreview) {
            rememberVectorPainter(Icons.Filled.Build)
          } else {
            // TODO kinda gross but shrug
            remember(context) {
              val imageBitmap =
                (AppCompatResources.getDrawable(context, R.mipmap.ic_launcher)
                    as AdaptiveIconDrawable)
                  .toBitmap()
                  .asImageBitmap()
              BitmapPainter(imageBitmap)
            }
          }
        Image(
          painter = icon,
          contentDescription = "CatchUp icon",
          modifier =
            Modifier.size(48.dp)
              .align(CenterHorizontally)
              .graphicsLayer { alpha = bannerAlpha }
              .clickable { debugUiCount++ }
        )
        Spacer(Modifier.height(8.dp))
        var fixedTitleHeight by remember { mutableStateOf<Dp?>(null) }
        val heightModifier = fixedTitleHeight?.let { Modifier.requiredHeight(it) } ?: Modifier
        val density = LocalDensity.current
        val titleDebugBackgroundBottom = if (debugUi) Modifier.background(Color.Red) else Modifier
        val titleDebugBackgroundTop = if (debugUi) Modifier.background(Color.Green) else Modifier
        Text(
          modifier =
            Modifier.align(CenterHorizontally)
              .then(titleDebugBackgroundBottom)
              .offset { offset }
              .onGloballyPositioned { coordinates ->
                titleHeight = coordinates.size.height
                actualY = coordinates.positionInParent().y
                if (debugUi) {
                  println(
                    "ZAC: y: ${coordinates.positionInParent().y} - ${coordinates.positionInParent().y} - ${coordinates.positionInWindow().y}"
                  )
                }
                if (!titleLaidOut) {
                  fixedTitleHeight = density.run { coordinates.size.height.toDp() }
                  if (debugUi) {
                    println("ZAC: fixedTitleHeight: $fixedTitleHeight, pinnedHeight: $pinnedHeight")
                  }

                  // positionInParent calls do not account for the system bar
                  titleY = coordinates.positionInParent().y

                  // TODO account for parallax effect of shrinking column
                  // TODO account for system bar
                  val settledTitleOffset =
                    IntOffset(
                      x = coordinates.positionInParent().x.roundToInt(),
                      y = coordinates.positionInParent().y.roundToInt()
                    )

                  // top + vcenter of pinned height
                  val targetY = (((pinnedHeightPx - top) - titleHeight) / 2f)
                  targetTitleOffset =
                    IntOffset(
                      x = density.run { 56.dp.toPx() }.roundToInt() - settledTitleOffset.x,
                      y = targetY.roundToInt() - settledTitleOffset.y
                    )
                  titleLaidOut = true
                }
              }
              .then(titleDebugBackgroundTop)
              .then(heightModifier),
          // Hardcoded in previews because lol compose tooling
          text = if (isInPreview) "CatchUp" else stringResource(R.string.app_name),
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        val text =
          if (isInPreview) {
            buildAnnotatedString {
              append("An app for catching up on things.\n\nv1.0\nBy Zac Sweers  —  Source code")
            }
          } else {
            buildAnnotatedString {
              append(stringResource(R.string.about_description))
              repeat(3) { appendLine() }
              append(stringResource(R.string.about_version, versionName))
              appendLine()
              append(stringResource(R.string.about_by))
              append(" ")
              pushUrlAnnotation(UrlAnnotation("https://twitter.com/ZacSweers"))
              append("Zac Sweers")
              pop()
              append(" – ")
              pushUrlAnnotation(UrlAnnotation("https://github.com/ZacSweers/CatchUp"))
              append(stringResource(R.string.about_source_code))
              pop()
            }
          }
        // TODO material3 has zero ways to actually add clickable links in text out of the box
        Text(
          modifier = Modifier.align(CenterHorizontally).graphicsLayer(alpha = aboutTextAlpha),
          text = text,
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.requiredHeight(48.dp))
      }
      BackPressNavButton(Modifier.align(Alignment.TopStart).padding(4.dp))
      if (debugUi) {
        Text(
          text =
            """
            offset: $offset, originalY: $titleY, titleHeight: $titleHeight
            actualY: ${actualY.roundToInt()}, distance: ${(targetTitleOffset.y - actualY).roundToInt().absoluteValue}
            height: $appBarHeightPx, interp: ${(adjustedPercentage * 100).roundToInt().takeUnless { it > 100 } ?: "--"}%
            appBarOverlap: $appBarOverlap heightOffset: $appBarHeightPx
            titleTarget: $targetTitleOffset, top: $top, pinned: $pinnedHeightPx
            """
              .trimIndent(),
          modifier = Modifier.align(TopEnd),
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.End,
          fontSize = 10.sp,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleAppBar(
  state: TopAppBarState,
  velocity: Float,
  flingAnimationSpec: DecayAnimationSpec<Float>?,
  snapAnimationSpec: AnimationSpec<Float>?
): Velocity {
  // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
  // and just return Zero Velocity.
  // Note that we don't check for 0f due to float precision with the collapsedFraction
  // calculation.
  if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
    return Velocity.Zero
  }
  var remainingVelocity = velocity
  // In case there is an initial velocity that was left after a previous user fling, animate to
  // continue the motion to expand or collapse the app bar.
  if (flingAnimationSpec != null && abs(velocity) > 1f) {
    var lastValue = 0f
    AnimationState(
        initialValue = 0f,
        initialVelocity = velocity,
      )
      .animateDecay(flingAnimationSpec) {
        val delta = value - lastValue
        val initialHeightOffset = state.heightOffset
        state.heightOffset = initialHeightOffset + delta
        val consumed = abs(initialHeightOffset - state.heightOffset)
        lastValue = value
        remainingVelocity = this.velocity
        // avoid rounding errors and stop if anything is unconsumed
        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
      }
  }
  // Snap if animation specs were provided.
  if (snapAnimationSpec != null) {
    if (state.heightOffset < 0 && state.heightOffset > state.heightOffsetLimit) {
      AnimationState(initialValue = state.heightOffset).animateTo(
        if (state.collapsedFraction < 0.5f) {
          0f
        } else {
          state.heightOffsetLimit
        },
        animationSpec = snapAnimationSpec
      ) {
        state.heightOffset = value
      }
    }
  }

  return Velocity(0f, remainingVelocity)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewCollapsingAboutHeader() {
  CatchUpTheme { CollapsingAboutHeader("v1.0", TopAppBarDefaults.enterAlwaysScrollBehavior()) }
}
