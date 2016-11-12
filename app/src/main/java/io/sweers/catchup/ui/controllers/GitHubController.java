package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.ContextThemeWrapper;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.squareup.moshi.Moshi;

import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.reactivex.Maybe;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.ISOInstantAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.github.GitHubService;
import io.sweers.catchup.data.github.TrendingTimespan;
import io.sweers.catchup.data.github.model.Order;
import io.sweers.catchup.data.github.model.Repository;
import io.sweers.catchup.data.github.model.SearchQuery;
import io.sweers.catchup.data.github.model.SearchRepositoriesResult;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;


public final class GitHubController extends BaseNewsController<Repository> {

  @Inject GitHubService service;
  @Inject LinkManager linkManager;

  public GitHubController() {
    this(null);
  }

  public GitHubController(Bundle args) {
    super(args);
  }

  @Override
  protected void performInjection() {
    DaggerGitHubController_Component
        .builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override
  protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_GitHub);
  }

  @Override
  protected void bindItemView(@NonNull Repository item, @NonNull ViewHolder holder) {
    holder.hideComments();
    holder.title(item.fullName());
    holder.score(Pair.create("★", item.starsCount()));
    holder.timestamp(item.createdAt());
    holder.author(item.owner().login());
    holder.source(null);
    holder.tag(item.language());
    holder.itemClicks()
        .compose(transformUrl(item.htmlUrl()))
        .subscribe(linkManager);
  }

  @NonNull
  @Override
  protected Maybe<List<Repository>> getDataObservable() {
    return service.searchRepositories(
        SearchQuery.builder().createdSince(TrendingTimespan.WEEK.createdSince()).build(),
        "watchers",
        Order.DESC
    )
        .map(SearchRepositoriesResult::items);

  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(GitHubController controller);
  }

  @dagger.Module
  public abstract static class Module {

    @Provides
    @PerController
    @ForApi
    static OkHttpClient provideGitHubOkHttpClient(OkHttpClient client) {
      return client
          .newBuilder()
          .addInterceptor(AuthInterceptor.create("token", BuildConfig.GITHUB_DEVELOPER_TOKEN))
          .build();
    }

    @Provides
    @PerController
    @ForApi
    static Moshi provideGitHubMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(new ISOInstantAdapter())
          .build();
    }

    @Provides
    @PerController
    static GitHubService provideGitHubService(
        @ForApi final Lazy<OkHttpClient> client,
        @ForApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      return new Retrofit.Builder()
          .baseUrl(GitHubService.ENDPOINT)
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(GitHubService.class);
    }
  }
}
