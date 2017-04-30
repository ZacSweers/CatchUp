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
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.bluelinelabs.conductor.Controller;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.nytimes.android.external.fs.FileSystemPersister;
import com.nytimes.android.external.fs.filesystem.FileSystemFactory;
import com.nytimes.android.external.store.base.Persister;
import com.nytimes.android.external.store.base.impl.MemoryPolicy;
import com.nytimes.android.external.store.base.impl.Store;
import com.nytimes.android.external.store.base.impl.StoreBuilder;
import com.nytimes.android.external.store.middleware.moshi.MoshiParserFactory;
import com.serjltt.moshi.adapters.WrappedJsonAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.uber.autodispose.CompletableScoper;
import com.uber.autodispose.ObservableScoper;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
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
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Qualifier;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import okio.Okio;
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import timber.log.Timber;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV1Observable;
import static io.sweers.catchup.data.RemoteConfigKeys.SMMRY_ENABLED;

public final class MediumController extends BaseNewsController<MediumPost> {

  @Inject LinkManager linkManager;
  @Inject FirebaseRemoteConfig remoteConfig;
  @Inject Store<List<MediumPost>, String> store;

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
          .subscribe(SmmryController.showFor(this, item.constructUrl()));
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
    if (request.fromRefresh()) {
      return Completable.fromAction(() -> store.clear())
          .andThen(RxJavaInterop.toV2Observable(store.fetch(""))
              .firstOrError());
    } else {
      return RxJavaInterop.toV2Observable(store.get(""))
          .firstOrError();
    }
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

    @Provides
    static Persister<BufferedSource, String> providePersister(@ApplicationContext Context context) {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        Timber.e("Persister on main thread!");
        //throw new IllegalStateException("Persister initialized on main thread.");
      }
      try {
        return FileSystemPersister.create(FileSystemFactory.create(context.getFilesDir()),
            key -> "medium");
      } catch (IOException e) {
        throw new RuntimeException("Creating FS persister failed", e);
      }
    }

    @Provides
    static Store<List<MediumPost>, String> providePersistedMediumStore(@InternalApi Moshi moshi,
        MediumService service,
        Persister<BufferedSource, String> persister) {
      final Type mediumPostListType = Types.newParameterizedType(List.class, MediumPost.class);
      final JsonAdapter<List<MediumPost>> adapter = moshi.adapter(mediumPostListType);
      return StoreBuilder.<String, BufferedSource, List<MediumPost>>parsedWithKey().fetcher(ignored -> {
        return toV1Observable(service.top()
            .flatMap(references -> Observable.fromIterable(references.post()
                .values())
                .map(post -> MediumPost.builder()
                    .post(post)
                    .user(references.user()
                        .get(post.creatorId()))
                    .collection(references.collection()
                        .get(post.homeCollectionId()))
                    .build()))
            .toList()
            .map(value -> {
              // Ew - because the FS persister only supports BufferedSource
              return Okio.buffer(Okio.source(new ByteArrayInputStream(adapter.toJson(value)
                  .getBytes(StandardCharsets.UTF_8))));
            })
            .toObservable(), BackpressureStrategy.ERROR);
      })
          .persister(persister)
          .memoryPolicy(MemoryPolicy.builder()
              .setExpireAfter(1)
              .setExpireAfterTimeUnit(TimeUnit.HOURS)
              .build())
          .parser(MoshiParserFactory.createSourceParser(moshi, mediumPostListType))
          .open();
    }
  }
}
