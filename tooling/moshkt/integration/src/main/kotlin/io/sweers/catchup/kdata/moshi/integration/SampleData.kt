package io.sweers.catchup.kdata.moshi.integration

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import io.sweers.catchup.kdata.moshi.api.GenerateJsonAdapter

@GenerateJsonAdapter
data class Post(
    @Json(name = "comments_count") val commentsCount: Int,
    val title: String,
    val description: String?,
    val likesRatio: Float,
    val followers: List<String>
) {
  companion object
}

class SampleData {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val post = Post(2, "Hi!", "Such description", 0.5f, emptyList())
      val moshi = Moshi.Builder()
          .add { type, _, moshi ->
            if (type == Post::class.java) {
              Post.jsonAdapter(moshi)
            }
            null
          }
          .build()

      val json = moshi.adapter(Post::class.java).toJson(post)
      println(json)
    }
  }
}
