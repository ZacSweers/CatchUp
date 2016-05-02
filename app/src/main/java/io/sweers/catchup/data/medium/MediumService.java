package io.sweers.catchup.data.medium;

import io.sweers.catchup.data.medium.model.MediumResponse;
import retrofit2.http.GET;
import rx.Observable;

public interface MediumService {

  String ENDPOINT = "https://medium.com/";

  @GET("browse/top?format=json")
  Observable<MediumResponse> top();
}
