/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data.dribbble;

import io.reactivex.Single;
import io.sweers.catchup.data.dribbble.model.Shot;
import java.util.List;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Dribbble API - http://developer.dribbble.com/v1/
 */
public interface DribbbleService {

  String HOST = "api.dribbble.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/v1/shots")
  Single<List<Shot>> getPopular(
      @Query("page") int page,
      @Query("per_page") int pageSize);

}
