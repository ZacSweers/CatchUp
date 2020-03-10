/*
 * Copyright (C) 2019. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sweers.catchup.service.dribbble

import android.text.TextUtils
import io.sweers.catchup.service.dribbble.DribbbleApi.Companion.ENDPOINT
import io.sweers.catchup.service.dribbble.model.Images
import io.sweers.catchup.service.dribbble.model.Shot
import io.sweers.catchup.service.dribbble.model.User
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * Dribbble V2 API does not have read endpoints so we have to do gross things :(
 *
 * Adapted from Plaid's search API, which this could conceivably be used for too.
 */
internal object DribbbleParser {

  private val PATTERN_PLAYER_ID = Pattern.compile("users/(\\d+?)/", Pattern.DOTALL)
  private val DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy")

  fun parse(body: ResponseBody): List<Shot> {
    val shotElements = Jsoup.parse(body.string(), ENDPOINT)
        .select("li[id^=screenshot]")
    return shotElements.mapNotNull(::parseShot)
  }

  private fun parseShot(element: Element): Shot? {
    val descriptionBlock = element.select("a.dribbble-over")
        .first()
    // API responses wrap description in a <p> tag. Do the same for consistent display.
    var description = descriptionBlock.select("span.comment")
        .text()
        .trim { it <= ' ' }
    if (!TextUtils.isEmpty(description)) {
      description = "<p>$description</p>"
    }
    var imgUrl = element.select("img")
        .first()
        .attr("src")
    if (imgUrl.contains("_teaser.")) {
      imgUrl = imgUrl.replace("_teaser.", ".")
    }
    val createdAt: Instant = try {
      DATE_FORMAT.parse(descriptionBlock.select("em.timestamp")
          .first()
          .text(),
          Instant::from)
    } catch (e2: Exception) {
      Instant.now()
    }

    val isAnimated = imgUrl.endsWith(".gif")
    return Shot(
        id = element.id().replace("screenshot-", "").toLong(),
        htmlUrl = "$ENDPOINT${element.select("a.dribbble-link").first().attr("href")}",
        title = descriptionBlock.select("strong").first().text(),
        description = description,
        images = Images(null, if (isAnimated) imgUrl.replace("_1x", "") else imgUrl),
        animated = isAnimated,
        createdAt = createdAt,
        likesCount = element.select("li.fav")
            .first()
            .child(0)
            .text()
            .replace(",", "")
            .toLong(),
        commentsCount = element.select("li.cmnt")
            .first()
            .child(0)
            .text()
            .replace(",", "")
            .toLong(),
        viewsCount = element.select("li.views")
            .first()
            .child(0)
            .text()
            .replace(",", "")
            .toLong(),
        user = parsePlayer(element)
    )
  }

  private fun parsePlayer(element: Element): User {
    val userBlock = element.select("a.url")
        .first()
    var avatarUrl = userBlock.select("img.photo")
        .first()
        .attr("src")
    if (avatarUrl.contains("/mini/")) {
      avatarUrl = avatarUrl.replace("/mini/", "/normal/")
    }
    val matchId = PATTERN_PLAYER_ID.matcher(avatarUrl)
    var id: Long = -1L
    if (matchId.find() && matchId.groupCount() == 1) {
      matchId.group(1)?.toLong()?.let {
        id = it
      }
    }
    val slashUsername = userBlock.attr("href")
    val username = if (slashUsername.isEmpty()) null else slashUsername.substring(1)
    return User(
        id = id,
        name = userBlock.text(),
        username = username,
        htmlUrl = "$ENDPOINT$slashUsername",
        avatarUrl = avatarUrl,
        pro = element.select("span.badge-pro").size > 0
    )
  }
}
