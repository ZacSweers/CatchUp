/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.auth

import androidx.annotation.Keep
import com.slack.eithernet.ApiResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

@Keep
internal interface AuthApi {
  @FormUrlEncoded
  @POST
  suspend fun authenticateFormEncoded(
    @Url url: String,
    @Field("client_id") clientId: String,
    @Field("client_secret") secret: String,
    @Field("grant_type") grantType: String = "client_credentials",
  ): ApiResult<AuthenticationResponse, Unit>

  @POST
  suspend fun authenticateJSON(
    @Url url: String,
    @Body request: AuthRequest,
  ): ApiResult<AuthenticationResponse, Unit>
}

@JsonClass(generateAdapter = true)
internal data class AuthRequest(
  @Json(name = "client_id") val clientId: String,
  @Json(name = "client_secret") val secret: String,
  @Json(name = "grant_type") val grantType: String = "client_credentials",
)

@JsonClass(generateAdapter = true)
internal data class AuthenticationResponse(
  @Json(name = "token_type") val tokenType: String,
  @Json(name = "expires_in") val expiresIn: Long? = null,
  @Json(name = "access_token") val accessToken: String,
)
