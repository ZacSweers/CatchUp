package catchup.util.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun createFileShareIntent(context: Context, file: File, intentType: String): Intent {
  val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

  return Intent(Intent.ACTION_SEND).apply {
    type = intentType
    putExtra(Intent.EXTRA_STREAM, fileUri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
}
