package io.sweers.catchup.skunkworks.benchmark.moshiKotlinReflective

import com.squareup.moshi.Json

data class KRUser(
    @Json(name = "_id")
    val id: String,
    val index: Int,
    val guid: String,
    @Json(name = "is_active")
    val isActive: Boolean,
    val balance: String,
    @Json(name = "picture")
    val pictureUrl: String,
    val age: Int,
    val name: KRName,
    val company: String,
    val email: String,
    val address: String,
    val about: String,
    val registered: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>,
    val range: List<Int>,
    val friends: List<KRFriend>,
    val images: List<KRImage>,
    val greeting: String,
    @Json(name = "favorite_fruit")
    val favoriteFruit: String,
    @Json(name = "eye_color")
    val eyeColor: String,
    val phone: String
)
