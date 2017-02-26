package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import com.squareup.moshi.Moshi;
import dagger.Lazy;
import dagger.Provides;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.R;
import io.sweers.catchup.data.EpochInstantJsonAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.hackernews.HackerNewsService;
import io.sweers.catchup.data.hackernews.model.HackerNewsStory;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.rx.autodispose.AutoDispose;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public final class HackerNewsController extends BaseNewsController<HackerNewsStory> {

  @Inject HackerNewsService service;
  @Inject LinkManager linkManager;

  public HackerNewsController() {
    super();
  }

  public HackerNewsController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerHackerNewsController_Component.builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
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
      holder.source(HttpUrl.parse(url)
          .host());
    }

    int commentsCount = 0;
    // TODO Adapter to coerce this to Collections.emptyList()?
    List<String> kids = story.kids();
    if (kids != null) {
      commentsCount = kids.size();
    }
    holder.comments(commentsCount);
    holder.tag(null);

    if (!TextUtils.isEmpty(url)) {
      holder.itemLongClicks()
          .subscribe(AutoDispose.observable(this)
              .around(SmmryController.showFor(this, url)));
    }

    holder.itemClicks()
        .compose(transformUrlToMeta(url))
        .flatMapCompletable(linkManager)
        .subscribe(AutoDispose.completable(this)
            .empty());

    holder.itemCommentClicks()
        .compose(transformUrlToMeta("https://news.ycombinator.com/item?id=" + story.id()))
        .flatMapCompletable(linkManager)
        .subscribe(AutoDispose.completable(this)
            .empty());
  }

  @NonNull @Override protected Single<List<HackerNewsStory>> getDataSingle() {
    return service.topStories()
        .flattenAsFlowable(strings -> strings)
        .take(50) // TODO Pref this
        .zipWith(Flowable.range(0, Integer.MAX_VALUE), Pair::create) // "Map with index"
        .parallel()
        .runOn(Schedulers.io())
        .flatMap(pair -> service.getItem(pair.first)
            .map(story -> Pair.create(story, pair.second))
            .toFlowable())
        .sorted((o1, o2) -> o1.second.compareTo(o2.second))
        .map(pair -> pair.first)
        .toList();
  }

  @PerController
  @dagger.Component(modules = Module.class,
                    dependencies = ActivityComponent.class)
  public interface Component {
    void inject(HackerNewsController controller);
  }

  @dagger.Module
  public abstract static class Module {

    @ForApi @Provides @PerController
    static OkHttpClient provideHackerNewsOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addNetworkInterceptor(chain -> {
            Request request = chain.request();
            HttpUrl url = request.url();
            request = request.newBuilder()
                .url(url.newBuilder()
                    .encodedPath(url.encodedPath() + ".json")
                    .build())
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

    @Provides @PerController @ForApi static Moshi provideHackerNewsMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Instant.class, new EpochInstantJsonAdapter(TimeUnit.SECONDS))
          .build();
    }

    @Provides @PerController
    static HackerNewsService provideHackerNewsService(@ForApi final Lazy<OkHttpClient> client,
        @ForApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      Retrofit retrofit = new Retrofit.Builder().baseUrl(HackerNewsService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build();
      return retrofit.create(HackerNewsService.class);
    }
  }
}
