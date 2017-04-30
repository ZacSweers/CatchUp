/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data.reddit;

import android.support.annotation.Nullable;
import io.reactivex.Single;
import io.sweers.catchup.data.reddit.model.RedditResponse;
import java.util.List;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RedditService {
  String HOST = "www.reddit.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/r/{subreddit}/comments/{id}") Single<List<RedditResponse>> comments(
      @Path("subreddit") String subreddit, @Path("id") String id);

  @GET("/") Single<RedditResponse> frontPage(@Query("limit") int limit,
      @Query("after") @Nullable String after);

  @GET("/r/{subreddit}") Single<RedditResponse> subreddit(@Path("subreddit") String subreddit,
      @Query("after") String after,
      @Query("limit") int limit);

  @GET("/top") Single<RedditResponse> top(@Query("after") String after,
      @Query("limit") int limit);
}
