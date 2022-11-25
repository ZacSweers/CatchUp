package dev.zacsweers.catchup.service

import android.os.Parcel
import android.os.Parcelable
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
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import io.sweers.catchup.service.api.CatchUpItem

@Composable
fun VisualServiceUi(
  lazyItems: LazyPagingItems<CatchUpItem>,
  themeColor: Color,
  onRefreshChange: (Boolean) -> Unit,
  eventSink: (ServiceScreen.Event) -> Unit,
) {
  LazyVerticalGrid(columns = GridCells.Fixed(2)) {
    items(lazyItems) { item ->
      SelectableItem(lazyItems, item, eventSink) { VisualItem(it, eventSink) }
    }
    handleLoadStates(lazyItems, themeColor, onRefreshChange)
  }
}

@Composable
fun VisualItem(item: CatchUpItem, eventSink: (ServiceScreen.Event) -> Unit) {
  // TODO bring this up to parity with ImageAdapter.
  //  placeholders
  //  palette
  //  badges
  //  gif handling
  //  etc
  Box(Modifier.aspectRatio(4f / 3f)) {
    AsyncImage(
      modifier = Modifier.fillMaxSize(),
      model = item.imageInfo!!.url,
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
private fun <T : Any> LazyGridScope.items(
  items: LazyPagingItems<T>,
  key: ((item: T) -> Any)? = null,
  itemContent: @Composable LazyGridItemScope.(value: T?) -> Unit
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
            key(item)
          }
        }
  ) { index ->
    itemContent(items[index])
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
