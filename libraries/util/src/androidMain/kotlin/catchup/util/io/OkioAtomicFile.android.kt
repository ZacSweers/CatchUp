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
