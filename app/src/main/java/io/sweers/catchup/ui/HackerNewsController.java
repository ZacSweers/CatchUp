package io.sweers.catchup.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Pair;
import android.view.View;

import com.squareup.moshi.Moshi;

import org.threeten.bp.Instant;

import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.R;
import io.sweers.catchup.data.EpochInstantJsonAdapter;
import io.sweers.catchup.data.hackernews.HackerNewsService;
import io.sweers.catchup.data.hackernews.model.HackerNewsStory;
import io.sweers.catchup.injection.API;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;
import rx.schedulers.Schedulers;


public final class HackerNewsController extends BaseNewsController<HackerNewsStory> {

  @Inject HackerNewsService service;
  @Inject CustomTabActivityHelper customTab;

  public HackerNewsController() {
    this(null);
  }

  public HackerNewsController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerHackerNewsController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_HackerNews);
  }

  @Override
  protected void bindItemView(@NonNull BaseNewsController<HackerNewsStory>.ViewHolder holder, @NonNull View itemView, @NonNull HackerNewsStory story) {
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
  }

  @Override
  protected void onItemClick(@NonNull BaseNewsController<HackerNewsStory>.ViewHolder holder, @NonNull View view, @NonNull HackerNewsStory story) {
    // TODO Check supported media types, otherwise Chrome Custom Tabs
    customTab.openCustomTab(customTab.getCustomTabIntent()
            .setToolbarColor(getServiceThemeColor())
            .build(),
        Uri.parse(story.url()));
  }

  @Override
  protected void onCommentClick(@NonNull BaseNewsController<HackerNewsStory>.ViewHolder holder, @NonNull View view, @NonNull HackerNewsStory story) {
    // TODO Make the app choice a pref
    customTab.openCustomTab(customTab.getCustomTabIntent()
            .setToolbarColor(getServiceThemeColor())
            .build(),
        Uri.parse("https://news.ycombinator.com/item?id=" + story.id()));
  }

  @NonNull @Override protected Observable<List<HackerNewsStory>> getDataObservable() {
    return service.topStories()
        .concatMapIterable(strings -> strings)
        // TODO Pref this
        .take(50)
        .concatMap(id -> service.getItem(id).subscribeOn(Schedulers.io()))
        .toList();
  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(HackerNewsController controller);
  }

  @dagger.Module
  public static class Module {

    @Provides
    @PerController
    @API
    Moshi provideHackerNewsMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Instant.class, new EpochInstantJsonAdapter(true))
          .build();
    }

    @Provides
    @PerController
    HackerNewsService provideHackerNewsService(
        final Lazy<OkHttpClient> client,
        @API Moshi moshi,
        RxJavaCallAdapterFactory rxJavaCallAdapterFactory) {
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
