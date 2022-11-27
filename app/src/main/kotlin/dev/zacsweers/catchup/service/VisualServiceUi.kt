package dev.zacsweers.catchup.service

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.drawable.MovieDrawable
import coil.request.ImageRequest
import io.sweers.catchup.base.ui.ColorUtils
import io.sweers.catchup.base.ui.generateAsync
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.service.api.VisualService
import io.sweers.catchup.util.UiUtil
import kotlinx.coroutines.launch

@Composable
fun VisualServiceUi(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  onRefreshChange: (Boolean) -> Unit,
  spanConfig: VisualService.SpanConfig,
  eventSink: (ServiceScreen.Event) -> Unit,
) {
  val placeholders =
    if (isSystemInDarkTheme()) {
      PLACEHOLDERS_DARK
    } else {
      PLACEHOLDERS_LIGHT
    }
  val spanCount = spanConfig.spanCount
  // TODO with multi-size spans we get blank spaces in the grid
  LazyVerticalGrid(columns = GridCells.Fixed(spanCount)) {
    itemsIndexed(
      lazyItems,
      key = { _, item -> item.stableId() },
      span = { index -> GridItemSpan(spanConfig.spanSizeResolver?.invoke(index) ?: 1) },
    ) { index, item ->
      val clickableItemState = remember { ClickableItemState() }
      ClickableItem(lazyItems, item, eventSink, clickableItemState) { clickableItem ->
        VisualItem(
          item = clickableItem,
          index = index,
          placeholder = placeholders[index % placeholders.size],
          onEnableChanged = { clickableItemState.enabled = it },
          onContentColorChanged = { clickableItemState.contentColor = it },
        )
      }
    }
    handleLoadStates(lazyItems, themeColor, spanCount, onRefreshChange)
  }
}

@Composable
fun VisualItem(
  item: CatchUpItem,
  index: Int,
  placeholder: ColorDrawable,
  onEnableChanged: (Boolean) -> Unit = {},
  onContentColorChanged: (Color) -> Unit = {},
) {
  // TODO bring this up to parity with ImageAdapter.
  //  gif handling
  //  saturation transformation
  //  etc
  val displayMetrics = LocalContext.current.resources.displayMetrics.scaledDensity
  Box(Modifier.aspectRatio(4f / 3f)) {
    val imageInfo = item.imageInfo!!
    var hasFadedIn by remember(index) { mutableStateOf(false) }
    var badgeColor by remember(index) { mutableStateOf(Color.Unspecified) }
    val scope = rememberCoroutineScope()
    AsyncImage(
      model =
        ImageRequest.Builder(LocalContext.current)
          .data(imageInfo.url)
          .placeholder(placeholder)
          .memoryCacheKey(imageInfo.cacheKey)
          .crossfade(true)
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
                val cornerSize = (56 * displayMetrics).toInt()
                val corner =
                  Bitmap.createBitmap(
                    it,
                    it.width - cornerSize,
                    it.height - cornerSize,
                    cornerSize,
                    cornerSize
                  )
                val isDark = ColorUtils.isDark(corner)
                corner.recycle()
                badgeColor = if (isDark) Color(0xb3ffffff) else Color(0x40000000)
              }
            },
            onError = { _, _ -> onEnableChanged(false) },
          )
          .build(),
      modifier = Modifier.fillMaxSize(),
      contentDescription = "Image for ${item.title}",
      contentScale = ContentScale.Crop,
      alignment = Alignment.Center,
    )
    if (imageInfo.animatable && badgeColor != Color.Unspecified) {
      Badge(badgeColor = badgeColor, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
  }
}

@Composable
private fun Badge(
  badgeColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = badgeColor.copy(alpha = 0.5f),
    shape = RoundedCornerShape(2.dp)
  ) {
    Text(
      modifier = Modifier.padding(4.dp),
      text = "GIF",
      fontSize = 12.sp,
      fontStyle = FontStyle.Normal,
      fontWeight = FontWeight.Black,
      color = Color.White
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

private fun applyPalette(palette: Palette, onContentColorChanged: (Color) -> Unit) {
  val color = UiUtil.createRippleColor(palette, 0.25f, 0.5f, 0x40808080)
  onContentColorChanged(Color(color))
}

fun LazyGridScope.handleLoadStates(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  spanCount: Int,
  onRefreshChange: (Boolean) -> Unit
) {
  lazyItems.apply {
    when {
      loadState.refresh is LoadState.Loading -> {
        onRefreshChange(true)
        item(span = { GridItemSpan(spanCount) }) { LoadingView(themeColor, Modifier.fillMaxSize()) }
      }
      loadState.refresh is LoadState.NotLoading -> {
        onRefreshChange(false)
      }
      loadState.append is LoadState.Loading -> {
        item(span = { GridItemSpan(spanCount) }) { LoadingItem() }
      }
      loadState.refresh is LoadState.Error -> {
        val e = loadState.refresh as LoadState.Error
        item(span = { GridItemSpan(spanCount) }) {
          ErrorItem(
            "Error loading service: ${e.error.localizedMessage}",
            Modifier.fillMaxSize(),
            ::retry
          )
        }
      }
      loadState.append is LoadState.Error -> {
        val e = loadState.append as LoadState.Error
        item(span = { GridItemSpan(2) }) {
          ErrorItem("Error loading service: ${e.error.localizedMessage}", onRetryClick = ::retry)
        }
      }
    }
  }
}

// TODO copied + modified from Paging
fun <T : Any> LazyGridScope.itemsIndexed(
  items: LazyPagingItems<T>,
  key: ((index: Int, item: T) -> Any)? = null,
  span: (LazyGridItemSpanScope.(index: Int) -> GridItemSpan)? = null,
  itemContent: @Composable LazyGridItemScope.(index: Int, value: T?) -> Unit
) {
  items(
    count = items.itemCount,
    span = span,
    key =
      if (key == null) null
      else
        { index ->
          val item = items.peek(index)
          if (item == null) {
            PagingPlaceholderKey(index)
          } else {
            key(index, item)
          }
        }
  ) { index ->
    itemContent(index, items[index])
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

private val PLACEHOLDERS_DARK =
  arrayOf(
    ColorDrawable(0xff191919.toInt()),
    ColorDrawable(0xff212121.toInt()),
    ColorDrawable(0xff232323.toInt()),
  )

private val PLACEHOLDERS_LIGHT =
  arrayOf(
    ColorDrawable(0xfff5f5f5.toInt()),
    ColorDrawable(0xffeeeeee.toInt()),
    ColorDrawable(0xffe0e0e0.toInt()),
  )
