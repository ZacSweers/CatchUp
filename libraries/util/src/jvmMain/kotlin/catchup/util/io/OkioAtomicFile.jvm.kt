/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
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
package catchup.util.io

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import okio.Path

internal actual fun Path.setPosixFilePermissions() {
  // Set perms to 00771
  Files.setPosixFilePermissions(
    toNioPath(),
    setOf(
      // Owner
      PosixFilePermission.OWNER_READ,
      PosixFilePermission.OWNER_WRITE,
      PosixFilePermission.OWNER_EXECUTE,
      // Group
      PosixFilePermission.GROUP_READ,
      PosixFilePermission.GROUP_WRITE,
      PosixFilePermission.GROUP_EXECUTE,
      // Other
      PosixFilePermission.OTHERS_EXECUTE,
    ),
  )
}
