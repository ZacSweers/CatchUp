package catchup.app.service.bookmarks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

@Suppress("UnusedReceiverParameter")
val Icons.Outlined.Bookmark: ImageVector
  get() {
    if (_outlinedBookmark != null) {
      return _outlinedBookmark!!
    }
    _outlinedBookmark =
      materialIcon(name = "Outlined.Bookmark") {
        materialPath {
          moveTo(17.0f, 3.0f)
          lineTo(7.0f, 3.0f)
          curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
          lineTo(5.0f, 21.0f)
          lineToRelative(7.0f, -3.0f)
          lineToRelative(7.0f, 3.0f)
          lineTo(19.0f, 5.0f)
          curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
          close()
          moveTo(17.0f, 18.0f)
          lineToRelative(-5.0f, -2.18f)
          lineTo(7.0f, 18.0f)
          lineTo(7.0f, 5.0f)
          horizontalLineToRelative(10.0f)
          verticalLineToRelative(13.0f)
          close()
        }
      }
    return _outlinedBookmark!!
  }

@Suppress("ObjectPropertyName") private var _outlinedBookmark: ImageVector? = null
