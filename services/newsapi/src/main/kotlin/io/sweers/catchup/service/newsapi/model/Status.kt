package io.sweers.catchup.service.newsapi.model

import com.squareup.moshi.Json

internal enum class Status {
  @Json(name = "ok")
  OK,
  @Json(name = "error")
  ERROR
}
