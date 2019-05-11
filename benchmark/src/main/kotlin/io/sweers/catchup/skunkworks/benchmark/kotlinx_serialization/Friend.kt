package io.sweers.catchup.skunkworks.benchmark.kotlinx_serialization

import kotlinx.serialization.Serializable

@Serializable
class Friend {

    var id: Int = 0

    var name: String? = null
}
