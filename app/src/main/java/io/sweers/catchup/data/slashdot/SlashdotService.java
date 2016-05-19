package io.sweers.catchup.data.slashdot;

import retrofit2.http.GET;
import rx.Observable;

public interface SlashdotService {
  String HOST = "rss.slashdot.org";
  String ENDPOINT = "http://" + HOST;

  @GET("/Slashdot/slashdotMainatom")
  Observable<Feed> main();
}
