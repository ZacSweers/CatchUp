package io.sweers.catchup.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;

import com.squareup.moshi.Moshi;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.github.GitHubService;
import io.sweers.catchup.data.InstantAdapter;
import io.sweers.catchup.data.github.TrendingTimespan;
import io.sweers.catchup.data.github.model.Order;
import io.sweers.catchup.data.github.model.Repository;
import io.sweers.catchup.data.github.model.SearchQuery;
import io.sweers.catchup.data.github.model.SearchRepositoriesResult;
import io.sweers.catchup.injection.API;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BasicNewsController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;


public final class GitHubController extends BasicNewsController<Repository> {

  @Inject GitHubService service;
  @Inject CustomTabActivityHelper customTab;

  public GitHubController() {
    this(null);
  }

  public GitHubController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerGitHubController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_GitHub);
  }

  @Override
  protected void bindItemView(@NonNull ViewHolder holder, @NonNull View view, @NonNull Repository item) {
    holder.comments().setVisibility(View.GONE);
    holder.title(item.full_name());
    holder.score(String.format(Locale.getDefault(), "★ %d", item.stargazers_count()));
    holder.timestamp(item.created_at());
    holder.author(item.owner().login());
    holder.source(item.language());
//    holder.comments(item.comments_count());
  }

  @Override
  protected void onItemClick(@NonNull ViewHolder holder, @NonNull View view, @NonNull Repository item) {
    // TODO Make the app choice a pref
    customTab.openCustomTab(customTab.getCustomTabIntent()
            .setToolbarColor(getServiceThemeColor())
            .build(),
        Uri.parse(item.html_url()));
  }

  @NonNull @Override protected Observable<List<Repository>> getDataObservable() {
    return service.searchRepositories(
//        "created:>`date -v-7d '+%Y-%m-%d'`",
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
  public static class Module {

    @Provides
    @PerController
    @API
    OkHttpClient provideGitHubOkHttpClient(OkHttpClient client) {
      return client
          .newBuilder()
          .addInterceptor(new AuthInterceptor("token", BuildConfig.GITHUB_DEVELOPER_TOKEN))
          .build();
    }

    @Provides
    @PerController
    @API
    Moshi provideGitHubMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(new InstantAdapter())
          .build();
    }

    @Provides
    @PerController
    GitHubService provideGitHubService(
        @API final Lazy<OkHttpClient> client,
        @API Moshi moshi,
        RxJavaCallAdapterFactory rxJavaCallAdapterFactory) {
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
