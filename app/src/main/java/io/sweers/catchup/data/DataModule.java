package io.sweers.catchup.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.squareup.moshi.ArrayCollectionJsonAdapter;
import com.squareup.moshi.ArrayMapJsonAdapter;
import com.squareup.moshi.Moshi;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.data.adapters.UnescapeJsonAdapter;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import io.sweers.catchup.injection.qualifiers.NetworkInterceptor;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

@Module
public abstract class DataModule {

  private static final Interceptor NOOP_INTERCEPTOR = chain -> chain.proceed(chain.request());
  private static final long HTTP_RESPONSE_CACHE = 10 * 1024 * 1024;
  private static final int HTTP_TIMEOUT_S = 30;

  @Provides static Cache provideCache(@ApplicationContext Context context) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException("Cache initialized on main thread.");
    }
    return new Cache(context.getCacheDir(), HTTP_RESPONSE_CACHE);
  }

  @Provides static OkHttpClient provideOkHttpClient(Cache cache,
      Set<Interceptor> interceptors,
      @NetworkInterceptor Set<Interceptor> networkInterceptors) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException("HTTP client initialized on main thread.");
    }

    OkHttpClient.Builder builder =
        new OkHttpClient.Builder().connectTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .cache(cache);

    builder.networkInterceptors()
        .addAll(networkInterceptors);
    builder.interceptors()
        .addAll(interceptors);

    return builder.build();
  }

  protected void configureOkHttpClientForVariant(@ApplicationContext Context context,
      OkHttpClient.Builder builder) {
    // Override in variants
  }

  @Provides static Moshi provideMoshi() {
    return new Moshi.Builder().add(AutoValueMoshiAdapterFactory.create())
        .add(UnescapeJsonAdapter.FACTORY)
        .add(ArrayMapJsonAdapter.FACTORY)
        .add(ArrayCollectionJsonAdapter.FACTORY)
        .build();
  }

  @Provides static RxJava2CallAdapterFactory provideRxJavaCallAdapterFactory() {
    return RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io());
  }

  @Provides
  public static SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
    return context.getSharedPreferences("catchup", Context.MODE_PRIVATE);
  }

  @Provides
  public static RxSharedPreferences provideRxSharedPreferences(SharedPreferences sharedPreferences) {
    return RxSharedPreferences.create(sharedPreferences);
  }

  /**
   * Stub to force multibindings to at least one element, which allows for potentially empty variant
   * provides
   */
  @Provides @NetworkInterceptor @IntoSet static Interceptor provideStubNetworkInterceptor() {
    return NOOP_INTERCEPTOR;
  }

  /**
   * Stub to force multibindings to at least one element, which allows for potentially empty variant
   * provides
   */
  @Provides @IntoSet static Interceptor provideStubInterceptor() {
    return NOOP_INTERCEPTOR;
  }
}
