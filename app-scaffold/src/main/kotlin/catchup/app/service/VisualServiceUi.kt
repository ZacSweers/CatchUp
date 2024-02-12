@file:Suppress("DEPRECATION_ERROR")

package catchup.app.service

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.palette.graphics.Palette
import catchup.app.service.ServiceScreen.Event
import catchup.app.service.ServiceScreen.Event.ItemClicked
import catchup.app.util.UiUtil
import catchup.base.ui.BlurHashDecoder
import catchup.base.ui.ColorUtils
import catchup.base.ui.generateAsync
import catchup.compose.ScrollToTopHandler
import catchup.compose.columnCount
import catchup.compose.rememberStableCoroutineScope
import catchup.service.api.CatchUpItem
import coil.compose.AsyncImage
import coil.drawable.MovieDrawable
import coil.request.ImageRequest
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Composable
fun VisualServiceUi(
  items: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  eventSink: (Event) -> Unit,
  modifier: Modifier = Modifier,
) {
  val delegateRippleTheme = LocalRippleTheme.current
  CompositionLocalProvider(LocalRippleTheme provides ImageItemRippleTheme(delegateRippleTheme)) {
    val state = rememberLazyStaggeredGridState()
    ScrollToTopHandler(state)
    LazyVerticalStaggeredGrid(
      columns = StaggeredGridCells.Fixed(columnCount(2)),
      state = state,
      modifier = modifier,
    ) {
      items(
        count = items.itemCount,
        // Here we use the new itemKey extension on LazyPagingItems to
        // handle placeholders automatically, ensuring you only need to provide
        // keys for real items
        key = items.itemKey { it.id },
      ) { index ->
        val item = items[index]
        if (item != null) {
          val clickableItemState =
            rememberClickableItemState(
              contentColor =
                item.imageInfo?.color?.let { Color(android.graphics.Color.parseColor(it)) }
                  ?: Color.Unspecified
            )
          ClickableItem(onClick = { eventSink(ItemClicked(item)) }, state = clickableItemState) {
            VisualItem(
              item = item,
              index = index,
              // TODO what's the right way to make this work...?
              //              modifier = Modifier.heightIn(max = 150.dp),
              onEnableChanged = { clickableItemState.enabled = it },
              onContentColorChanged = { clickableItemState.contentColor = it },
            )
          }
        }
      }
      handleLoadStates(items, themeColor)
    }
  }
}

