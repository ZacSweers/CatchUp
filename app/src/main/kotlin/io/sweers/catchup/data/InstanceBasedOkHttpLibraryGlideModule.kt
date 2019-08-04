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
package io.sweers.catchup.data

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.LibraryGlideModule
import dagger.Subcomponent
import io.sweers.catchup.app.CatchUpApplication
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject

@GlideModule
class InstanceBasedOkHttpLibraryGlideModule : LibraryGlideModule() {
  @Inject
  lateinit var okHttpClient: dagger.Lazy<OkHttpClient>

  init {
    // TODO this is ugly, can we eliminate the need for this?
    //  maybe when dagger makes custom hierarchy injections available
    CatchUpApplication.appComponent
        .okhttpGlideComponentBuilder()
        .create()
        .inject(this)
  }

  override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
    registry.replace(
        GlideUrl::class.java,
        InputStream::class.java,
        OkHttpUrlLoader.Factory(object : Call.Factory {
          override fun newCall(request: Request): Call = okHttpClient.get().newCall(request)
        })
    )
  }

  @Subcomponent
  interface Component {

    fun inject(instanceOkHttpLibraryGlideModule: InstanceBasedOkHttpLibraryGlideModule)

    @Subcomponent.Factory
    interface Builder {
      fun create(): Component
    }
  }
}
