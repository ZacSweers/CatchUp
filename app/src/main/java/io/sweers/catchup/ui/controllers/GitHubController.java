package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.apollographql.android.rx2.Rx2Apollo;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.bluelinelabs.conductor.Controller;
import com.uber.autodispose.CompletableScoper;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.HttpUrlApolloAdapter;
import io.sweers.catchup.data.ISO8601InstantApolloAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.github.GitHubSearch;
import io.sweers.catchup.data.github.GitHubSearch.Data.Languages;
import io.sweers.catchup.data.github.GitHubSearch.Data.Node;
import io.sweers.catchup.data.github.TrendingTimespan;
import io.sweers.catchup.data.github.model.Repository;
import io.sweers.catchup.data.github.model.SearchQuery;
import io.sweers.catchup.data.github.model.User;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.type.CustomType;
import io.sweers.catchup.type.LanguageOrder;
import io.sweers.catchup.type.LanguageOrderField;
import io.sweers.catchup.type.OrderDirection;
import io.sweers.catchup.ui.base.BaseNewsController;
import io.sweers.catchup.util.Lists;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Qualifier;
import okhttp3.OkHttpClient;

public final class GitHubController extends BaseNewsController<Repository> {

  @Inject ApolloClient apolloClient;
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

    String query = SearchQuery.builder()
        .createdSince(TrendingTimespan.WEEK.createdSince())
        .minStars(50)
        .build()
        .toString();

    //noinspection ConstantConditions it's not null here
    ApolloCall<GitHubSearch.Data> searchQuery = apolloClient.newCall(new GitHubSearch(query,
        50,
        LanguageOrder.builder()
            .direction(OrderDirection.DESC)
            .field(LanguageOrderField.SIZE)
            .build()))
        .cacheControl(CacheControl.CACHE_FIRST);

    return Rx2Apollo.from(searchQuery)
        .flatMap(data -> Observable.fromIterable(Lists.emptyIfNull(data.search()
            .nodes()))
            .map(Node::asRepository)
            .map(node -> {
              String primaryLanguage = null;
              Languages langs = node.languages();
              if (langs != null && langs.nodes() != null) {
                List<GitHubSearch.Data.Node1> nodes = langs.nodes();
                if (nodes != null && !nodes.isEmpty()) {
                  primaryLanguage = nodes.get(0)
                      .name();
                }
              }
              return Repository.builder()
                  .createdAt(node.createdAt())
                  .fullName(node.name())
                  .htmlUrl(node.url()
                      .toString())
                  .id(node.id()
                      .hashCode())
                  .language(primaryLanguage)
                  .name(node.name())
                  .owner(User.create(node.owner()
                      .login()))
                  .starsCount(node.stargazers()
                      .totalCount())
                  .build();
            })
            .toList())
        .subscribeOn(Schedulers.io());
  }

  @Subcomponent
  public interface Component extends AndroidInjector<GitHubController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<GitHubController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    private static final String SERVER_URL = "https://api.github.com/graphql";

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

    @Provides static CacheKeyResolver<Map<String, Object>> provideCacheKeyResolver() {
      return new CacheKeyResolver<Map<String, Object>>() {
        @NonNull @Override public CacheKey resolve(@NonNull Map<String, Object> objectSource) {
          //Specific id for User type.
          if (objectSource.get("__typename")
              .equals("User")) {
            String userKey = objectSource.get("__typename") + "." + objectSource.get("login");
            return CacheKey.from(userKey);
          }
          //Use id as default case.
          if (objectSource.containsKey("id")) {
            String typeNameAndIDKey = objectSource.get("__typename") + "." + objectSource.get("id");
            return CacheKey.from(typeNameAndIDKey);
          }
          return CacheKey.NO_KEY;
        }
      };
    }

    @Provides static NormalizedCacheFactory provideNormalizedCacheFactory(
        @ApplicationContext Context context) {
      ApolloSqlHelper apolloSqlHelper = new ApolloSqlHelper(context, "githubdb");
      return new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION,
          new SqlNormalizedCacheFactory(apolloSqlHelper));
    }

    @Provides static ApolloClient provideApolloClient(@InternalApi final Lazy<OkHttpClient> client,
        NormalizedCacheFactory cacheFactory,
        CacheKeyResolver<Map<String, Object>> resolver) {
      return ApolloClient.builder()
          .serverUrl(SERVER_URL)
          .okHttpClient(client.get())
          .normalizedCache(cacheFactory, resolver)
          .withCustomTypeAdapter(CustomType.DATETIME, new ISO8601InstantApolloAdapter())
          .withCustomTypeAdapter(CustomType.URI, new HttpUrlApolloAdapter())
          .build();
    }
  }
}
