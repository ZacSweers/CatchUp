package io.sweers.catchup.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Pair;
import android.widget.Toast;

import com.squareup.moshi.Moshi;

import org.threeten.bp.Instant;

import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.R;
import io.sweers.catchup.data.EpochInstantJsonAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.hackernews.HackerNewsService;
import io.sweers.catchup.data.hackernews.model.HackerNewsStory;
import io.sweers.catchup.injection.ForApi;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;
import rx.schedulers.Schedulers;


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
        .module(new Module())
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

    holder.itemClicks()
        .subscribe(v -> {
          if (url == null) {
            Toast.makeText(holder.itemView.getContext(), R.string.error_no_url, Toast.LENGTH_SHORT).show();
          } else {
            linkManager.openUrl(url);
          }
        });

    holder.itemCommentClicks()
        .subscribe(v -> linkManager.openUrl("https://news.ycombinator.com/item?id=" + story.id()));
  }

  @NonNull
  @Override
  protected Observable<List<HackerNewsStory>> getDataObservable() {
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
    @ForApi
    Moshi provideHackerNewsMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Instant.class, new EpochInstantJsonAdapter(true))
          .build();
    }

    @Provides
    @PerController
    HackerNewsService provideHackerNewsService(
        final Lazy<OkHttpClient> client,
        @ForApi Moshi moshi,
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
