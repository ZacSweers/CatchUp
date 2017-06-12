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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.serjltt.moshi.adapters.WrappedJsonAdapter;
import com.squareup.moshi.Moshi;
import com.uber.autodispose.CompletableScoper;
import com.uber.autodispose.ObservableScoper;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.EpochInstantJsonAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.medium.MediumService;
import io.sweers.catchup.data.medium.model.Collection;
import io.sweers.catchup.data.medium.model.MediumPost;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Qualifier;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

import static io.sweers.catchup.data.RemoteConfigKeys.SMMRY_ENABLED;

public final class MediumController extends BaseNewsController<MediumPost> {

  @Inject LinkManager linkManager;
  @Inject FirebaseRemoteConfig remoteConfig;
  @Inject MediumService service;

  public MediumController() {
    super();
  }

  public MediumController(Bundle args) {
    super(args);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Medium);
  }

  @Override
  protected void bindItemView(@NonNull MediumPost item, @NonNull NewsItemViewHolder holder) {
    holder.title(item.post()
        .title());

    holder.score(Pair.create(
        "\u2665\uFE0E", // Because lol: https://code.google.com/p/android/issues/detail?id=231068
        item.post()
            .virtuals()
            .recommends()));
    holder.timestamp(item.post()
        .createdAt());

    holder.author(item.user()
        .name());

    Collection collection = item.collection();
    if (collection != null) {
      holder.tag(collection.name());
    } else {
      holder.tag(null);
    }

    holder.comments(item.post()
        .virtuals()
        .responsesCreatedCount());
    holder.source(null);

    if (remoteConfig.getBoolean(SMMRY_ENABLED)) {
      holder.itemLongClicks()
          .to(new ObservableScoper<>(holder))
          .subscribe(SmmryController.showFor(this,
              item.constructUrl(),
              item.post()
                  .title()));
    }

    holder.itemClicks()
        .compose(transformUrlToMeta(item.constructUrl()))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
    holder.itemCommentClicks()
        .compose(transformUrlToMeta(item.constructCommentsUrl()))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
  }

  @NonNull @Override protected Single<List<MediumPost>> getDataSingle(DataRequest request) {
    setMoreDataAvailable(false);
    return service.top()
        .flatMap(references -> Observable.fromIterable(references.post()
            .values())
            .map(post -> MediumPost.builder()
                .post(post)
                .user(references.user()
                    .get(post.creatorId()))
                .collection(references.collection()
                    .get(post.homeCollectionId()))
                .build()))
        .toList();
  }

  @Subcomponent
  public interface Component extends AndroidInjector<MediumController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<MediumController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    @Qualifier
    private @interface InternalApi {}

    @Binds @IntoMap @ControllerKey(MediumController.class)
    abstract AndroidInjector.Factory<? extends Controller> bindMediumControllerInjectorFactory(
        Component.Builder builder);

    @Provides @InternalApi static OkHttpClient provideMediumOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addInterceptor(chain -> {
            Request request = chain.request();
            request = request.newBuilder()
                .url(request.url()
                    .newBuilder()
                    .addQueryParameter("format", "json")
                    .build())
                .build();
            Response response = chain.proceed(request);
            BufferedSource source = response.body()
                .source();
            source.skip(source.indexOf((byte) '{'));
            return response;
          })
          .build();
    }

    @Provides @InternalApi static Moshi provideMediumMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Instant.class, new EpochInstantJsonAdapter(TimeUnit.MILLISECONDS))
          .add(WrappedJsonAdapter.FACTORY)
          .build();
    }

    @Provides
    static MediumService provideMediumService(@InternalApi final Lazy<OkHttpClient> client,
        @InternalApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      Retrofit retrofit = new Retrofit.Builder().baseUrl(MediumService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .validateEagerly(BuildConfig.DEBUG)
          .build();
      return retrofit.create(MediumService.class);
    }
  }
}
