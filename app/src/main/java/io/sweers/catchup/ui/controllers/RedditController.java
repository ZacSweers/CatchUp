package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ryanharter.auto.value.gson.AutoValueGsonTypeAdapterFactory;

import org.threeten.bp.Instant;

import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.reddit.EpochInstantTypeAdapter;
import io.sweers.catchup.data.reddit.RedditService;
import io.sweers.catchup.data.reddit.model.RedditLink;
import io.sweers.catchup.data.reddit.model.RedditObject;
import io.sweers.catchup.data.reddit.model.RedditObjectDeserializer;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;


public final class RedditController extends BaseNewsController<RedditLink> {

  @Inject RedditService service;
  @Inject LinkManager linkManager;

  public RedditController() {
    this(null);
  }

  public RedditController(Bundle args) {
    super(args);
  }

  @Override
  protected void performInjection() {
    DaggerRedditController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override
  protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Reddit);
  }

  @Override
  protected void bindItemView(@NonNull RedditLink link, @NonNull ViewHolder holder) {
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
        .compose(transformUrl(link.url()))
        .subscribe(linkManager);
    holder.itemCommentClicks()
        .compose(transformUrl("https://reddit.com/comments/" + link.id()))
        .subscribe(linkManager);
  }

  @NonNull
  @Override
  protected Observable<List<RedditLink>> getDataObservable() {
    return service.frontPage(50)
        .map((redditListingRedditResponse) -> {
          //noinspection CodeBlock2Expr,unchecked
          return (List<RedditLink>) redditListingRedditResponse.data().children();
        });
  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(RedditController controller);
  }

  @dagger.Module
  public static class Module {

    @Provides
    @PerController
    Gson provideGson() {
      return new GsonBuilder()
          .registerTypeAdapter(RedditObject.class, new RedditObjectDeserializer())
          .registerTypeAdapter(Instant.class, new EpochInstantTypeAdapter(true))
          .registerTypeAdapterFactory(new AutoValueGsonTypeAdapterFactory())
          .create();
    }

    @ForApi
    @Provides
    @PerController
    OkHttpClient provideRedditOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addNetworkInterceptor(chain -> {
            Request request = chain.request();
            HttpUrl url = request.url();
            request = request.newBuilder()
                .header("User-Agent", "CatchUp app by /u/pandanomic")
                .url(url.newBuilder().encodedPath(url.encodedPath() + ".json").build())
                .build();
            return chain.proceed(request);
          })
          .build();
    }

    @Provides
    @PerController
    RedditService provideRedditService(
        @ForApi final Lazy<OkHttpClient> client,
        RxJavaCallAdapterFactory rxJavaCallAdapterFactory,
        Gson gson) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl(RedditService.ENDPOINT)
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(GsonConverterFactory.create(gson))
          .build();
      return retrofit.create(RedditService.class);
    }
  }
}
