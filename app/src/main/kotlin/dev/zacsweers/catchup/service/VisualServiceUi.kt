package dev.zacsweers.catchup.service

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.sweers.catchup.service.api.CatchUpItem

@Composable
fun VisualServiceUi(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  onRefreshChange: (Boolean) -> Unit,
  eventSink: (ServiceScreen.Event) -> Unit,
) {
  val placeholders =
    if (isSystemInDarkTheme()) {
      PLACEHOLDERS_DARK
    } else {
      PLACEHOLDERS_LIGHT
    }
  LazyVerticalGrid(columns = GridCells.Fixed(2)) {
    itemsIndexed(lazyItems) { index, item ->
      SelectableItem(lazyItems, item, eventSink) {
        VisualItem(it, placeholders[index % placeholders.size], eventSink)
      }
    }
    handleLoadStates(lazyItems, themeColor, onRefreshChange)
  }
}

@Composable
fun VisualItem(item: CatchUpItem, placeholder: Drawable, eventSink: (ServiceScreen.Event) -> Unit) {
  // TODO bring this up to parity with ImageAdapter.
  //  palette
  //  badges
  //  gif handling
  //  etc
  Box(Modifier.aspectRatio(4f / 3f)) {
    val imageInfo = item.imageInfo!!
    AsyncImage(
      model =
        ImageRequest.Builder(LocalContext.current)
          .data(imageInfo.url)
          .placeholder(placeholder)
          .memoryCacheKey(imageInfo.cacheKey)
          .crossfade(true)
          .build(),
      modifier = Modifier.fillMaxSize(),
      contentDescription = "Image for ${item.title}",
      contentScale = ContentScale.Crop,
      alignment = Alignment.Center
    )
  }
}

fun LazyGridScope.handleLoadStates(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  onRefreshChange: (Boolean) -> Unit
) {
  lazyItems.apply {
    when {
      loadState.refresh is LoadState.Loading -> {
        onRefreshChange(true)
        item { LoadingView(themeColor, Modifier.fillMaxSize()) }
      }
      loadState.refresh is LoadState.NotLoading -> {
        onRefreshChange(false)
      }
      loadState.append is LoadState.Loading -> {
        item { LoadingItem() }
      }
      loadState.refresh is LoadState.Error -> {
        val e = loadState.refresh as LoadState.Error
        item {
          ErrorItem(
            "Error loading service: ${e.error.localizedMessage}",
            Modifier.fillMaxSize(),
            ::retry
          )
        }
      }
      loadState.append is LoadState.Error -> {
        val e = loadState.append as LoadState.Error
        item {
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
  itemContent: @Composable LazyGridItemScope.(index: Int, value: T?) -> Unit
) {
  items(
    count = items.itemCount,
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
