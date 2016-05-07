package io.sweers.catchup.data.reddit.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import timber.log.Timber;

// TODO Make this Moshi-friendly when Moshi can do this.
public class RedditObjectDeserializer implements JsonDeserializer<RedditObject> {
  @Override
  public RedditObject deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    if (!json.isJsonObject()) {
      // if there are no replies, we're given a String rather than an object
      return null;
    }
    try {
      RedditObjectWrapper wrapper = context.deserialize(json, RedditObjectWrapper.class);
      return context.deserialize(wrapper.getData(), wrapper.getKind().getDerivedClass());
    } catch (JsonParseException e) {
      Timber.e(e, "Failed to deserialize RedditObject");
      return null;
    }
  }
}
