package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class PostPreviewContent {

  public static JsonAdapter<PostPreviewContent> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_PostPreviewContent.MoshiJsonAdapter(moshi);
  }

  public abstract BodyModel bodyModel();

  public abstract boolean isFullContent();
}
