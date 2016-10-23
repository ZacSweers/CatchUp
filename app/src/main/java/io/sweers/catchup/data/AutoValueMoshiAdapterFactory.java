package io.sweers.catchup.data;

import android.support.annotation.NonNull;

import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;
import com.squareup.moshi.JsonAdapter;

@MoshiAdapterFactory
public abstract class AutoValueMoshiAdapterFactory implements JsonAdapter.Factory {

  @NonNull
  public static AutoValueMoshiAdapterFactory create() {
    return new AutoValueMoshi_AutoValueMoshiAdapterFactory();
  }

}
