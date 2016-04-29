package io.sweers.catchup.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.squareup.moshi.Moshi;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.model.HackerNewsStory;
import io.sweers.catchup.network.HackerNewsService;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BasicNewsController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;

import static rx.schedulers.Schedulers.io;


public final class HackerNewsController extends BasicNewsController<HackerNewsStory> {

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

  @Override
  protected void bindItemView(@NonNull BasicNewsController<HackerNewsStory>.ViewHolder holder, @NonNull View itemView, @NonNull HackerNewsStory story) {
    holder.title(story.title());
    holder.score(String.format(Locale.getDefault(), "+ %d", story.score()));
    holder.timestamp(story.time());
    holder.author(story.by());
    holder.source(HttpUrl.parse(story.url()).host());

    int commentsCount = 0;
    // TODO Adapter to coerce this to Collections.emptyList()?
    List<String> kids = story.kids();
    if (kids != null) {
      kids.size();
    }
    holder.comments(commentsCount);
  }

  @Override
  protected void onItemClick(@NonNull BasicNewsController<HackerNewsStory>.ViewHolder holder, @NonNull View view, @NonNull HackerNewsStory story) {
    // TODO Check supported media types, otherwise Chrome Custom Tabs
    customTab.openCustomTab(customTab.getCustomTabIntent().build(),
        Uri.parse(story.url()));
  }

  @Override
  protected void onCommentClick(@NonNull BasicNewsController<HackerNewsStory>.ViewHolder holder, @NonNull View view, @NonNull HackerNewsStory story) {
    // TODO Make the app choice a pref
    customTab.openCustomTab(customTab.getCustomTabIntent().build(),
        Uri.parse("https://news.ycombinator.com/item?id=" + story.id()));
  }

  @NonNull @Override protected Observable<List<HackerNewsStory>> getDataObservable() {
    return service.topStories()
        .concatMapIterable(strings -> strings)
        .take(25)
        .concatMap(id -> service.getItem(id))
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
    HackerNewsService provideHackerNewsService(final Lazy<OkHttpClient> client, Moshi moshi) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl("https://hacker-news.firebaseio.com/v0/")
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(io()))
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build();
      return retrofit.create(HackerNewsService.class);
    }
  }
}
