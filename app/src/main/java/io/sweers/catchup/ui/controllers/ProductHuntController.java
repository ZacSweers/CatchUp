package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Pair;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.reactivex.Maybe;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.producthunt.ProductHuntService;
import io.sweers.catchup.data.producthunt.model.Post;
import io.sweers.catchup.data.producthunt.model.PostsResponse;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;


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
  protected void bindItemView(@NonNull Post item, @NonNull ViewHolder holder) {
    holder.title(item.name());
    holder.score(Pair.create("▲", item.votes_count()));
    holder.timestamp(item.created_at());
    holder.author(item.user().name());
    holder.tag(item.getFirstTopic());
    holder.source(null);
    holder.comments(item.comments_count());

    holder.itemClicks()
        .compose(transformUrl(item.redirect_url()))
        .subscribe(linkManager);
    holder.itemCommentClicks()
        .compose(transformUrl(item.discussion_url()))
        .subscribe(linkManager);
  }

  @NonNull
  @Override
  protected Maybe<List<Post>> getDataObservable() {
    return service.getPosts(0)
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
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
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
