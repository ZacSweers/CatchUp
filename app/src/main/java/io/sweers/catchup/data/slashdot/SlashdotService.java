package io.sweers.catchup.data.slashdot;

import io.reactivex.Maybe;
import retrofit2.http.GET;

public interface SlashdotService {
  String HOST = "rss.slashdot.org";
  String ENDPOINT = "http://" + HOST;

  @GET("/Slashdot/slashdotMainatom")
  Maybe<Feed> main();
}
