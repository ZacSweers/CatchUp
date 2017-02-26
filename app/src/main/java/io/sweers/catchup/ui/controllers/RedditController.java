package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.squareup.moshi.Moshi;
import dagger.Lazy;
import dagger.Provides;
import io.reactivex.Single;
import io.sweers.catchup.R;
import io.sweers.catchup.data.AutoValueMoshiAdapterFactory;
import io.sweers.catchup.data.EpochInstantJsonAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.reddit.RedditService;
import io.sweers.catchup.data.reddit.model.RedditLink;
import io.sweers.catchup.data.reddit.model.RedditObjectJsonAdapter;
import io.sweers.catchup.data.smmry.SmmryService;
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
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public final class RedditController extends BaseNewsController<RedditLink> {

  @Inject RedditService service;
  @Inject LinkManager linkManager;
  @Inject SmmryService smmryService;

  public RedditController() {
    super();
  }

  public RedditController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerRedditController_Component.builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Reddit);
  }

  @Override protected void bindItemView(@NonNull RedditLink link, @NonNull ViewHolder holder) {
    holder.title(link.title());

    holder.score(Pair.create("+", link.score()));
    holder.timestamp(link.createdUtc());
    holder.author("/u/" + link.author());

    if (link.domain() != null) {
      holder.source(link.domain());
    } else {
      holder.source("self");
    }

    holder.comments(link.commentsCount());
    holder.tag(link.subreddit());

    holder.itemClicks()
        .compose(transformUrlToMeta(link.url()))
        .flatMapCompletable(linkManager)
        .subscribe(AutoDispose.completable(this)
            .empty());

    holder.itemLongClicks()
        .subscribe(AutoDispose.observable(this)
            .around(SmmryController.showFor(this, link.url())));
    holder.itemCommentClicks()
        .compose(transformUrlToMeta("https://reddit.com/comments/" + link.id()))
        .flatMapCompletable(linkManager)
        .subscribe(AutoDispose.completable(this)
            .empty());
  }

  @NonNull @Override protected Single<List<RedditLink>> getDataSingle() {
    return service.frontPage(50)
        .map((redditListingRedditResponse) -> {
          //noinspection CodeBlock2Expr,unchecked
          return (List<RedditLink>) redditListingRedditResponse.data()
              .children();
        });
  }

  @PerController
  @dagger.Component(modules = Module.class,
                    dependencies = ActivityComponent.class)
  public interface Component {
    void inject(RedditController controller);
  }

  @dagger.Module
  public abstract static class Module {

    @ForApi @Provides @PerController static Moshi provideMoshi(Moshi upstreamMoshi) {
      return new Moshi.Builder().add(RedditObjectJsonAdapter.FACTORY)
          .add(AutoValueMoshiAdapterFactory.create())
          //.add((type, annotations, m) -> upstreamMoshi.adapter(type, annotations))  // TODO Why doesn't this work?
          .add(Instant.class, new EpochInstantJsonAdapter(TimeUnit.SECONDS))
          .build();
    }

    @ForApi @Provides @PerController
    static OkHttpClient provideRedditOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addNetworkInterceptor(chain -> {
            Request request = chain.request();
            HttpUrl url = request.url();
            request = request.newBuilder()
                .header("User-Agent", "CatchUp app by /u/pandanomic")
                .url(url.newBuilder()
                    .encodedPath(url.encodedPath() + ".json")
                    .build())
                .build();
            return chain.proceed(request);
          })
          .build();
    }

    @Provides @PerController
    static RedditService provideRedditService(@ForApi final Lazy<OkHttpClient> client,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory,
        @ForApi Moshi moshi) {
      Retrofit retrofit = new Retrofit.Builder().baseUrl(RedditService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build();
      return retrofit.create(RedditService.class);
    }
  }
}
