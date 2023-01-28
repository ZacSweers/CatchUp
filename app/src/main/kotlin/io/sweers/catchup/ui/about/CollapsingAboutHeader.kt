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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
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
import io.sweers.catchup.R
import io.sweers.catchup.base.ui.BackPressNavButton
import io.sweers.catchup.util.UiUtil
import kotlin.math.abs
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
@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CollapsingAboutHeader(
  versionName: String,
  scrollBehavior: TopAppBarScrollBehavior,
  modifier: Modifier = Modifier,
  maxHeight: Dp = 275.0.dp,
  pinnedHeight: Dp = 56.0.dp,
  debugUi: Boolean = false,
) {
  val pinnedHeightPx: Float
  val maxHeightPx: Float
  LocalDensity.current.run {
    pinnedHeightPx = pinnedHeight.toPx()
    maxHeightPx = maxHeight.toPx()
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
  val adjustedPercentage = (1 - collapsedFraction) * (1.0F / TITLE_TRANSLATION_PERCENT)
  var bannerAlpha by remember { mutableStateOf(1f) }
  var aboutTextAlpha by remember { mutableStateOf(1f) }
  var titleLaidOut by remember { mutableStateOf(false) }
  val xDelta = LocalDensity.current.run { 72.dp.toPx() }
  var yDelta by remember { mutableStateOf(0f) }
  var targetY by remember { mutableStateOf(0f) }
  var titleY by remember { mutableStateOf(0f) }
  var titleHeight by remember { mutableStateOf(0) }
  var actualY by remember { mutableStateOf(0f) }
  var offset by remember { mutableStateOf(IntOffset.Zero) }
  val appBarHeightPx = maxHeightPx + scrollBehavior.state.heightOffset
  val appBarHeight = LocalDensity.current.run { appBarHeightPx.toDp() }
  val appBarOverlap = (appBarHeightPx - (titleY + titleHeight)).coerceAtMost(0f)
  if (debugUi) {
    SideEffect {
      println("ZAC: Surface height: ${maxHeightPx + scrollBehavior.state.heightOffset}")
    }
  }

  val boxDebugBackground = if (debugUi) Modifier.background(Color.Cyan) else Modifier
  Surface(
    modifier = modifier.then(appBarDragModifier).height(appBarHeight).then(boxDebugBackground)
  ) {
    Box {
      Column(Modifier.fillMaxWidth()) {
        if (titleLaidOut && collapsedFraction > TITLE_TRANSLATION_PERCENT) {
          val interpolation = UiUtil.fastOutSlowInInterpolator.getInterpolation(adjustedPercentage)
          val newY =
            -LocalDensity.current.run {
              lerp(
                  start = titleY.toDp() + appBarOverlap.toDp(),
                  stop = targetY.toDp(),
                  fraction = adjustedPercentage
                )
                .roundToPx()
            }
          offset =
            IntOffset(
              x = -(xDelta - (interpolation * xDelta)).roundToInt(),
              y = newY,
            )
          if (debugUi) {
            SideEffect {
              println("ZAC: offset: $offset, yDelta: $yDelta, interpolation: $interpolation")
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
          val adjustedPercentage = 1 - (collapsedFraction * (1.0F / FADE_PERCENT))
          val interpolation = UiUtil.fastOutSlowInInterpolator.getInterpolation(adjustedPercentage)
          bannerAlpha = interpolation
          aboutTextAlpha = interpolation
        }

        Spacer(Modifier.height(48.dp))

        // TODO kinda gross but shrug
        val icon =
          (AppCompatResources.getDrawable(LocalContext.current, R.mipmap.ic_launcher)
              as AdaptiveIconDrawable)
            .toBitmap()
        Image(
          bitmap = icon.asImageBitmap(),
          contentDescription = "CatchUp icon",
          modifier =
            Modifier.size(48.dp).align(CenterHorizontally).graphicsLayer { alpha = bannerAlpha }
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                    "ZAC: y: ${coordinates.positionInParent().y} - ${coordinates.positionInRoot().y} - ${coordinates.positionInWindow().y}"
                  )
                }
                if (!titleLaidOut) {
                  fixedTitleHeight = density.run { coordinates.size.height.toDp() }
                  if (debugUi) {
                    println("ZAC: fixedTitleHeight: $fixedTitleHeight, pinnedHeight: $pinnedHeight")
                  }
                  targetY = density.run { ((pinnedHeight - fixedTitleHeight!!) / 2).toPx() }
                  titleY = coordinates.positionInParent().y

                  // Y values are a bit trickier - these need to figure out where they would be on
                  // the larger plane, so we calculate it upfront by predicting where it would land
                  // after collapse is done. This requires knowing the parallax multiplier and
                  // adjusting for the parent plane rather  than the relative plane of the internal
                  // Column. Once we know the predicted global Y, easy to calculate desired delta
                  // from there.
                  // TODO account for parallax effect of shrinking column
                  yDelta = titleY - targetY
                  if (debugUi) {
                    println(
                      "ZAC: Computed yDelta: $yDelta. HeightOffset: ${scrollBehavior.state.heightOffsetLimit}, titleY: $titleY, height: ${coordinates.size.height}, targetY: $targetY"
                    )
                  }
                  titleLaidOut = true
                }
              }
              .then(titleDebugBackgroundTop)
              .then(heightModifier),
          text = stringResource(R.string.app_name),
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        val isInPreview = LocalInspectionMode.current
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
        Spacer(Modifier.height(48.dp))
      }
      BackPressNavButton(Modifier.align(Alignment.TopStart).padding(16.dp))
      if (debugUi) {
        Text(
          text =
            "offset: $offset\n" +
              "actualY: ${actualY.roundToInt()}, distance: ${(actualY - targetY).roundToInt()}\n" +
              "height: ${LocalDensity.current.run { appBarHeight.toPx() }.roundToInt()}, interp: ${(adjustedPercentage * 100).roundToInt().takeUnless { it > 100 } ?: "--"}%\n" +
              "appBarOverlap: $appBarOverlap heightOffset: $appBarHeightPx, titleYActual: ${titleHeight + titleY}\n" +
              "targetY: ${targetY.roundToInt()}, yDelta: $yDelta\n",
          modifier = Modifier.align(TopEnd),
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.End,
          fontSize = 10.sp,
          color = Color.Black,
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

// private val headerHeight = 250.dp
// private val toolbarHeight = 56.dp
//
// private val paddingMedium = 16.dp
//
// private val titlePaddingStart = 16.dp
// private val titlePaddingEnd = 72.dp
//
// private const val titleFontScaleStart = 1f
// private const val titleFontScaleEnd = 0.66f
//
// @Composable
// fun CollapsingToolbarParallaxEffect() {
//  val scroll: ScrollState = rememberScrollState(0)
//
//  val headerHeightPx = with(LocalDensity.current) { headerHeight.toPx() }
//  val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.toPx() }
//
//  Box(modifier = Modifier.fillMaxSize()) {
//    Header(scroll, headerHeightPx)
//    Toolbar(scroll, headerHeightPx, toolbarHeightPx)
//    Title(scroll, headerHeightPx, toolbarHeightPx)
//  }
// }
//
// @Composable
// private fun Header(scroll: ScrollState, headerHeightPx: Float) {
//  Box(
//    modifier =
//      Modifier.fillMaxWidth().height(headerHeight).graphicsLayer {
//        translationY = -scroll.value.toFloat() / 2f // Parallax effect
//        alpha = (-1f / headerHeightPx) * scroll.value + 1
//      }
//  ) {
//    //    Image(
//    //      painter = painterResource(id = R.drawable.bg_pexel),
//    //      contentDescription = "",
//    //      contentScale = ContentScale.FillBounds
//    //    )
//
//    Box(
//      Modifier.fillMaxSize()
//        .background(
//          brush =
//            Brush.verticalGradient(
//              colors = listOf(Color.Transparent, Color(0xAA000000)),
//              startY = 3 * headerHeightPx / 4 // Gradient applied to wrap the title only
//            )
//        )
//    )
//  }
// }
//
// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// private fun Toolbar(scroll: ScrollState, headerHeightPx: Float, toolbarHeightPx: Float) {
//  val toolbarBottom = headerHeightPx - toolbarHeightPx
//  val showToolbar by remember { derivedStateOf { scroll.value >= toolbarBottom } }
//
//  AnimatedVisibility(
//    visible = showToolbar,
//    enter = fadeIn(animationSpec = tween(300)),
//    exit = fadeOut(animationSpec = tween(300))
//  ) {
//    TopAppBar(
//      modifier =
//        Modifier.background(
//          brush = Brush.horizontalGradient(listOf(Color(0xff026586), Color(0xff032C45)))
//        ),
//      navigationIcon = {
//        IconButton(onClick = {}, modifier = Modifier.padding(16.dp).size(24.dp)) {
//          Icon(imageVector = Icons.Default.Menu, contentDescription = "", tint = Color.White)
//        }
//      },
//      title = {},
//      //      backgroundColor = Color.Transparent,
//      //      elevation = 0.dp
//    )
//  }
// }
//
// @Composable
// private fun Title(scroll: ScrollState, headerHeightPx: Float, toolbarHeightPx: Float) {
//  var titleHeightPx by remember { mutableStateOf(0f) }
//  var titleWidthPx by remember { mutableStateOf(0f) }
//
//  Text(
//    text = "New York",
//    fontSize = 30.sp,
//    fontWeight = FontWeight.Bold,
//    modifier =
//      Modifier.graphicsLayer {
//          val collapseRange: Float = (headerHeightPx - toolbarHeightPx)
//          val collapseFraction: Float = (scroll.value / collapseRange).coerceIn(0f, 1f)
//
//          val scaleXY = lerp(titleFontScaleStart.dp, titleFontScaleEnd.dp, collapseFraction)
//
//          val titleExtraStartPadding = titleWidthPx.toDp() * (1 - scaleXY.value) / 2f
//
//          val titleYFirstInterpolatedPoint =
//            lerp(
//              headerHeight - titleHeightPx.toDp() - paddingMedium,
//              headerHeight / 2,
//              collapseFraction
//            )
//
//          val titleXFirstInterpolatedPoint =
//            lerp(
//              titlePaddingStart,
//              (titlePaddingEnd - titleExtraStartPadding) * 5 / 4,
//              collapseFraction
//            )
//
//          val titleYSecondInterpolatedPoint =
//            lerp(headerHeight / 2, toolbarHeight / 2 - titleHeightPx.toDp() / 2, collapseFraction)
//
//          val titleXSecondInterpolatedPoint =
//            lerp(
//              (titlePaddingEnd - titleExtraStartPadding) * 5 / 4,
//              titlePaddingEnd - titleExtraStartPadding,
//              collapseFraction
//            )
//
//          val titleY =
//            lerp(titleYFirstInterpolatedPoint, titleYSecondInterpolatedPoint, collapseFraction)
//
//          val titleX =
//            lerp(titleXFirstInterpolatedPoint, titleXSecondInterpolatedPoint, collapseFraction)
//
//          translationY = titleY.toPx()
//          translationX = titleX.toPx()
//          scaleX = scaleXY.value
//          scaleY = scaleXY.value
//        }
//        .onGloballyPositioned {
//          titleHeightPx = it.size.height.toFloat()
//          titleWidthPx = it.size.width.toFloat()
//        }
//  )
// }
//
// @Preview(showBackground = true)
// @Composable
// fun DefaultPreview() {
//  CatchUpTheme { CollapsingToolbarParallaxEffect() }
// }
