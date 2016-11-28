package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.ContextThemeWrapper;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.squareup.moshi.Moshi;
import dagger.Lazy;
import dagger.Provides;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.sweers.catchup.R;
import io.sweers.catchup.data.EpochInstantJsonAdapter;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.medium.MediumService;
import io.sweers.catchup.data.medium.model.Collection;
import io.sweers.catchup.data.medium.model.MediumPost;
import io.sweers.catchup.data.medium.model.MediumResponse;
import io.sweers.catchup.data.medium.model.Payload;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import java.util.List;
import javax.inject.Inject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import org.threeten.bp.Instant;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;


public final class MediumController extends BaseNewsController<MediumPost> {

  @Inject MediumService service;
  @Inject LinkManager linkManager;

  public MediumController() {
    this(null);
  }

  public MediumController(Bundle args) {
    super(args);
  }

  @Override
  protected void performInjection() {
    DaggerMediumController_Component
        .builder()
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override
  protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Medium);
  }

  @Override
  protected void bindItemView(@NonNull MediumPost item, @NonNull ViewHolder holder) {
    holder.title(item.post().title());

    holder.score(Pair.create("♥", item.post().virtuals().recommends()));
    holder.timestamp(item.post().createdAt());

    holder.author(item.user().name());

    Collection collection = item.collection();
    if (collection != null) {
      holder.tag(collection.name());
    } else {
      holder.tag(null);
    }

    holder.comments(item.post().virtuals().responsesCreatedCount());
    holder.source(null);

    holder.itemClicks()
        .compose(transformUrl(item.constructUrl()))
        .subscribe(linkManager);
    holder.itemCommentClicks()
        .compose(transformUrl(item.constructCommentsUrl()))
        .subscribe(linkManager);
  }

  @NonNull
  @Override
  protected Single<List<MediumPost>> getDataObservable() {
    return service.top()
        .map(MediumResponse::payload)
        .map(Payload::references)
        .flatMap(references -> Observable.fromIterable(references.post().values())
            .map(post -> MediumPost.builder()
                .post(post)
                .user(references.user().get(post.creatorId()))
                .collection(references.collection().get(post.homeCollectionId()))
                .build())
        )
        .toList();
  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(MediumController controller);
  }

  @dagger.Module
  public abstract static class Module {

    @Provides
    @PerController
    @ForApi
    static OkHttpClient provideMediumOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addInterceptor(chain -> {
            Request request = chain.request();
            request = request.newBuilder()
                .url(request.url().newBuilder()
                    .addQueryParameter("format", "json")
                    .build())
                .build();
            Response response = chain.proceed(request);
            BufferedSource source = response.body().source();
            source.skip(source.indexOf((byte) '{'));
            return response;
          })
          .build();
    }

    @Provides
    @PerController
    @ForApi
    static Moshi provideMediumMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Instant.class, new EpochInstantJsonAdapter())
          .build();
    }

    @Provides
    @PerController
    static MediumService provideMediumService(
        @ForApi final Lazy<OkHttpClient> client,
        @ForApi Moshi moshi,
        RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl(MediumService.ENDPOINT)
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build();
      return retrofit.create(MediumService.class);
    }
  }
}
