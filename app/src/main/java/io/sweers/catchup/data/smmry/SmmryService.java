package io.sweers.catchup.data.smmry;

import io.reactivex.Single;
import io.sweers.catchup.data.smmry.model.SmmryResponse;
import java.util.Map;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface SmmryService {

  String HOST = "api.smmry.com/";
  String ENDPOINT = "http://" + HOST;

  @POST(".") Single<SmmryResponse> summarizeUrl(@QueryMap Map<String, Object> params);

  @POST(".") @FormUrlEncoded Single<SmmryResponse> summarizeText(
      @QueryMap Map<String, Object> params, @Field("sm_api_input") String text);
}
