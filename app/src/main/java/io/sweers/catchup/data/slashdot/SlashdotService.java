package io.sweers.catchup.data.slashdot;

import io.reactivex.Single;
import retrofit2.http.GET;

public interface SlashdotService {
  String HOST = "rss.slashdot.org";
  String ENDPOINT = "http://" + HOST;

  @GET("/Slashdot/slashdotMainatom")
  Single<Feed> main();
}
