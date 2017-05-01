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
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.ISO8601InstantAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.producthunt.ProductHuntService;
import io.sweers.catchup.data.producthunt.model.Post;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Qualifier;
import okhttp3.OkHttpClient;
import okio.BufferedSource;
import okio.Okio;
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import timber.log.Timber;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV1Observable;

public final class ProductHuntController extends BaseNewsController<Post> {

  @Inject Store<List<Post>, Integer> store;
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
    if (request.multipage()) {
      // Backfill pages
      return Observable.range(0, request.page())
          .flatMapSingle(this::getPage)
          .collectInto(new ArrayList<>(), List::addAll);
    } else if (request.fromRefresh()) {
      Completable clearCompletable = Completable.fromAction(() -> store.clear());

      // TODO temporary until https://github.com/NYTimes/Store/issues/142 is fixed
      // Just a hack that clears the first potential 15 pages ¯\_(ツ)_/¯
      // Actually this doesn't work either :|
      clearCompletable = Observable.range(0, 10)
          .doOnNext(page -> store.clear(page))
          .ignoreElements();

      return clearCompletable
          .andThen(RxJavaInterop.toV2Observable(store.fetch(request.page()))
              .firstOrError());
    } else {
      return getPage(request.page());
    }
  }

  private Single<List<Post>> getPage(int page) {
    return RxJavaInterop.toV2Observable(store.get(page))
        .firstOrError();
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
          .add(WrappedJsonAdapter.FACTORY)
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

    @Provides static Persister<BufferedSource, Integer> providePersister(
        @ApplicationContext Context context) {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        Timber.e("Persister on main thread!");
        //throw new IllegalStateException("Persister initialized on main thread.");
      }
      try {
        return FileSystemPersister.create(FileSystemFactory.create(context.getFilesDir()),
            key -> "producthunt" + File.pathSeparator + key.toString());
      } catch (IOException e) {
        throw new RuntimeException("Creating FS persister failed", e);
      }
    }

    @Provides static Store<List<Post>, Integer> providePersistedPostsStore(@InternalApi Moshi moshi,
        ProductHuntService service,
        Persister<BufferedSource, Integer> persister) {
      final Type postListType = Types.newParameterizedType(List.class, Post.class);
      final JsonAdapter<List<Post>> adapter = moshi.adapter(postListType);
      return StoreBuilder.<Integer, BufferedSource, List<Post>>parsedWithKey().fetcher(page -> {
        return toV1Observable(service.getPosts(page)
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
          .parser(MoshiParserFactory.createSourceParser(moshi, postListType))
          .open();
    }
  }
}
