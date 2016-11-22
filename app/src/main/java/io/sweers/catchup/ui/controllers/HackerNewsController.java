package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.ContextThemeWrapper;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import io.sweers.arraysetbackport.ArraySet;
import io.sweers.catchup.ui.activity.ActivityModule;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.R;
import io.sweers.catchup.data.InstantModule;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.hackernews.HackerNewsService;
import io.sweers.catchup.data.hackernews.model.HackerNewsStory;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;


public final class HackerNewsController extends BaseNewsController<HackerNewsStory> {

  @Inject HackerNewsService service;
  @Inject LinkManager linkManager;

  public HackerNewsController() {
    this(null);
  }

  public HackerNewsController(Bundle args) {
    super(args);
  }

  @Override
  protected void performInjection() {
    DaggerHackerNewsController_Component
        .builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override
  protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_HackerNews);
  }

  @Override
  protected void bindItemView(@NonNull HackerNewsStory story, @NonNull ViewHolder holder) {
    holder.title(story.title());
    holder.score(Pair.create("+", story.score()));
    holder.timestamp(story.time());
    holder.author(story.by());

    String url = story.url();
    if (url == null) {
      holder.source(null);
    } else {
      holder.source(HttpUrl.parse(url).host());
    }

    int commentsCount = 0;
    // TODO Adapter to coerce this to Collections.emptyList()?
    List<String> kids = story.kids();
    if (kids != null) {
      commentsCount = kids.size();
    }
    holder.comments(commentsCount);
    holder.tag(null);

    holder.itemClicks()
        .compose(transformUrl(url))
        .subscribe(linkManager);

    holder.itemCommentClicks()
        .compose(transformUrl("https://news.ycombinator.com/item?id=" + story.id()))
        .subscribe(linkManager);
  }

  @NonNull
  @Override
  protected Maybe<List<HackerNewsStory>> getDataObservable() {
    return service.topStories()
        .concatMapIterable(strings -> strings)
        // TODO Pref this
        .take(50)
        .concatMap(id -> service.getItem(id).subscribeOn(Schedulers.io()))
        .toList()
        .toMaybe();
  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(HackerNewsController controller);
  }

  @dagger.Module(includes = InstantModule.class)
  public abstract static class Module {

    @ForApi
    @Provides
    @PerController
    static OkHttpClient provideHackerNewsOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addNetworkInterceptor(chain -> {
            Request request = chain.request();
            HttpUrl url = request.url();
            request = request.newBuilder()
                .url(url.newBuilder().encodedPath(url.encodedPath() + ".json").build())
                .build();
            Response originalResponse = chain.proceed(request);
            // Hacker News requests are expensive and take awhile, so cache for 5min
            int maxAge = 60 * 5;
            return originalResponse.newBuilder()
                .header("Cache-Control", "public, max-age=" + maxAge)
                .build();
          })
          .build();
    }

    @Provides
    @PerController
    @ForApi
    static Set<JsonAdapter.Factory> factories(Set<JsonAdapter.Factory> moduleFactories,
        @ActivityModule.Factories Set<JsonAdapter.Factory> parentFactories) {
      Set<JsonAdapter.Factory> factories = new ArraySet<>();
      factories.addAll(moduleFactories);
      factories.removeAll(parentFactories);
      return factories;
    }

    @Provides
    @PerController
    @ForApi
    static Moshi provideHackerNewsMoshi(@ActivityModule.Factories Moshi rootMoshi,
        @ForApi Set<JsonAdapter.Factory> factories) {

      Moshi.Builder builder = rootMoshi.newBuilder();

      // Populate from the factories, use https://github.com/square/moshi/pull/216
      Observable.from(factories).subscribe(builder::add);

      // Borrow from the parent's cache
      builder.add((type, annotations, moshi) -> rootMoshi.adapter(type, annotations));

      return builder.build();
    }

    @Provides
    @PerController
    static HackerNewsService provideHackerNewsService(
        @ForApi final Lazy<OkHttpClient> client,
        @ForApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl(HackerNewsService.ENDPOINT)
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build();
      return retrofit.create(HackerNewsService.class);
    }
  }
}
