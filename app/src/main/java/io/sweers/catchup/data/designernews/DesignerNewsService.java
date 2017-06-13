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

package io.sweers.catchup.data.designernews;

import com.serjltt.moshi.adapters.Wrapped;
import io.reactivex.Single;
import io.sweers.catchup.data.designernews.model.Story;
import io.sweers.catchup.data.designernews.model.User;
import io.sweers.catchup.util.collect.CommaJoinerList;
import java.util.List;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Models the Designer News API.
 * <p>
 * v2 docs: https://github.com/DesignerNews/dn_api_v2
 */
public interface DesignerNewsService {

  String HOST = "www.designernews.co/api/v2/";
  String ENDPOINT = "https://" + HOST;

  @GET("stories") @Wrapped(path = "stories") Single<List<Story>> getTopStories(@Query("page") int page);

  @GET("users/{ids}") @Wrapped(path = "users") Single<List<User>> getUsers(
      @Path("ids") CommaJoinerList<String> ids);
}
