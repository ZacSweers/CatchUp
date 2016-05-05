package io.sweers.catchup.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.R;
import io.sweers.catchup.data.reddit.UtcDateTypeAdapter;
import io.sweers.catchup.data.reddit.RedditService;
import io.sweers.catchup.data.reddit.model.RedditLink;
import io.sweers.catchup.data.reddit.model.RedditObject;
import io.sweers.catchup.data.reddit.model.RedditObjectDeserializer;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;


public final class RedditController extends BaseNewsController<RedditLink> {

  @Inject RedditService service;
  @Inject CustomTabActivityHelper customTab;

  public RedditController() {
    this(null);
  }

  public RedditController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerRedditController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Reddit);
  }

  @Override
  protected void bindItemView(@NonNull BaseNewsController<RedditLink>.ViewHolder holder, @NonNull View view, @NonNull RedditLink link) {
    holder.title(link.getTitle());

    holder.score(String.format(Locale.getDefault(), "+ %d", link.getScore()));
    holder.timestamp(link.getCreatedUtc());
    holder.author("/u/" + link.getAuthor());

    if (link.getDomain() != null) {
      holder.source(link.getDomain());
    } else {
      holder.source("self");
    }

    holder.comments(link.getNumComments());
  }

  @Override
  protected void onItemClick(@NonNull BaseNewsController<RedditLink>.ViewHolder holder, @NonNull View view, @NonNull RedditLink link) {
    customTab.openCustomTab(customTab.getCustomTabIntent()
            .setToolbarColor(getServiceThemeColor())
            .build(),
        Uri.parse(link.getUrl()));
  }

  @Override
  protected void onCommentClick(@NonNull BaseNewsController<RedditLink>.ViewHolder holder, @NonNull View view, @NonNull RedditLink link) {
    // TODO Make the app choice a pref
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com/comments/" + link.getId()));
    startActivity(intent);

    // TODO This should be how it's actually done
//      customTab.openCustomTab(customTab.getCustomTabIntent()
//    .setToolbarColor(getServiceThemeColor())
//        .build(),
//          Uri.parse("https://reddit.com/comments/" + link.getId()));
  }

  @NonNull @Override protected Observable<List<RedditLink>> getDataObservable() {
    return service.frontPage(50)
        .map((redditListingRedditResponse) -> {
          //noinspection CodeBlock2Expr,unchecked
          return (List<RedditLink>) redditListingRedditResponse.getData().getChildren();
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
          .registerTypeAdapter(Date.class, new UtcDateTypeAdapter(true))
          .create();
    }

    @Provides
    @PerController
    RedditService provideRedditService(
        final Lazy<OkHttpClient> client,
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
