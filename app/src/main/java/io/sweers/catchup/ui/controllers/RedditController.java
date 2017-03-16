package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.bluelinelabs.conductor.Controller;
import com.squareup.moshi.Moshi;
import com.uber.autodispose.CompletableScoper;
import com.uber.autodispose.ObservableScoper;
import dagger.Binds;
import dagger.Lazy;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.reactivex.Single;
import io.sweers.catchup.R;
import io.sweers.catchup.data.EpochInstantJsonAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.reddit.RedditService;
import io.sweers.catchup.data.reddit.model.RedditLink;
import io.sweers.catchup.data.reddit.model.RedditObjectFactory;
import io.sweers.catchup.data.smmry.SmmryService;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Qualifier;
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
        .to(new CompletableScoper(holder))
        .subscribe();

    holder.itemLongClicks()
        .to(new ObservableScoper<>(holder))
        .subscribe(SmmryController.showFor(this, link.url()));
    holder.itemCommentClicks()
        .compose(transformUrlToMeta("https://reddit.com/comments/" + link.id()))
        .flatMapCompletable(linkManager)
        .to(new CompletableScoper(holder))
        .subscribe();
  }

  @NonNull @Override protected Single<List<RedditLink>> getDataSingle() {
    return service.frontPage(50)
        .map((redditListingRedditResponse) -> {
          //noinspection CodeBlock2Expr,unchecked
          return (List<RedditLink>) redditListingRedditResponse.data()
              .children();
        });
  }

  @Subcomponent
  public interface Component extends AndroidInjector<RedditController> {

    @Subcomponent.Builder
    abstract class Builder extends AndroidInjector.Builder<RedditController> {}
  }

  @dagger.Module(subcomponents = Component.class)
  public abstract static class Module {

    @Qualifier
    private @interface InternalApi {}

    @Binds @IntoMap @ControllerKey(RedditController.class)
    abstract AndroidInjector.Factory<? extends Controller> bindRedditControllerInjectorFactory(
        Component.Builder builder);

    @InternalApi @Provides static Moshi provideMoshi(Moshi upstreamMoshi) {
      return upstreamMoshi.newBuilder()
          .add(RedditObjectFactory.getInstance())
          .add(Instant.class, new EpochInstantJsonAdapter(TimeUnit.SECONDS))
          .build();
    }

    @InternalApi @Provides static OkHttpClient provideRedditOkHttpClient(OkHttpClient client) {
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

    @Provides
    static RedditService provideRedditService(@InternalApi final Lazy<OkHttpClient> client,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory,
        @InternalApi Moshi moshi) {
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
