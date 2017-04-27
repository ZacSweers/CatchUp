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

import java.util.List;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Root;

@NamespaceList({
                   @Namespace(reference = "http://www.w3.org/2005/Atom",
                              prefix = "atom")
               })
@Root(strict = false)
public class Feed {

  @ElementList(name = "item",
               inline = true) public List<Entry> itemList;

  @Element(name = "title",
           required = false) public String title;

  @Element(name = "language",
           required = false) public String language;

  @Element(name = "updated",
           required = false) public String updated;
}
