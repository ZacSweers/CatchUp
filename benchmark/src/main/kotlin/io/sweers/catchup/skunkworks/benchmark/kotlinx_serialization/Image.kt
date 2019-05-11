package io.sweers.catchup.skunkworks.benchmark.kotlinx_serialization

import kotlinx.serialization.Serializable

@Serializable
class Image {

    var id: String? = null

    var format: String? = null

    var url: String? = null

    var description: String? = null

}
