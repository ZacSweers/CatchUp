package io.sweers.catchup.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;

import com.squareup.moshi.Moshi;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.R;
import io.sweers.catchup.data.UtcDateJsonAdapter;
import io.sweers.catchup.data.medium.MediumService;
import io.sweers.catchup.data.medium.model.Collection;
import io.sweers.catchup.data.medium.model.MediumPost;
import io.sweers.catchup.data.medium.model.MediumResponse;
import io.sweers.catchup.data.medium.model.Payload;
import io.sweers.catchup.injection.API;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BasicNewsController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;


public final class MediumController extends BasicNewsController<MediumPost> {

  @Inject MediumService service;
  @Inject CustomTabActivityHelper customTab;

  public MediumController() {
    this(null);
  }

  public MediumController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerMediumController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Medium);
  }

  @Override
  protected void bindItemView(@NonNull BasicNewsController<MediumPost>.ViewHolder holder, @NonNull View view, @NonNull MediumPost item) {
    holder.title(item.post().title());

    holder.score(String.format(Locale.getDefault(), "♥ %d", item.post().virtuals().recommends()));
    holder.timestamp(item.post().createdAt());

    holder.author(item.user().name());

    Collection collection = item.collection();
    if (collection != null) {
      holder.source(collection.name());
    } else {
      holder.source(null);
    }

    holder.comments(item.post().virtuals().responsesCreatedCount());
  }

  @Override
  protected void onItemClick(@NonNull BasicNewsController<MediumPost>.ViewHolder holder, @NonNull View view, @NonNull MediumPost item) {
    // TODO construct URL. domain + uniqueSlug
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.constructUrl()));
    startActivity(intent);

    // TODO This should be how it's actually done
//      customTab.openCustomTab(customTab.getCustomTabIntent()
//        .setToolbarColor(getServiceThemeColor())
//        .build(),
//          Uri.parse(item.constructUrl()));
  }

  @Override
  protected void onCommentClick(@NonNull BasicNewsController<MediumPost>.ViewHolder holder, @NonNull View view, @NonNull MediumPost item) {
    // TODO Make the app choice a pref
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.constructCommentsUrl()));
    startActivity(intent);

    // TODO This should be how it's actually done
//      customTab.openCustomTab(customTab.getCustomTabIntent()
//        .setToolbarColor(getServiceThemeColor())
//        .build(),
//          Uri.parse(item.constructCommentsUrl()));
  }

  @NonNull @Override protected Observable<List<MediumPost>> getDataObservable() {
    return service.top()
        .map(MediumResponse::payload)
        .map(Payload::references)
        // TODO why doesn't this work? Only emits once
//        .flatMap(references -> {
//          return Observable.combineLatest(
//              Observable.from(references.Post().values()),
//              Observable.just(references.User()),
//              Observable.just(references.Collection()),
//              (post, userMap, collectionMap) -> {
//                return MediumPost.builder()
//                    .post(post)
//                    .user(userMap.get(post.creatorId()))
//                    .collection(collectionMap.get(post.homeCollectionId()))
//                    .build();
//              }
//          );
//        })
        .flatMap(references -> Observable.from(references.Post().values())
            .map(post -> MediumPost.builder()
                .post(post)
                .user(references.User().get(post.creatorId()))
                .collection(references.Collection().get(post.homeCollectionId()))
                .build()))
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
  public static class Module {

    @Provides
    @PerController
    @API
    OkHttpClient provideMediumOkHttpClient(OkHttpClient client) {
      return client.newBuilder()
          .addInterceptor(chain -> {
            Request request1 = chain.request();
            Response response = chain.proceed(request1);
            BufferedSource source = response.body().source();
            source.skip(source.indexOf((byte) '{'));
            return response;
          })
          .build();
    }

    @Provides
    @PerController
    @API
    Moshi provideMediumMoshi(Moshi moshi) {
      return moshi.newBuilder()
          .add(Date.class, new UtcDateJsonAdapter())
          .build();
    }

    @Provides
    @PerController
    MediumService provideMediumService(
        @API final Lazy<OkHttpClient> client,
        @API Moshi moshi,
        RxJavaCallAdapterFactory rxJavaCallAdapterFactory) {
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
