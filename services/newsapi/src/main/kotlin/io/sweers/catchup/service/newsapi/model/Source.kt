package io.sweers.catchup.service.newsapi.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Source(
    val id: String?,
    val name: String
)
