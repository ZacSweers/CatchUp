package catchup.service.api

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant

// TODO
//  sort?
@Immutable
sealed interface Detail {
  val id: String
  val itemId: Long
  val title: String
  val text: String?
  val imageUrl: String?
  val score: Int?
  val commentsCount: Int?
  val linkUrl: String?

  data class Shallow(
    override val id: String,
    override val itemId: Long,
    override val title: String,
    override val text: String? = null,
    override val imageUrl: String? = null,
    override val score: Int? = null,
    override val commentsCount: Int? = null,
    override val linkUrl: String? = null,
  ) : Detail

  data class Full(
    override val id: String,
    override val itemId: Long,
    override val title: String,
    override val text: String? = null,
    override val imageUrl: String? = null,
    override val score: Int? = null,
    override val commentsCount: Int? = null,
    override val linkUrl: String? = null,
    val url: String,
    val comments: ImmutableList<Comment> = persistentListOf(),
  ) : Detail
}

@Immutable
data class Comment(
  val id: String,
  val serviceId: String,
  val author: String,
  val timestamp: Instant,
  val text: String,
  val score: Int,
  val children: List<Comment>,
  val depth: Int = 0,
  val clickableUrls: List<ClickableUrl>,
)

@Immutable
data class ClickableUrl(val text: String, val url: String, val previewUrl: String?)