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
package catchup.service.dribbble

import catchup.service.dribbble.DribbbleApi.Companion.ENDPOINT
import catchup.service.dribbble.model.Shot
import catchup.service.dribbble.model.User
import kotlinx.datetime.Clock
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Dribbble V2 API does not have read endpoints so we have to do gross things :(
 *
 * Adapted from Plaid's search API, which this could conceivably be used for too.
 */
internal object DribbbleParser {

  fun parse(body: ResponseBody): List<Shot> {
    val shotElements = Jsoup.parse(body.string(), ENDPOINT).select("li[id^=screenshot]")
    return shotElements.mapNotNull(DribbbleParser::parseShot)
  }

  private val HttpUrl.path: String
    get() = "$scheme://$host/${pathSegments.joinToString("/")}"

  private fun parseShot(element: Element): Shot {
    val image = element.select("img").first()!!
    val altText = image.attr("alt")
    val imgUrl =
      image
        .attr("src")
        .let {
          if (it.contains("_teaser.")) {
            it.replace("_teaser.", ".")
          } else {
            it
          }
        }
        .toHttpUrl()
        .path
    // API responses wrap description in a <p> tag. Do the same for consistent display.
    var description = image.attr("alt")
    if (!description.isEmpty()) {
      description = "<p>$description</p>"
    }

    // Video URLs
    val videoUrl = element.select("video").firstOrNull()?.attr("src")

    val createdAt = Clock.System.now()

    return Shot(
      id = element.id().replace("screenshot-", "").toLong(),
      htmlUrl = "$ENDPOINT${element.select("a.dribbble-link").first()!!.attr("href")}",
      title = element.getElementsByClass("shot-title").first()!!.text(),
      description = description,
      imageUrl = imgUrl,
      imageAlt = altText,
      videoUrl = videoUrl,
      createdAt = createdAt,
      likesCount = element.getElementsByClass("js-shot-likes-count").first()!!.text().parseCount(),
      commentsCount = 0,
      viewsCount = element.getElementsByClass("js-shot-views-count").first()!!.text().parseCount(),
      user = parsePlayer(element),
    )
  }

  private fun parsePlayer(element: Element): User {
    val userInformation = element.getElementsByClass("user-information").first()!!
    val avatarUrl =
      userInformation.select("img").first()!!.attr("data-src").let {
        if ("/mini/" in it) {
          it.replace("/mini/", "/normal/")
        } else {
          it
        }
      }
    val username =
      userInformation.getElementsByClass("hoverable url").first()?.attr("href")?.removePrefix("/")
    val id = avatarUrl.substringAfter("users/").substringBefore("/").toLong()
    val name = userInformation.getElementsByClass("display-name").first()?.text()
    return User(
      id = id,
      name = name,
      username = username,
      htmlUrl = "$ENDPOINT/$username",
      avatarUrl = avatarUrl,
      pro = element.select("span.badge-pro").size > 0,
    )
  }
}

private fun String.parseCount(): Long {
  return when {
    endsWith("k") -> {
      (removeSuffix("k").toFloat() * 1000).toLong()
    }
    else -> {
      toLong()
    }
  }
}
