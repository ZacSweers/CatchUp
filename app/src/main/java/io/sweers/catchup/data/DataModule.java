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
import io.reactivex.schedulers.Schedulers;
import io.sweers.catchup.data.adapters.UnescapeJsonAdapter;
import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

@Module
public class DataModule {

  private static final long HTTP_RESPONSE_CACHE = 10 * 1024 * 1024;
  private static final int HTTP_TIMEOUT_S = 30;

  @Provides @Singleton static Cache provideCache(@ApplicationContext Context context) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException("Cache initialized on main thread.");
    }
    return new Cache(context.getCacheDir(), HTTP_RESPONSE_CACHE);
  }

  @Provides @Singleton OkHttpClient provideOkHttpClient(@ApplicationContext Context context,
      Cache cache) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException("HTTP client initialized on main thread.");
    }

    OkHttpClient.Builder builder =
        new OkHttpClient.Builder().connectTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_S, TimeUnit.SECONDS)
            .cache(cache);

    configureOkHttpClientForVariant(context, builder);
    return builder.build();
  }

  protected void configureOkHttpClientForVariant(@ApplicationContext Context context,
      OkHttpClient.Builder builder) {
    // Override in variants
  }

  @Provides @Singleton static Moshi provideMoshi() {
    return new Moshi.Builder().add(AutoValueMoshiAdapterFactory.create())
        .add(UnescapeJsonAdapter.FACTORY)
        .add(ArrayMapJsonAdapter.FACTORY)
        .add(ArrayCollectionJsonAdapter.FACTORY)
        .build();
  }

  @Provides @Singleton static RxJava2CallAdapterFactory provideRxJavaCallAdapterFactory() {
    return RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io());
  }

  @Provides @Singleton
  public static SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
    return context.getSharedPreferences("catchup", Context.MODE_PRIVATE);
  }

  @Provides @Singleton
  public static RxSharedPreferences provideRxSharedPreferences(SharedPreferences sharedPreferences) {
    return RxSharedPreferences.create(sharedPreferences);
  }
}
