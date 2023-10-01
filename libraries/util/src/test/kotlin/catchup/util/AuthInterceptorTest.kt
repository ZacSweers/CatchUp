/*
 * Copyright (C) 2020. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package catchup.util

import catchup.util.network.AuthInterceptor
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test

class AuthInterceptorTest {
  @Ignore("Disabled for now for IR https://github.com/ZacSweers/redacted-compiler-plugin/issues/22")
  @Test
  fun verifyRedacted() {
    val authInterceptor = AuthInterceptor("get", "token")
    assertThat(authInterceptor.toString()).isEqualTo("AuthInterceptor(method=get, accessToken=██)")
  }
}
