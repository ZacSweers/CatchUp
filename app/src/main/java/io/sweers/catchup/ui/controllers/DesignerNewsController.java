package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Rfc3339DateJsonAdapter;
import dagger.Lazy;
import dagger.Provides;
import io.reactivex.Single;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.designernews.DesignerNewsService;
import io.sweers.catchup.data.designernews.model.StoriesResponse;
import io.sweers.catchup.data.designernews.model.Story;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.rx.autodispose.AutoDispose;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public final class DesignerNewsController extends BaseNewsController<Story> {

  @Inject DesignerNewsService service;
  @Inject LinkManager linkManager;

  public DesignerNewsController() {
    super();
  }

  public DesignerNewsController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerDesignerNewsController_Component.builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_DesignerNews);
  }

  @Override protected void bindItemView(@NonNull Story story, @NonNull ViewHolder holder) {
    holder.title(story.title());

    holder.score(Pair.create("▲", story.voteCount()));
    holder.timestamp(story.createdAt());
    holder.author(story.userDisplayName());

    holder.source(story.hostname());

    holder.comments(story.commentCount());
    holder.tag(story.badge());

    holder.itemClicks()
        .compose(transformUrlToMeta(story.url()))
        .flatMapCompletable(linkManager)
        .subscribe(AutoDispose.completable(this)
            .empty());
    holder.itemCommentClicks()
        .compose(transformUrlToMeta(story.siteUrl()
            .replace("api.", "www.")))
        .flatMapCompletable(linkManager)
        .subscribe(AutoDispose.completable(this)
            .empty());
  }

  @NonNull @Override protected Single<List<Story>> getDataObservable() {
    return service.getTopStories(1)
        .map(StoriesResponse::stories);
  }

  @PerController
  @dagger.Component(modules = Module.class,
                    dependencies = ActivityComponent.class)
  public interface Component {
    void inject(DesignerNewsController controller);
  }

  @dagger.Module
  public abstract static class Module {

    @Provides @ForApi @PerController
    static OkHttpClient provideDesignerNewsOkHttpClient(OkHttpClient okHttpClient) {
      return okHttpClient.newBuilder()
          .addNetworkInterceptor(chain -> {
            Request originalRequest = chain.request();
            HttpUrl originalUrl = originalRequest.url();
            return chain.proceed(originalRequest.newBuilder()
                .url(originalUrl.newBuilder()
                    .addQueryParameter("client_id", BuildConfig.DESIGNER_NEWS_CLIENT_ID)
                    .build())
                .build());
          })
          .build();
    }

    @Provides @ForApi @PerController static Moshi provideDesignerNewsMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Date.class, new Rfc3339DateJsonAdapter())
          .build();
    }

    @Provides @PerController
    static DesignerNewsService provideDesignerNewsService(@ForApi final Lazy<OkHttpClient> client,
        @ForApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      Retrofit retrofit = new Retrofit.Builder().baseUrl(DesignerNewsService.ENDPOINT)
          .callFactory(request -> client.get()
              .newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build();
      return retrofit.create(DesignerNewsService.class);
    }
  }
}
