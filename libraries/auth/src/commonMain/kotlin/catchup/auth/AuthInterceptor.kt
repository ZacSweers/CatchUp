package catchup.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
  override fun intercept(chain: Chain): Response {
    // TODO check for 401s and re-auth?
    val request = runBlocking { tokenManager.authenticate(chain.request()) }
    return chain.proceed(request)
  }
}
