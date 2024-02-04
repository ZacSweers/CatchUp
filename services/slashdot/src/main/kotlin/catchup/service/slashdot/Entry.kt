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
package catchup.service.slashdot

import catchup.util.parsePossiblyOffsetInstant
import com.tickaroo.tikxml.converter.htmlescape.StringEscapeUtils
import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

private const val SLASH_PREFIX = "slash"

@Serializable
@XmlSerialName("entry")
data class Entry(
  @XmlElement @Serializable(HtmlEscapeStringSerializer::class) val title: String,
  @XmlElement val id: String,
  val link: Link,
  @XmlElement @Serializable(HtmlEscapeStringSerializer::class) val summary: String,
  @XmlElement @Serializable(InstantSerializer::class) val updated: Instant,
  @XmlElement
  @XmlSerialName(
    "section",
    namespace = "http://purl.org/rss/1.0/modules/slash/",
    prefix = SLASH_PREFIX,
  )
  val section: String,
  @XmlElement
  @XmlSerialName(
    "comments",
    namespace = "http://purl.org/rss/1.0/modules/slash/",
    prefix = SLASH_PREFIX,
  )
  val comments: Int = 0,
  val author: Author,
  @XmlElement
  @XmlSerialName(
    "department",
    namespace = "http://purl.org/rss/1.0/modules/slash/",
    prefix = SLASH_PREFIX,
  )
  val department: String,
)

/** Nearly identical to [InstantIso8601Serializer] but handles possibly offset instants. */
internal object InstantSerializer : KSerializer<Instant> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Instant =
    decoder.decodeString().parsePossiblyOffsetInstant()

  override fun serialize(encoder: Encoder, value: Instant) {
    encoder.encodeString(value.toString())
  }
}

/**
 * A String TypeConverter that escapes and unescapes HTML characters directly from string. This one
 * uses apache 3 StringEscapeUtils borrowed from tikxml.
 */
object HtmlEscapeStringSerializer : KSerializer<String> {

  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("EscapedString", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): String {
    return StringEscapeUtils.unescapeHtml4(decoder.decodeString())
  }

  override fun serialize(encoder: Encoder, value: String) {
    encoder.encodeString(StringEscapeUtils.escapeHtml4(value))
  }
}
