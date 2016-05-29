package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Pair;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.producthunt.ProductHuntService;
import io.sweers.catchup.data.producthunt.model.Post;
import io.sweers.catchup.data.producthunt.model.PostsResponse;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.rx.Confine;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public final class ProductHuntController extends BaseNewsController<Post> {

  @Inject ProductHuntService service;
  @Inject LinkManager linkManager;

  public ProductHuntController() {
    this(null);
  }

  public ProductHuntController(Bundle args) {
    super(args);
  }

  @Override
  protected void performInjection() {
    DaggerProductHuntController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override
  protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_ProductHunt);
  }

  @Override
  protected void bindItemView(@NonNull Post post, @NonNull ViewHolder holder) {
    holder.title(post.name());
    holder.score(Pair.create("▲", post.votes_count()));
    holder.timestamp(post.created_at());
    holder.author(post.user().name());
    holder.tag(post.getFirstTopic());
    holder.comments(post.comments_count());

    if (post.redirect_url() != null) {
      if (post.hasCachedRedirectHost()) {
        holder.source(post.getCachedRedirectHost());
      } else {
        holder.source(null);
        Observable<String> resolveHostObservable = resolveHost(post, holder);

        // Neat little effect to indicate that host is still being resolved
        // .
        // ..
        // ...
        // .
        // ..
        // github.com
        Observable.interval(300, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.computation())
            .takeUntil(resolveHostObservable)
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToHolder(holder))
            .compose(Confine.to(this))
            .subscribe(tick -> {
              char[] chars = new char[(int) (tick % 4)];
              Arrays.fill(chars, '.');
              String s = new String(chars);
              holder.source(s);
            });
        resolveHostObservable
            .subscribe(holder::source, throwable -> {
              holder.source(null);
            });
      }
    }

    holder.itemClicks()
        .compose(transformUrl(post.redirect_url()))
        .subscribe(linkManager);
    holder.itemCommentClicks()
        .compose(transformUrl(post.discussion_url()))
        .subscribe(linkManager);
  }

  private Observable<String> resolveHost(@NonNull Post post, @NonNull ViewHolder holder) {
    return service.resolveDomain(post.redirect_url())
        .subscribeOn(Schedulers.io())
        .map(response -> response.raw().request().url().host())
        .doOnNext(post::setCachedRedirectHost)
        .observeOn(AndroidSchedulers.mainThread())
        .compose(bindToHolder(holder))
        .compose(Confine.to(this));
  }

  @NonNull
  @Override
  protected Observable<List<Post>> getDataObservable() {
    return service.getPosts(1)
        .map(PostsResponse::posts);

  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(ProductHuntController controller);
  }

  @dagger.Module
  public static class Module {

    @Provides
    @PerController
    @ForApi
    OkHttpClient provideProductHuntOkHttpClient(OkHttpClient client) {
      return client
          .newBuilder()
          .addInterceptor(AuthInterceptor.create("Bearer", BuildConfig.PROCUCT_HUNT_DEVELOPER_TOKEN))
          .build();
    }

    @Provides
    @PerController
    @ForApi
    Moshi provideProductHuntMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Date.class, new Rfc3339DateJsonAdapter())
          .build();
    }

    @Provides
    @PerController
    ProductHuntService provideProductHuntService(
        @ForApi final Lazy<OkHttpClient> client,
        @ForApi Moshi moshi,
        RxJavaCallAdapterFactory rxJavaCallAdapterFactory) {
      return new Retrofit.Builder()
          .baseUrl(ProductHuntService.ENDPOINT)
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(ProductHuntService.class);
    }
  }
}
