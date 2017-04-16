package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.bluelinelabs.conductor.Controller;
import com.squareup.moshi.Moshi;
import com.uber.autodispose.CompletableScoper;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.reactivex.Single;
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
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Qualifier;
import okhttp3.OkHttpClient;
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public final class GitHubController extends BaseNewsController<Repository> {

  @Inject GitHubService service;
  @Inject LinkManager linkManager;

  public GitHubController() {
    super();
  }

  public GitHubController(Bundle args) {
    super(args);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_GitHub);
  }

  @Override protected void bindItemView(@NonNull Repository item, @NonNull ViewHolder holder) {
    holder.hideComments();
    holder.title(item.fullName());
    holder.score(Pair.create("★", item.starsCount()));
    holder.timestamp(item.createdAt());
    holder.author(item.owner()
        .login());
    holder.source(null);
    holder.tag(item.language());
    holder.itemClicks()
        .compose(transformUrlToMeta(item.htmlUrl()))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
  }

  @NonNull @Override protected Single<List<Repository>> getDataSingle() {
    return service.searchRepositories(SearchQuery.builder()
        .createdSince(TrendingTimespan.WEEK.createdSince())
        .build(), "watchers", Order.DESC)
        .map(SearchRepositoriesResult::items);
  }

  @Subcomponent
  public interface Component extends AndroidInjector<GitHubController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<GitHubController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    @Qualifier
    private @interface InternalApi {}

    @Binds @IntoMap @ControllerKey(GitHubController.class)
    abstract AndroidInjector.Factory<? extends Controller> bindGitHubControllerInjectorFactory(
        Component.Builder builder);

    @Provides @InternalApi static OkHttpClient provideGitHubOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addInterceptor(AuthInterceptor.create("token", BuildConfig.GITHUB_DEVELOPER_TOKEN))
          .build();
    }

    @Provides @InternalApi static Moshi provideGitHubMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Instant.class, new ISOInstantAdapter())
          .build();
    }

    @Provides
    static GitHubService provideGitHubService(@InternalApi final Lazy<OkHttpClient> client,
        @InternalApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      return new Retrofit.Builder().baseUrl(GitHubService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(GitHubService.class);
    }
  }
}
