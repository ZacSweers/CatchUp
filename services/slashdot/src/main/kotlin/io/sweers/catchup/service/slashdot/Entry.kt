/*
 * Copyright (c) 2018 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.service.slashdot

import androidx.annotation.Keep
import com.tickaroo.tikxml.TypeConverter
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import com.tickaroo.tikxml.converter.htmlescape.HtmlEscapeStringConverter
import io.sweers.catchup.service.api.HasStableId
import io.sweers.catchup.util.parsePossiblyOffsetInstant
import org.threeten.bp.Instant

private const val SLASH_PREFIX = "slash:"

@Keep
@Xml
internal data class Entry(
    @PropertyElement(converter = HtmlEscapeStringConverter::class)
    val title: String,
    @PropertyElement
    val id: String,
    @PropertyElement
    val link: String,
    @PropertyElement(converter = HtmlEscapeStringConverter::class)
    val summary: String,
    @PropertyElement(converter = InstantTypeConverter::class)
    val updated: Instant,
    @PropertyElement(name = "${SLASH_PREFIX}section")
    val section: String,
    @PropertyElement(name = "${SLASH_PREFIX}comments")
    val comments: Int = 0,
    @Element
    val author: Author,
    @PropertyElement(name = "${SLASH_PREFIX}department")
    val department: String
) : HasStableId {
  override fun stableId(): Long = id.hashCode().toLong()
}

internal class InstantTypeConverter : TypeConverter<Instant> {
  override fun write(value: Instant): String = TODO("Unsupported")

  override fun read(value: String) = value.parsePossiblyOffsetInstant()
}
