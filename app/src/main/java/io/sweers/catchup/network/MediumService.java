package io.sweers.catchup.network;

import io.sweers.catchup.model.medium.MediumResponse;
import retrofit2.http.GET;
import rx.Observable;

public interface MediumService {

  @GET("browse/top?format=json")
  Observable<MediumResponse> top();
}
