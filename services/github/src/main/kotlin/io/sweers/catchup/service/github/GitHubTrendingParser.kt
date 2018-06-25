/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.github

import io.sweers.catchup.service.github.GitHubApi.Companion.ENDPOINT
import io.sweers.catchup.service.github.model.TrendingItem
import io.sweers.catchup.util.d
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * GitHub API does not have /trending endpoints so we have to do gross things :(
 */
internal object GitHubTrendingParser {
  private val NUMBER_PATTERN = "\\d+".toRegex()
  private fun String.removeCommas() = replace(",", "")

  internal fun parse(body: ResponseBody): List<TrendingItem> {
    return Jsoup.parse(body.string(), ENDPOINT)
        .select(".repo-list li")
        .mapNotNull(::parseTrendingItem)
  }

  private fun parseTrendingItem(element: Element): TrendingItem? {
    // /creativetimofficial/material-dashboard
    val authorAndName = element.select("h3 > a")
        .attr("href")
        .toString()
        .removePrefix("/")
        .trimEnd()
        .split("/")
        .let { Pair(it[0], it[1]) }
    val (author, repoName) = authorAndName
    val url = "$ENDPOINT/${authorAndName.first}/${authorAndName.second}"
    val description = element.select("p").text()

    val language = element.select("[itemprop=\"programmingLanguage\"]").text()
    // "background-color:#563d7c;"
    val languageColor = element.select(".repo-language-color")
        .firstOrNull()
        ?.attr("style")
        ?.removePrefix("background-color:")
        ?.removeSuffix(";")

    // "3,441" stars, forks
    val counts = element.select(".muted-link.d-inline-block.mr-3")
        .asSequence()
        .map(Element::text)
        .map { it.removeCommas() }
        .map(String::toInt)
        .toList()

    val stars = counts[0]
    val forks = counts.getOrNull(1)

    // "691 stars today"
    val starsToday = element.select(".f6.text-gray.mt-2 > span:last-child")[0]
        .text()
        .removeCommas()
        .let {
          NUMBER_PATTERN.find(it)?.groups?.firstOrNull()?.value?.toInt() ?: run {
            d { "$authorAndName didn't have today" }
            null
          }
        }

    return TrendingItem(
        author = author,
        url = url,
        repoName = repoName,
        description = description,
        stars = stars,
        forks = forks,
        starsToday = starsToday,
        language = language,
        languageColor = languageColor
    )
  }
}

