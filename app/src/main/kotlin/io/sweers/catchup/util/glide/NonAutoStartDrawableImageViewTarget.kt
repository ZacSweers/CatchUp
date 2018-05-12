/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.util.glide

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.request.target.DrawableImageViewTarget

/**
 * Like [DrawableImageViewTarget] but will not automatically start playing its resource.
 */
open class NonAutoStartDrawableImageViewTarget : NonAutoStartImageViewTarget<Drawable> {

  constructor(view: ImageView) : super(view)

  constructor(view: ImageView, waitForLayout: Boolean) : super(view, waitForLayout)

  override fun setResource(resource: Drawable?) {
    view.setImageDrawable(resource)
  }
}
