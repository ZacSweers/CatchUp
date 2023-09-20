package catchup.app.service.bookmarks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

@Suppress("UnusedReceiverParameter")
val Icons.Filled.Bookmark: ImageVector
  get() {
    if (_filledBookmark != null) {
      return _filledBookmark!!
    }
    _filledBookmark =
      materialIcon(name = "Filled.Bookmark") {
        materialPath {
          moveTo(17.0f, 3.0f)
          horizontalLineTo(7.0f)
          curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
          lineTo(5.0f, 21.0f)
          lineToRelative(7.0f, -3.0f)
          lineToRelative(7.0f, 3.0f)
          verticalLineTo(5.0f)
          curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
          close()
        }
      }
    return _filledBookmark!!
  }

@Suppress("ObjectPropertyName") private var _filledBookmark: ImageVector? = null
