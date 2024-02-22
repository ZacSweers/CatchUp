package catchup.repro

import java.io.File
import okhttp3.OkHttpClient

abstract class HttpClient
internal constructor(protected val client: OkHttpClient, protected val defaultDownloadsPath: File) {

}