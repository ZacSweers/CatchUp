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

package io.sweers.catchup.data.medium;

import com.serjltt.moshi.adapters.Wrapped;
import io.reactivex.Observable;
import io.sweers.catchup.data.medium.model.References;
import okhttp3.ResponseBody;
import retrofit2.http.GET;

public interface MediumService {

  String HOST = "medium.com";
  String ENDPOINT = "https://" + HOST;

  @GET("/browse/top") @Wrapped(path = { "payload", "references" }) Observable<References> top();

  @GET("/browse/top") Observable<ResponseBody> topRaw();
}
