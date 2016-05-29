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

package io.sweers.catchup.data.producthunt;

import io.sweers.catchup.data.producthunt.model.PostsResponse;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

/**
 * Models the Product Hunt API. See https://api.producthunt.com/v1/docs
 */
public interface ProductHuntService {

  String SCHEME = "https";
  String HOST = "api.producthunt.com";
  String ENDPOINT = SCHEME + "://" + HOST;

  @GET("/v1/posts")
  Observable<PostsResponse> getPosts(@Query("days_ago") int page);

  @HEAD
  Observable<Response<Void>> resolveDomain(@Url String url);
}
