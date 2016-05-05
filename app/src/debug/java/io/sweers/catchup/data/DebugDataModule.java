package io.sweers.catchup.data;

import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.OkHttpClient;

public final class DebugDataModule extends DataModule {
  @Override protected void configureOkHttpClientForVariant(OkHttpClient.Builder builder) {
    builder.addNetworkInterceptor(new StethoInterceptor());
  }
}
