package io.sweers.catchup.data.medium;

import io.reactivex.Maybe;
import io.sweers.catchup.data.medium.model.MediumResponse;
import retrofit2.http.GET;

public interface MediumService {

  String HOST = "medium.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/browse/top")
  Maybe<MediumResponse> top();
}
