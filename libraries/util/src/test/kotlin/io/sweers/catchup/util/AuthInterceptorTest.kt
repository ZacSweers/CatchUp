package io.sweers.catchup.util

import com.google.common.truth.Truth.assertThat
import io.sweers.catchup.util.network.AuthInterceptor
import org.junit.Test

class AuthInterceptorTest {
  @Test
  fun verifyRedacted() {
    val authInterceptor = AuthInterceptor("get", "token")
    assertThat(authInterceptor.toString()).isEqualTo("AuthInterceptor(method=get, accessToken=██)")
  }
}