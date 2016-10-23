package io.sweers.catchup.data;

import android.support.annotation.NonNull;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class AutoValueGsonTypeAdapterFactory implements TypeAdapterFactory {

  @NonNull
  public static AutoValueGsonTypeAdapterFactory create() {
    return new AutoValueGson_AutoValueGsonTypeAdapterFactory();
  }

}
