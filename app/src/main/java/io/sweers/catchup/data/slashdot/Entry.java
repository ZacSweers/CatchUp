/*
 * Copyright (c) 2017 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sweers.catchup.data.slashdot;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Root;

import io.sweers.catchup.ui.base.HasStableId;

@NamespaceList(
    {
        @Namespace(reference = "http://purl.org/rss/1.0/modules/slash/", prefix = "slash")
    }
)
@Root(name = "entry", strict = false)
public class Entry implements HasStableId {

  @Element(name = "title", required = false)
  public String title;

  @Element(name = "id")
  public String id;

  @Element(name = "link", required = false)
  public String link;

  @Element(name = "updated", required = false)
  public String updated;

  @Element(name = "section", required = false)
  public String section;

  @Element(name = "comments", required = false)
  public int comments;

  @Element(name = "author", required = false)
  public Author author;

  @Element(name = "department", required = false)
  public String department;

  @Override
  public long stableId() {
    return id.hashCode();
  }
}
