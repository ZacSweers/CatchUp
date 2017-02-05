package io.sweers.catchup.data.medium;

import com.serjltt.moshi.adapters.Wrapped;
import io.reactivex.Observable;
import io.sweers.catchup.data.medium.model.References;
import retrofit2.http.GET;

public interface MediumService {

  String HOST = "medium.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/browse/top")
  @Wrapped({"payload", "references"})
  Observable<References> top();
}
