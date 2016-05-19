package io.sweers.catchup.data;

import android.content.Context;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

public final class DebugDataModule extends DataModule {
  @Override
  protected void configureOkHttpClientForVariant(
      @ApplicationContext Context context,
      OkHttpClient.Builder builder) {
    builder.addNetworkInterceptor(new StethoInterceptor());

    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Timber.tag("OkHttp").v(message));
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
    builder.addNetworkInterceptor(loggingInterceptor);

    builder.addInterceptor(new MockDataInterceptor(context));
  }
}
