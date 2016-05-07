package io.sweers.catchup.data.medium.model;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class Paragraph {
  public static JsonAdapter<Paragraph> jsonAdapter(@NonNull Moshi moshi) {
    return new AutoValue_Paragraph.MoshiJsonAdapter(moshi);
  }

  public abstract List<Markup> markups();

  public abstract String name();

  public abstract String text();

  public abstract int type();
}
