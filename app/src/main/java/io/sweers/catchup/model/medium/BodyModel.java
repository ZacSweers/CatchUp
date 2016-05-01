package io.sweers.catchup.model.medium;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class BodyModel {
  public static JsonAdapter<BodyModel> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_BodyModel.MoshiJsonAdapter(moshi);
  }

  public abstract boolean isFullContent();

  public abstract List<Paragraph> paragraphs();

  public Paragraph getTitle() {
    return paragraphs().get(0);
  }
}
