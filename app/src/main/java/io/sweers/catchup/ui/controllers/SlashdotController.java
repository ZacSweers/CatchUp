package io.sweers.catchup.ui.controllers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;

import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.R;
import io.sweers.catchup.data.LinkManager;
import io.sweers.catchup.data.slashdot.Entry;
import io.sweers.catchup.data.slashdot.SlashdotService;
import io.sweers.catchup.injection.qualifiers.ForApi;
import io.sweers.catchup.injection.scopes.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BaseNewsController;
import io.sweers.catchup.util.Iso8601Utils;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import rx.Observable;


public final class SlashdotController extends BaseNewsController<Entry> {

  @Inject SlashdotService service;
  @Inject LinkManager linkManager;

  public SlashdotController() {
    this(null);
  }

  public SlashdotController(Bundle args) {
    super(args);
  }

  @Override
  protected void performInjection() {
    DaggerSlashdotController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override
  protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_Slashdot);
  }

  @Override
  protected void bindItemView(@NonNull Entry entry, @NonNull ViewHolder holder) {
    holder.title(entry.title);

    holder.score(null);
    holder.timestamp(Iso8601Utils.parse(entry.updated));
    holder.author(entry.author.name);

    holder.source(entry.department);

    holder.comments(Integer.parseInt(entry.comments));
    holder.tag(entry.section);

    holder.itemClicks()
        .compose(transformUrl(entry.id))
        .subscribe(linkManager);
    holder.itemCommentClicks()
        .compose(transformUrl(entry.id + "#comments"))
        .subscribe(linkManager);
  }

  @NonNull
  @Override
  protected Observable<List<Entry>> getDataObservable() {
    return service.main()
        .map(channel -> channel.itemList);
  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(SlashdotController controller);
  }

  @dagger.Module
  public static class Module {

    @Provides
    @ForApi
    @PerController
    OkHttpClient provideSlashdotOkHttpClient(OkHttpClient okHttpClient) {
      return okHttpClient.newBuilder()
          .addNetworkInterceptor(chain -> {
            Response originalResponse = chain.proceed(chain.request());
            // read from cache for 30 minutes, per slashdot's preferred limit
            int maxAge = 60 * 30;
            return originalResponse.newBuilder()
                .header("Cache-Control", "public, max-age=" + maxAge)
                .build();
          })
          .build();
    }

    @Provides
    @PerController
    SlashdotService provideSlashdotService(
        @ForApi final Lazy<OkHttpClient> client,
        RxJavaCallAdapterFactory rxJavaCallAdapterFactory) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl(SlashdotService.ENDPOINT)
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
          .build();
      return retrofit.create(SlashdotService.class);
    }
  }
}
