package io.sweers.catchup.data;

import android.content.Context;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import io.sweers.catchup.injection.qualifiers.ApplicationContext;
import okhttp3.OkHttpClient;

public final class DebugDataModule extends DataModule {
  @Override
  protected void configureOkHttpClientForVariant(
      @ApplicationContext Context context,
      OkHttpClient.Builder builder) {
    builder.addNetworkInterceptor(new StethoInterceptor());

    // TODO Pref this
    builder.addInterceptor(new MockDataInterceptor(context));
  }
}
