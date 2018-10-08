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

package in.uncod.android.bypass;

import java.util.HashMap;
import java.util.Map;

public class Element {

  public static final int F_LIST_ORDERED = 1;
  String text;
  Map<String, String> attributes = new HashMap<String, String>();
  Element[] children;
  Type type;
  Element parent;
  int nestLevel = 0;

  public Element(String text, int type) {
    this.text = text;
    this.type = Type.fromInteger(type);
  }

  public void setChildren(Element[] children) {
    this.children = children;
  }

  public void addAttribute(String name, String value) {
    attributes.put(name, value);
  }

  public String getAttribute(String name) {
    return attributes.get(name);
  }

  public Element getParent() {
    return parent;
  }

  public void setParent(Element element) {
    this.parent = element;
  }

  public String getText() {
    return text;
  }

  public int size() {
    if (children != null) {
      return children.length;
    }
    return 0;
  }

  public Type getType() {
    return type;
  }

  public boolean isBlockElement() {
    return (type.value & 0x100) == 0x000;
  }

  public boolean isSpanElement() {
    return (type.value & 0x100) == 0x100;
  }

  public enum Type {

    // Block Element Types

    BLOCK_CODE(0x000), BLOCK_QUOTE(0x001), BLOCK_HTML(0x002), HEADER(0x003), HRULE(0x004), LIST(
        0x005), LIST_ITEM(0x006), PARAGRAPH(0x007), TABLE(0x008), TABLE_CELL(0x009), TABLE_ROW(0x00A),

    // Span Element Types

    AUTOLINK(0x10B), CODE_SPAN(0x10C), DOUBLE_EMPHASIS(0x10D), EMPHASIS(0x10E), IMAGE(0x10F), LINEBREAK(
        0x110), LINK(0x111), RAW_HTML_TAG(0x112), TRIPLE_EMPHASIS(0x113), TEXT(0x114), STRIKETHROUGH(
        0x115);

    private static final Type[] TypeValues = Type.values();
    private final int value;

    private Type(int value) {
      this.value = value;
    }

    public static Type fromInteger(int x) {
      for (Type type : TypeValues) {
        if (type.value == x) {
          return type;
        }
      }
      return null;
    }
  }
}
