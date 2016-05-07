package io.sweers.catchup.data.reddit;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import io.sweers.catchup.data.reddit.model.RedditAccount;
import io.sweers.catchup.data.reddit.model.RedditComment;
import io.sweers.catchup.data.reddit.model.RedditLink;
import io.sweers.catchup.data.reddit.model.RedditListing;
import io.sweers.catchup.data.reddit.model.RedditMore;
import io.sweers.catchup.data.reddit.model.RedditResponse;

@SuppressWarnings("unchecked")
public class AutoValueTypeAdapterFactory implements TypeAdapterFactory {
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<? super T> rawType = type.getRawType();
    if (rawType.equals(RedditAccount.class)) {
      return (TypeAdapter<T>) RedditAccount.typeAdapter(gson);
    } else if (rawType.equals(RedditComment.class)) {
      return (TypeAdapter<T>) RedditComment.typeAdapter(gson);
    } else if (rawType.equals(RedditLink.class)) {
      return (TypeAdapter<T>) RedditLink.typeAdapter(gson);
    } else if (rawType.equals(RedditMore.class)) {
      return (TypeAdapter<T>) RedditMore.typeAdapter(gson);
    } else if (rawType.equals(RedditResponse.class)) {
      return (TypeAdapter<T>) RedditResponse.typeAdapter(gson);
    } else if (rawType.equals(RedditListing.class)) {
      return (TypeAdapter<T>) RedditListing.typeAdapter(gson);
    }
    return null;
  }
}
