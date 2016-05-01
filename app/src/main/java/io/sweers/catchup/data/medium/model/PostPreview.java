package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class PostPreview {

  public static JsonAdapter<PostPreview> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_PostPreview.MoshiJsonAdapter(moshi);
  }

  public abstract String postId();

  public abstract PostPreviewContent postPreviewContent();
}
