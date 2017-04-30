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
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.bluelinelabs.conductor.Controller;
import com.squareup.moshi.Moshi;
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
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.ISO8601InstantAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.producthunt.ProductHuntService;
import io.sweers.catchup.data.producthunt.model.Post;
import io.sweers.catchup.data.producthunt.model.PostsResponse;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Qualifier;
import okhttp3.OkHttpClient;
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public final class ProductHuntController extends BaseNewsController<Post> {

  @Inject ProductHuntService service;
  @Inject LinkManager linkManager;

  public ProductHuntController() {
    super();
  }

  public ProductHuntController(Bundle args) {
    super(args);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_ProductHunt);
  }

  @Override protected void bindItemView(@NonNull Post item, @NonNull NewsItemViewHolder holder) {
    holder.title(item.name());
    holder.score(Pair.create("▲", item.votes_count()));
    holder.timestamp(item.created_at());
    holder.author(item.user()
        .name());
    holder.tag(item.getFirstTopic());
    holder.source(null);
    holder.comments(item.comments_count());

    holder.itemClicks()
        .compose(transformUrlToMeta(item.redirect_url()))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
    holder.itemCommentClicks()
        .compose(transformUrlToMeta(item.discussion_url()))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
  }

  @NonNull @Override protected Single<List<Post>> getDataSingle(DataRequest request) {
    return service.getPosts(request.page())
        .map(PostsResponse::posts);
  }

  @Subcomponent
  public interface Component extends AndroidInjector<ProductHuntController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<ProductHuntController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    @Qualifier
    private @interface InternalApi {}

    @Binds @IntoMap @ControllerKey(ProductHuntController.class)
    abstract AndroidInjector.Factory<? extends Controller> bindProductHuntControllerInjectorFactory(
        Component.Builder builder);

    @Provides @InternalApi static OkHttpClient provideProductHuntOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addInterceptor(AuthInterceptor.create("Bearer",
              BuildConfig.PROCUCT_HUNT_DEVELOPER_TOKEN))
          .build();
    }

    @Provides @InternalApi static Moshi provideProductHuntMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Instant.class, new ISO8601InstantAdapter())
          .build();
    }

    @Provides static ProductHuntService provideProductHuntService(
        @InternalApi final Lazy<OkHttpClient> client,
        @InternalApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      return new Retrofit.Builder().baseUrl(ProductHuntService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build()
          .create(ProductHuntService.class);
    }
  }
}
