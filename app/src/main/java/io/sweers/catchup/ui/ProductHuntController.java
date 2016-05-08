package io.sweers.catchup.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Pair;
import android.view.View;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;

import java.util.Date;
import java.util.List;

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
import io.sweers.catchup.injection.API;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;


public final class ProductHuntController extends BaseNewsController<Post> {

  @Inject ProductHuntService service;
  @Inject LinkManager linkManager;

  public ProductHuntController() {
    this(null);
  }

  public ProductHuntController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerProductHuntController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_ProductHunt);
  }

  @Override
  protected void bindItemView(@NonNull ViewHolder holder, @NonNull View view, @NonNull Post item) {
    holder.title(item.name());
    holder.score(Pair.create("▲", item.votes_count()));
    holder.timestamp(item.created_at());
    holder.author(item.user().name());
    holder.source(item.getFirstTopic());
    holder.comments(item.comments_count());
  }

  @Override
  protected void onItemClick(@NonNull ViewHolder holder, @NonNull View view, @NonNull Post item) {
    linkManager.openUrl(item.redirect_url());
  }

  @Override
  protected void onCommentClick(@NonNull ViewHolder holder, @NonNull View view, @NonNull Post item) {
    linkManager.openUrl(item.discussion_url());
  }

  @NonNull @Override protected Observable<List<Post>> getDataObservable() {
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
    @API
    OkHttpClient provideProductHuntOkHttpClient(OkHttpClient client) {
      return client
          .newBuilder()
          .addInterceptor(AuthInterceptor.create("Bearer", BuildConfig.PROCUCT_HUNT_DEVELOPER_TOKEN))
          .build();
    }

    @Provides
    @PerController
    @API
    Moshi provideProductHuntMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Date.class, new Rfc3339DateJsonAdapter())
          .build();
    }

    @Provides
    @PerController
    ProductHuntService provideProductHuntService(
        @API final Lazy<OkHttpClient> client,
        @API Moshi moshi,
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
