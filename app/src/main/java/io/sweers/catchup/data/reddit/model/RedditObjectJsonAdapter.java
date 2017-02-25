package io.sweers.catchup.data.reddit.model;

import android.support.annotation.Nullable;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class RedditObjectJsonAdapter extends JsonAdapter<Object> {
  public static final JsonAdapter.Factory FACTORY = new Factory() {
    @Nullable @Override
    public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
      Class<?> clazz = Types.getRawType(type);
      if (!RedditObject.class.equals(clazz)) {
        return null;
      }
      JsonAdapter<RedditComment> commentAdapter =
          moshi.nextAdapter(this, RedditComment.class, Collections.emptySet());
      JsonAdapter<RedditLink> linkAdapter =
          moshi.nextAdapter(this, RedditLink.class, Collections.emptySet());
      JsonAdapter<RedditListing> listingAdapter =
          moshi.nextAdapter(this, RedditListing.class, Collections.emptySet());
      JsonAdapter<RedditMore> moreAdapter =
          moshi.nextAdapter(this, RedditMore.class, Collections.emptySet());
      return new RedditObjectJsonAdapter(commentAdapter, linkAdapter, listingAdapter, moreAdapter);
    }
  };

  private final JsonAdapter<RedditComment> commentAdapter;
  private final JsonAdapter<RedditLink> linkAdapter;
  private final JsonAdapter<RedditListing> listingAdapter;
  private final JsonAdapter<RedditMore> moreAdapter;

  private RedditObjectJsonAdapter(JsonAdapter<RedditComment> commentAdapter,
      JsonAdapter<RedditLink> linkAdapter,
      JsonAdapter<RedditListing> listingAdapter,
      JsonAdapter<RedditMore> moreAdapter) {
    this.commentAdapter = commentAdapter;
    this.linkAdapter = linkAdapter;
    this.listingAdapter = listingAdapter;
    this.moreAdapter = moreAdapter;
  }

  @Override public Object fromJson(JsonReader reader) throws IOException {
    Object jsonValue = reader.readJsonValue();
    if (jsonValue instanceof String) {
      // There are no replies.
      return jsonValue; // Or null, or something interesting to you.
    }
    // noinspection unchecked
    Map<String, Object> value = (Map<String, Object>) jsonValue;
    RedditType type = RedditType.valueOf(((String) value.get("kind")).toUpperCase());
    Object redditObject = value.get("data");
    switch (type) {
      case T1:
        return commentAdapter.fromJsonValue(redditObject);
      case T3:
        return linkAdapter.fromJsonValue(redditObject);
      case LISTING:
        return listingAdapter.fromJsonValue(redditObject);
      case MORE:
        return moreAdapter.fromJsonValue(redditObject);
      default:
        throw new JsonDataException();
    }
  }

  @Override public void toJson(JsonWriter writer, Object value) throws IOException {
    throw new UnsupportedOperationException("TODO");
    // Check the runtime type of value and delegate.
  }
}
