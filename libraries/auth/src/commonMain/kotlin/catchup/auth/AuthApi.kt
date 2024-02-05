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
