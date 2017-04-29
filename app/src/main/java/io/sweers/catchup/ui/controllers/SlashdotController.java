/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;
import com.bluelinelabs.conductor.Controller;
import com.uber.autodispose.CompletableScoper;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.reactivex.Single;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.slashdot.Entry;
import io.sweers.catchup.data.slashdot.SlashdotService;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.ui.base.BaseNewsController;
import io.sweers.catchup.util.Instants;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Qualifier;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public final class SlashdotController extends BaseNewsController<Entry> {

  @Inject SlashdotService service;
  @Inject LinkManager linkManager;

  public SlashdotController() {
    super();
  }

  public SlashdotController(Bundle args) {
    super(args);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Slashdot);
  }

  @Override protected void bindItemView(@NonNull Entry entry, @NonNull NewsItemViewHolder holder) {
    holder.title(entry.title);

    holder.score(null);
    holder.timestamp(Instants.parsePossiblyOffsetInstant(entry.updated));
    holder.author(entry.author.name);

    holder.source(entry.department);

    holder.comments(entry.comments);
    holder.tag(entry.section);

    holder.itemClicks()
        .compose(transformUrlToMeta(entry.id))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
    holder.itemCommentClicks()
        .compose(transformUrlToMeta(entry.id + "#comments"))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
  }

  @NonNull @Override protected Single<List<Entry>> getDataSingle(int page) {
    setMoreDataAvailable(false);
    return service.main()
        .map(channel -> channel.itemList);
  }

  @Subcomponent
  public interface Component extends AndroidInjector<SlashdotController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<SlashdotController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    @Qualifier
    private @interface InternalApi {}

    @Binds @IntoMap @ControllerKey(SlashdotController.class)
    abstract AndroidInjector.Factory<? extends Controller> bindSlashdotControllerInjectorFactory(
        Component.Builder builder);

    @Provides @InternalApi
    static OkHttpClient provideSlashdotOkHttpClient(OkHttpClient okHttpClient) {
      return okHttpClient.newBuilder()
          .addNetworkInterceptor(chain -> {
            Response originalResponse = chain.proceed(chain.request());
            // read from cache for 30 minutes, per slashdot's preferred limit
            int maxAge = 60 * 30;
            return originalResponse.newBuilder()
                .header("Cache-Control", "public, max-age=" + maxAge)
                .build();
          })
          .build();
    }

    @Provides
    static SlashdotService provideSlashdotService(@InternalApi final Lazy<OkHttpClient> client,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      Retrofit retrofit = new Retrofit.Builder().baseUrl(SlashdotService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
          .validateEagerly(BuildConfig.DEBUG)
          .build();
      return retrofit.create(SlashdotService.class);
    }
  }
}
