package io.sweers.catchup.skunkworks.benchmark.moshiKotlinReflective

import com.squareup.moshi.Json

data class KRResponse(
    val users: List<KRUser>,
    val status: String,
    @Json(name = "is_real_json")
    val isRealJson: Boolean
)
