package catchup.auth

import catchup.auth.TokenManager.AuthType.FORM_URL_ENCODED
import catchup.auth.TokenManager.AuthType.JSON
import catchup.auth.TokenManager.Credentials
import com.slack.eithernet.ApiResult
import com.slack.eithernet.ApiResultCallAdapterFactory
import com.slack.eithernet.ApiResultConverterFactory
import com.squareup.moshi.Moshi
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okhttp3.Call
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

internal suspend fun TokenStorage.updateAuthData(
  response: AuthenticationResponse,
  clock: Clock = Clock.System,
) {
  val expiration =
    response.expiresIn?.let { clock.now().plus(it.seconds) } ?: Instant.DISTANT_FUTURE
  updateAuthData(
    AuthData(tokenType = response.tokenType, expiration = expiration, token = response.accessToken)
  )
}

interface TokenManager {
  /** Authenticates a [request]. */
  suspend fun authenticate(request: Request): Request

  enum class AuthType {
    FORM_URL_ENCODED,
    JSON,
  }

  data class Credentials(
    val clientId: String,
    val secret: String,
    val grantType: String = "client_credentials",
    val authType: AuthType,
  )

  companion object {
    fun create(
      tokenStorage: TokenStorage,
      baseUrl: String,
      authEndpoint: String,
      callFactory: Call.Factory,
      moshi: Moshi,
      credentials: Credentials,
      clock: Clock = Clock.System,
    ): TokenManager {
      val api: AuthApi =
        Retrofit.Builder()
          .baseUrl(baseUrl)
          .callFactory(callFactory)
          .addConverterFactory(ApiResultConverterFactory)
          .addCallAdapterFactory(ApiResultCallAdapterFactory)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create()
      return TokenManagerImpl(authEndpoint, api, tokenStorage, credentials, clock)
    }
  }
}

internal class TokenManagerImpl(
  private val authEndpoint: String,
  private val api: AuthApi,
  private val tokenStorage: TokenStorage,
  private val credentials: Credentials,
  private val clock: Clock,
) : TokenManager {
  private val mutex = Mutex()
  private val authFunction =
    when (credentials.authType) {
      FORM_URL_ENCODED ->
        suspend {
          api.authenticateFormEncoded(
            authEndpoint,
            credentials.clientId,
            credentials.secret,
            credentials.grantType,
          )
        }
      JSON ->
        suspend {
          api.authenticateJSON(
            authEndpoint,
            AuthRequest(credentials.clientId, credentials.secret, credentials.grantType),
          )
        }
    }

  override suspend fun authenticate(request: Request): Request {
    return authenticate(request, false)
  }

  private suspend fun authenticate(request: Request, isAfterRefresh: Boolean): Request {
    println("INFO: Authenticating request ${request.url}")
    val newBuilder = request.newBuilder()
    val (tokenType, expiration, token) =
      tokenStorage.getAuthData()
        ?: run {
          refreshToken()
          return authenticate(request, isAfterRefresh)
        }
    if (clock.now() > expiration) {
      check(!isAfterRefresh)
      refreshToken()
      return authenticate(request, isAfterRefresh)
    } else {
      newBuilder.addHeader("Authorization", "$tokenType $token")
    }

    return newBuilder.build()
  }

  private suspend fun refreshToken() =
    mutex.withLock {
      println("INFO: Refreshing token")

      when (val result = authFunction()) {
        is ApiResult.Success -> tokenStorage.updateAuthData(result.value)
        is ApiResult.Failure -> {
          // TODO this will infinite loop!
          println("ERROR: Failed to refresh token: $result")
        }
      }
    }
}
