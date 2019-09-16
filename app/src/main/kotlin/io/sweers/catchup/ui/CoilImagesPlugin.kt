/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.widget.TextView
import coil.ImageLoader
import coil.api.load
import coil.target.Target
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableLoader
import io.noties.markwon.image.AsyncDrawableScheduler
import io.noties.markwon.image.DrawableUtils
import io.noties.markwon.image.ImageSpanFactory
import org.commonmark.node.Image
import java.util.HashMap

class CoilImagesPlugin(context: Context, imageLoader: ImageLoader) : AbstractMarkwonPlugin() {

  private val coilAsyncDrawableLoader = CoilAsyncDrawableLoader(context, imageLoader)

  override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
    builder.setFactory(Image::class.java, ImageSpanFactory())
  }

  override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
    builder.asyncDrawableLoader(coilAsyncDrawableLoader)
  }

  override fun beforeSetText(textView: TextView, markdown: Spanned) {
    AsyncDrawableScheduler.unschedule(textView)
  }

  override fun afterSetText(textView: TextView) {
    AsyncDrawableScheduler.schedule(textView)
  }

  // TODO cancellation isn't supported right now. Would need to make this be able to pass a
  //  lifecycle to the load
  private class CoilAsyncDrawableLoader(
    private val context: Context,
    private val imageLoader: ImageLoader
  ) : AsyncDrawableLoader() {
    private val cache = HashMap<AsyncDrawable, Target>(2)

    override fun load(drawable: AsyncDrawable) {
      val target = AsyncDrawableTarget(drawable)
      cache[drawable] = target
      imageLoader.load(context, drawable.destination) {
        target(target)
      }
    }

    override fun cancel(drawable: AsyncDrawable) {
      cache.remove(drawable)
    }

    override fun placeholder(drawable: AsyncDrawable): Drawable? {
      return null
    }

    private inner class AsyncDrawableTarget internal constructor(
      private val drawable: AsyncDrawable
    ) : Target {

      override fun onError(error: Drawable?) {
        if (cache.remove(drawable) != null) {
          if (error != null && drawable.isAttached) {
            DrawableUtils.applyIntrinsicBoundsIfEmpty(error)
            drawable.result = error
          }
        }
      }

      override fun onStart(placeholder: Drawable?) {
        if (placeholder != null && drawable.isAttached) {
          DrawableUtils.applyIntrinsicBoundsIfEmpty(placeholder)
          drawable.result = placeholder
        }
      }

      override fun onSuccess(result: Drawable) {
        if (cache.remove(drawable) != null) {
          if (drawable.isAttached) {
            DrawableUtils.applyIntrinsicBoundsIfEmpty(result)
            drawable.result = result
          }
        }
      }

      private fun onLoadCleared(placeholder: Drawable?) {
        // we won't be checking if target is still present as cancellation
        // must remove target anyway
        if (drawable.isAttached) {
          drawable.clearResult()
        }
      }
    }
  }

  companion object {

    fun create(context: Context, imageLoader: ImageLoader): CoilImagesPlugin {
      return CoilImagesPlugin(context, imageLoader)
    }
  }
}
