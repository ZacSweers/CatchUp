package io.sweers.catchup.skunkworks.benchmark.kotlinx_serialization

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@ImplicitReflectionSerializer
@Serializable
class Response {

    var users: List<User>? = null

    var status: String? = null

    @SerialName("is_real_json")
    @SerializedName("is_real_json") // Annotation needed for GSON
    @Json(name = "is_real_json")
    var isRealJson: Boolean = false

    fun stringify(serializer: KSerializer<Response>): String {
        return kotlinx.serialization.json.Json.stringify(serializer, this)
    }

    companion object {
        @JvmStatic
        fun parse(serializer: KSerializer<Response>, str: String): Response {
            return kotlinx.serialization.json.Json.parse(serializer, str)
        }

        @JvmStatic
        fun underlyingSerializer(): KSerializer<Response> {
            return serializer()
        }
    }
}
