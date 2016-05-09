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

package io.sweers.catchup.data;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A {@see RequestInterceptor} that adds an auth token to requests
 */
@AutoValue
public abstract class AuthInterceptor implements Interceptor {

  public static AuthInterceptor create(@NonNull String method, @NonNull String accessToken) {
    return new AutoValue_AuthInterceptor(accessToken, method);
  }

  @NonNull
  @Redacted
  public abstract String accessToken();

  @NonNull
  public abstract String method();

  @Override
  public Response intercept(Chain chain) throws IOException {
    final Request request = chain.request().newBuilder()
        .addHeader("Authorization", method() + " " + accessToken())
        .build();
    return chain.proceed(request);
  }
}
