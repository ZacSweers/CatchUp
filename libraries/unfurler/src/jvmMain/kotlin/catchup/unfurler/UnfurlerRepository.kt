package catchup.unfurler

import androidx.compose.runtime.Immutable
import catchup.di.AppScope
import catchup.di.SingleIn
import catchup.sqldelight.SqlDriverFactory
import catchup.util.kotlin.NullableLruCache
import dagger.Lazy
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.unfurl.Unfurler
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

/** A simple unfurler client that maintains a small in-memory and DB cache. */
@SingleIn(AppScope::class)
class UnfurlerRepository
@Inject
constructor(private val okHttpClient: Lazy<OkHttpClient>, sqlDriverFactory: SqlDriverFactory) {
  private val db = UnfurlerDatabase(sqlDriverFactory.create(UnfurlerDatabase.Schema, "unfurler.db"))

  // Note: Unfurler maintains its own internal 100-item in-memory cache but no API to expose check
  // it, so we disable it by setting cacheSize to 0.
  private val unfurler by lazy {
    // Initialized lazily so that it's off the main thread
    Unfurler(httpClient = okHttpClient.get(), cacheSize = 0)
  }

  private val cache = NullableLruCache<String, UnfurlResult>(maxSize = 100)

  suspend fun loadUnfurl(url: String): UnfurlResult? {
    return cache.computeIfAbsent(url) { loadInternal(url) }
  }

  private suspend fun loadInternal(url: String): UnfurlResult? {
    return withContext(Dispatchers.IO) {
      db.unfurlsQueries.getUnfurl(url, ::UnfurlResult).executeAsOneOrNull()?.let { dbResult ->
        return@withContext dbResult
      }
      val unfurlerResult =
        unfurler.unfurl(url)?.let(UnfurlResult::fromInternalResult) ?: return@withContext null
      // TODO make an alias table instead? We do this because urls may be different
      for (key in listOf(url, unfurlerResult.url).distinct()) {
        db.unfurlsQueries.insert(
          url = key,
          title = unfurlerResult.title,
          description = unfurlerResult.description,
          favicon = unfurlerResult.favicon.toString(),
          thumbnail = unfurlerResult.thumbnail.toString(),
        )
      }
      return@withContext unfurlerResult
    }
  }
}

@Immutable
data class UnfurlResult(
  // NOTE: the order matters here as it must match the DB.
  val url: String,
  val title: String?,
  val description: String?,
  val favicon: String?,
  val thumbnail: String?,
) {
  val domain = url.toHttpUrl().host.removePrefix("www.")

  internal companion object {
    fun fromInternalResult(unfurlResult: me.saket.unfurl.UnfurlResult): UnfurlResult {
      return UnfurlResult(
        url = unfurlResult.url.toString(),
        title = unfurlResult.title,
        description = unfurlResult.description,
        favicon = unfurlResult.favicon.toString(),
        thumbnail = unfurlResult.thumbnail.toString(),
      )
    }
  }
}
