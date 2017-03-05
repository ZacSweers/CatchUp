package io.sweers.catchup.data.smmry;

import com.bluelinelabs.conductor.Controller;
import com.squareup.moshi.Moshi;
import dagger.Binds;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;
import io.sweers.catchup.injection.ControllerKey;
import io.sweers.catchup.ui.controllers.SmmryController;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

@Module(subcomponents = SmmryController.Component.class)
public abstract class SmmryModule {
  @Binds @IntoMap @ControllerKey(SmmryController.class)
  abstract AndroidInjector.Factory<? extends Controller> bindSmmryControllerInjectorFactory(
      SmmryController.Component.Builder builder);

  @Provides @Singleton static SmmryService provideSmmryService(final Lazy<OkHttpClient> client,
      Moshi moshi,
      RxJava2CallAdapterFactory rxJavaCallAdapterFactory) {
    return new Retrofit.Builder().baseUrl(SmmryService.ENDPOINT)
        .callFactory(request -> client.get()
            .newCall(request))
        .addCallAdapterFactory(rxJavaCallAdapterFactory)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(SmmryService.class);
  }
}
