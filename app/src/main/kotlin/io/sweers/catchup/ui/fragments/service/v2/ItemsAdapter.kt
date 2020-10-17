package io.sweers.catchup.ui.fragments.service.v2

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import io.sweers.catchup.service.api.CatchUpItem
import io.sweers.catchup.ui.base.CatchUpItemViewHolder

class ItemsAdapter : PagingDataAdapter<CatchUpItem, CatchUpItemViewHolder>(ITEM_COMPARATOR) {

  init {
    setHasStableIds(true)
  }

  override fun onBindViewHolder(holder: CatchUpItemViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  override fun onBindViewHolder(
    holder: CatchUpItemViewHolder,
    position: Int,
    payloads: MutableList<Any>
  ) {
    onBindViewHolder(holder, position)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatchUpItemViewHolder {
    return CatchUpItemViewHolder.create(parent)
  }

  companion object {
    val ITEM_COMPARATOR = object : DiffUtil.ItemCallback<CatchUpItem>() {
      override fun areContentsTheSame(oldItem: CatchUpItem, newItem: CatchUpItem): Boolean =
        oldItem == newItem

      override fun areItemsTheSame(oldItem: CatchUpItem, newItem: CatchUpItem): Boolean =
        oldItem.id == newItem.id
    }
  }
}
