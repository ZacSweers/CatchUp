package io.sweers.catchup.service.reddit.model

import com.squareup.moshi.Json
import io.sweers.moshkt.api.MoshiSerializable

@MoshiSerializable
data class Foo(
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val age: Int,
    val nationalities: List<String> = emptyList(),
    val weight: Float,
    val tattoos: Boolean = false,
    val race: String?,
    val hasChildren: Boolean = false,
    val favoriteFood: String? = null,
    val favoriteDrink: String? = "Water",
    val wildcardOut: List<out String> = emptyList(),
    val wildcardIn: Array<in String>,
    val any: List<*>,
    val anyTwo: List<Any>,
    val anyOut: List<out Any>,
    val favoriteThreeNumbers: IntArray,
    val favoriteArrayValues: Array<String>,
    val favoriteNullableArrayValues: Array<String?>,
    val nullableSetListMapArrayNullableIntWithDefault: Set<List<Map<String, Array<IntArray?>>>>? = null
)
