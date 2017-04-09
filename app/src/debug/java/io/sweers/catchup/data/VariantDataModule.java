package io.sweers.catchup.data;

import android.content.Context;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.injection.qualifiers.NetworkInterceptor;
import okhttp3.Interceptor;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

@Module
public abstract class VariantDataModule {

  @Provides @NetworkInterceptor @IntoSet static Interceptor provideLoggingInterceptor() {
    HttpLoggingInterceptor loggingInterceptor =
        new HttpLoggingInterceptor(message -> Timber.tag("OkHttp")
            .v(message));
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
    return loggingInterceptor;
  }

  @Provides @NetworkInterceptor @IntoSet static Interceptor provideStethoInterceptor() {
    return new StethoInterceptor();
  }

  @Provides @IntoSet
  static Interceptor provideMockDataInterceptor(@ApplicationContext Context context) {
    return new MockDataInterceptor(context);
  }
}
