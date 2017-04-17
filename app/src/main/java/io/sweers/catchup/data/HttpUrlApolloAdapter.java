package io.sweers.catchup.data;

import com.apollographql.apollo.CustomTypeAdapter;
import okhttp3.HttpUrl;

/**
 * An Apollo adapter for converting between URI types to HttpUrl.
 */
public class HttpUrlApolloAdapter implements CustomTypeAdapter<HttpUrl> {
  @Override public HttpUrl decode(String s) {
    return HttpUrl.parse(s);
  }

  @Override public String encode(HttpUrl o) {
    return o.toString();
  }
}
