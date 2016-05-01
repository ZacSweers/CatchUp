package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class StreamItem {

  public static JsonAdapter<StreamItem> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_StreamItem.MoshiJsonAdapter(moshi);
  }

  public abstract long createdAt();

  public abstract PostPreview bmPostPreview();
}