@Composable
fun VisualItem(
  item: CatchUpItem,
  index: Int,
  modifier: Modifier = Modifier,
  onEnableChanged: (Boolean) -> Unit = {},
  onContentColorChanged: (Color) -> Unit = {},
) {
  // TODO bring this up to parity with ImageAdapter.
  //  gif handling
  //  saturation transformation
  //  etc
  Box(modifier) {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val imageInfo = item.imageInfo!!

    // Compute in-grid image size
    val imageWidth = displayMetrics.widthPixels / columnCount(2)
    val imageWidthDp = LocalDensity.current.run { imageWidth.toDp() }
    // To avoid super tall images, cap the aspect ration to 1:2 at most
    val maxHeight = (imageWidth * 1.5).roundToInt()
    val imageHeight = (imageWidth / imageInfo.aspectRatio).toInt().coerceAtMost(maxHeight)
    val imageHeightDp = LocalDensity.current.run { imageHeight.toDp() }

    val scope = rememberStableCoroutineScope()

    val placeholders =
      if (isSystemInDarkTheme()) {
        PLACEHOLDERS_DARK
      } else {
        PLACEHOLDERS_LIGHT
      }
    val placeholderColor = placeholders[index % placeholders.size]

    val blurHash = imageInfo.blurHash
    if (blurHash != null) {
      val blurHashBitmap by
        produceState<ImageBitmap?>(null) {
          value =
            withContext(IO) {
              BlurHashDecoder.decode(blurHash, imageWidth, imageHeight)?.asImageBitmap()
            }
        }

      Crossfade(blurHashBitmap != null, label = "Crossfade blurhash") { loaded ->
        if (loaded) {
          Image(
            bitmap = blurHashBitmap!!,
            contentDescription = "Loading image for ${item.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(imageWidthDp, imageHeightDp),
          )
        } else {
          Box(modifier = Modifier.background(placeholderColor).size(imageWidthDp, imageHeightDp)) {}
        }
      }
    }
    var hasFadedIn by remember(index) { mutableStateOf(false) }
    var badgeColor by remember(index) { mutableStateOf(Color.Unspecified) }
    val density = LocalDensity.current
    AsyncImage(
      model =
        ImageRequest.Builder(LocalContext.current)
          .data(imageInfo.url)
          .memoryCacheKey(imageInfo.cacheKey)
          .let {
            if (blurHash == null) {
              it.placeholder(ColorDrawable(placeholderColor.toArgb()))
            } else {
              it
            }
          }
          // .run {
          // TODO transitions don't work with AsyncImage yet
          //  https://coil-kt.github.io/coil/compose/#transitions
          //   if (!hasFadedIn) {
          //     transitionFactory(SaturatingTransformation.Factory)
          //   } else {
          //     crossfade(0)
          //   }
          // }
          .listener(
            onSuccess = { _, imageResult ->
              hasFadedIn = true
              val result = imageResult.drawable
              var bitmap: Bitmap? = null
              if (result is BitmapDrawable) {
                bitmap = result.bitmap
                scope.launch {
                  Palette.from(result.bitmap).clearFilters().generateAsync()?.let {
                    applyPalette(it, onContentColorChanged)
                  }
                }
              } else if (result is MovieDrawable) {
                // TODO need to extract the first frame somehow
                // val image = result.firstFrame
                if (bitmap == null || bitmap.isRecycled) {
                  return@listener
                }
                scope.launch {
                  Palette.from(bitmap).clearFilters().generateAsync()?.let {
                    applyPalette(it, onContentColorChanged)
                  }
                }
              }

              bitmap?.let {
                // look at the corner to determine the gif badge color
                val cornerSize = density.run { 56.sp.toPx() }.toInt()
                val corner =
                  Bitmap.createBitmap(
                    it,
                    it.width - cornerSize,
                    it.height - cornerSize,
                    cornerSize,
                    cornerSize,
                  )
                val isDark = ColorUtils.isDark(corner)
                corner.recycle()
                badgeColor = if (isDark) Color(0xb3ffffff) else Color(0x40000000)
              }
            },
            onError = { _, _ -> onEnableChanged(false) },
          )
          .build(),
      modifier = Modifier.size(imageWidthDp, imageHeightDp),
      contentDescription = "Image for ${item.title}. Description: ${item.description}",
      contentScale = ContentScale.Crop,
      alignment = Alignment.Center,
    )
    if (imageInfo.animatable && badgeColor != Color.Unspecified) {
      Badge(badgeColor = badgeColor, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
  }
}

@Composable
private fun Badge(badgeColor: Color, modifier: Modifier = Modifier, text: String = "GIF") {
  Surface(
    modifier = modifier,
    color = badgeColor.copy(alpha = 0.5f),
    shape = RoundedCornerShape(2.dp),
  ) {
    Text(
      modifier = Modifier.padding(4.dp),
      text = text,
      fontSize = 12.sp,
      fontStyle = FontStyle.Normal,
      fontWeight = FontWeight.Black,
      color = Color.White,
    )
  }
}

@Preview
@Composable
private fun PreviewBadge() {
  Column(verticalArrangement = spacedBy(16.dp)) {
    Badge(badgeColor = Color.White)
    Badge(badgeColor = Color.Black)
  }
}

@Stable
private class ImageItemRippleTheme(private val delegate: RippleTheme) : RippleTheme {
  companion object {
    val opaqueRippleAlpha =
      RippleAlpha(
        draggedAlpha = 0.16f,
        focusedAlpha = 0.12f,
        hoveredAlpha = 0.08f,
        // Changed from M3's nearly-imperceptible default of 0.12f
        pressedAlpha = 0.40f,
      )
  }

  @Deprecated(
    "RippleTheme and LocalRippleTheme have been deprecated - they are not compatible with the new ripple implementation using the new Indication APIs that provide notable performance improvements. For a migration guide and background information, please visit developer.android.com",
    level = DeprecationLevel.ERROR,
  )
  @Composable
  override fun defaultColor(): Color = delegate.defaultColor()

  @Deprecated(
    "RippleTheme and LocalRippleTheme have been deprecated - they are not compatible with the new ripple implementation using the new Indication APIs that provide notable performance improvements. For a migration guide and background information, please visit developer.android.com",
    level = DeprecationLevel.ERROR,
  )
  @Composable
  override fun rippleAlpha(): RippleAlpha = opaqueRippleAlpha
}

private fun applyPalette(palette: Palette, onContentColorChanged: (Color) -> Unit) {
  val color = UiUtil.createRippleColor(palette, 0.25f, 0.5f, 0x40808080)
  onContentColorChanged(Color(color))
}

fun LazyStaggeredGridScope.handleLoadStates(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
) {
  lazyItems.apply {
    when {
      loadState.refresh is LoadState.Loading -> {
        // Refresh
        item(key = "loading", span = StaggeredGridItemSpan.FullLine) {
          LoadingView(themeColor, Modifier.fillMaxSize())
        }
      }
      loadState.append is LoadState.Loading -> {
        item(key = "appendingMore", span = StaggeredGridItemSpan.FullLine) { LoadingItem() }
      }
      loadState.refresh is LoadState.Error -> {
        val e = loadState.refresh as LoadState.Error
        Timber.e(e.error)
        item(key = "errorLoading", span = StaggeredGridItemSpan.FullLine) {
          ErrorItem(
            "Error loading service: ${e.error.localizedMessage}",
            Modifier.fillMaxSize(),
            ::retry,
          )
        }
      }
      loadState.append is LoadState.Error -> {
        val e = loadState.append as LoadState.Error
        Timber.e(e.error)
        item(key = "errorLoadingMore", span = StaggeredGridItemSpan.FullLine) {
          ErrorItem("Error loading service: ${e.error.localizedMessage}", onRetryClick = ::retry)
        }
      }
    }
  }
}

// TODO copied from Paging
private data class PagingPlaceholderKey(private val index: Int) : Parcelable {
  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(index)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object {
    @Suppress("unused")
    @JvmField
    val CREATOR: Parcelable.Creator<PagingPlaceholderKey> =
      object : Parcelable.Creator<PagingPlaceholderKey> {
        override fun createFromParcel(parcel: Parcel) = PagingPlaceholderKey(parcel.readInt())

        override fun newArray(size: Int) = arrayOfNulls<PagingPlaceholderKey?>(size)
      }
  }
}

private val PLACEHOLDERS_DARK = arrayOf(Color(0xff191919), Color(0xff212121), Color(0xff232323))

private val PLACEHOLDERS_LIGHT = arrayOf(Color(0xfff5f5f5), Color(0xffeeeeee), Color(0xffe0e0e0))
