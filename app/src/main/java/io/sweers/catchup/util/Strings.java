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

package io.sweers.catchup.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import org.jetbrains.annotations.Contract;

public final class Strings {
  private Strings() {
    throw new InstantiationError();
  }

  public static String truncateAt(String string, int length) {
    return string.length() > length ? string.substring(0, length) : string;
  }

  /**
   * Unescapes a string that contains standard Java escape sequences.
   * <ul>
   * <li><strong>&#92;b &#92;f &#92;n &#92;r &#92;t &#92;" &#92;'</strong> :
   * BS, FF, NL, CR, TAB, double and single quote.</li>
   * <li><strong>&#92;X &#92;XX &#92;XXX</strong> : Octal character
   * specification (0 - 377, 0x00 - 0xFF).</li>
   * <li><strong>&#92;uXXXX</strong> : Hexadecimal based Unicode character.</li>
   * </ul>
   * <p>
   * From <a href="https://gist.github.com/uklimaschewski/6741769">Here</a>
   *
   * @param st A string optionally containing standard java escape sequences.
   * @return The translated string.
   */
  @Contract("null -> null; !null -> !null") @Nullable public static String unescapeJavaString(
      @Nullable String st) {
    if (TextUtils.isEmpty(st)) {
      return st;
    }

    StringBuilder sb = new StringBuilder(st.length());

    for (int i = 0; i < st.length(); i++) {
      char ch = st.charAt(i);
      if (ch == '\\') {
        char nextChar = (i == st.length() - 1) ? '\\' : st.charAt(i + 1);
        // Octal escape?
        if (nextChar >= '0' && nextChar <= '7') {
          String code = "" + nextChar;
          i++;
          if ((i < st.length() - 1) && st.charAt(i + 1) >= '0' && st.charAt(i + 1) <= '7') {
            code += st.charAt(i + 1);
            i++;
            if ((i < st.length() - 1) && st.charAt(i + 1) >= '0' && st.charAt(i + 1) <= '7') {
              code += st.charAt(i + 1);
              i++;
            }
          }
          sb.append((char) Integer.parseInt(code, 8));
          continue;
        }
        switch (nextChar) {
          case '\\':
            ch = '\\';
            break;
          case 'b':
            ch = '\b';
            break;
          case 'f':
            ch = '\f';
            break;
          case 'n':
            ch = '\n';
            break;
          case 'r':
            ch = '\r';
            break;
          case 't':
            ch = '\t';
            break;
          case '\"':
            ch = '\"';
            break;
          case '\'':
            ch = '\'';
            break;
          // Hex Unicode: u????
          case 'u':
            if (i >= st.length() - 5) {
              ch = 'u';
              break;
            }
            int code = Integer.parseInt(""
                + st.charAt(i + 2)
                + st.charAt(i + 3)
                + st.charAt(i + 4)
                + st.charAt(i + 5), 16);
            sb.append(Character.toChars(code));
            i += 5;
            continue;
        }
        i++;
      }
      sb.append(ch);
    }
    return sb.toString();
  }
}
