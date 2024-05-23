package catchup.app.ui.about

import android.graphics.drawable.AdaptiveIconDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.graphics.drawable.toBitmap
import catchup.base.ui.BackPressNavButton
import dev.zacsweers.catchup.app.scaffold.R
import kotlin.math.roundToInt
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

// TODO
//  - Can we use lerp() in more places?
//  - Use arcLerp() for title's movement.
//  - Can all this state be hoisted?
//  - Title initially jumps multiple pixels when collapsing
@Composable
fun CollapsingAboutHeader(
  versionName: String,
  modifier: Modifier = Modifier,
  body: @Composable () -> Unit = {},
) {
  val scaffoldState = rememberCollapsingToolbarScaffoldState()
  val parallaxRatio = 0.2f

  Surface(
    modifier,
    color = Color.Transparent,
  ) { // todo: why is this needed when AboutScreen is already using Scaffold()?
    CollapsingToolbarScaffold(
      state = scaffoldState,
      modifier = Modifier.statusBarsPadding(),
      scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
      toolbar = {
        // "toolbar" lambda requires two children. The
        // taller child gets marked as the expanded state.
        Box { BackPressNavButton(Modifier.padding(4.dp)) }

        Column(Modifier.fillMaxWidth().parallax(ratio = parallaxRatio)) {
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
                .alpha(scaffoldState.toolbarState.progress),
          )

          Spacer(Modifier.height(8.dp))

          var titleCoordinates by remember { mutableStateOf(Offset.Unspecified) }
          val navButtonWidthPx = LocalDensity.current.run { 56.dp.roundToPx() }

          val textOffset =
            if (titleCoordinates.isSpecified) {
              val parallaxOffset =
                (scaffoldState.toolbarState.maxHeight - scaffoldState.toolbarState.height) *
                  parallaxRatio

              val targetY =
                -titleCoordinates.y.roundToInt() +
                  // A fixed top-padding is used so that the title is top-aligned.
                  LocalDensity.current.run { 12.dp.roundToPx() }

              IntOffset(
                x = navButtonWidthPx - titleCoordinates.x.roundToInt(),
                y = targetY + parallaxOffset.roundToInt(),
              )
            } else {
              IntOffset.Zero
            }

          Text(
            modifier =
              Modifier.align(CenterHorizontally)
                .onGloballyPositioned { titleCoordinates = it.positionInParent() }
                .offset {
                  lerp(IntOffset.Zero, textOffset, 1f - scaffoldState.toolbarState.progress)
                },
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
                withLink(LinkAnnotation.Url("https://twitter.com/ZacSweers")) {
                  append("Zac Sweers")
                }
                append(" – ")
                withLink(LinkAnnotation.Url("https://github.com/ZacSweers/CatchUp")) {
                  append(stringResource(R.string.about_source_code))
                }
              }
            }
          Text(
            modifier =
              Modifier.align(CenterHorizontally).alpha(scaffoldState.toolbarState.progress),
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
          )
          Spacer(Modifier.requiredHeight(48.dp))
        }
      },
      body = { body() },
    )
  }
}
