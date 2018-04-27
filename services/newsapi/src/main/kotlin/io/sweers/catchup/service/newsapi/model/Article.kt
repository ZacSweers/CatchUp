package io.sweers.catchup.service.newsapi.model

import com.squareup.moshi.JsonClass
import org.threeten.bp.Instant

@JsonClass(generateAdapter = true)
internal data class Article(
    val source: Source,
    val author: String?,
    val title: String,
    val description: String?,
    val url: String,
    val urlToImage: String?,
    val publishedAt: Instant
)
