package catchup.app.ui.about

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import catchup.app.data.LinkManager
import catchup.app.data.github.ProjectOwnersByIdsQuery
import catchup.app.data.github.RepositoriesByIdsQuery
import catchup.app.data.github.RepositoryByNameAndOwnerQuery
import catchup.app.service.ClickableItem
import catchup.app.service.ErrorItem
import catchup.app.service.TextItem
import catchup.app.service.openUrl
import catchup.app.ui.about.LicensesScreen.Event.Click
import catchup.app.ui.about.LicensesScreen.State
import catchup.compose.rememberStableCoroutineScope
import catchup.di.AppScope
import catchup.gemoji.EmojiMarkdownConverter
import catchup.gemoji.replaceMarkdownEmojisIn
import catchup.service.api.CatchUpItem
import catchup.service.api.ContentType
import catchup.util.injection.qualifiers.ApplicationContext
import catchup.util.kotlin.groupBy
import catchup.util.kotlin.sortBy
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.http.HttpFetchPolicy
import com.apollographql.apollo3.cache.http.httpFetchPolicy
import com.apollographql.apollo3.exception.ApolloException
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.Module
import dagger.Provides
import dev.zacsweers.catchup.app.scaffold.R
import java.util.Objects
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okio.buffer
import okio.source
import timber.log.Timber

@Parcelize
object LicensesScreen : Screen {
  data class State(val items: ImmutableList<OssBaseItem>?, val eventSink: (Event) -> Unit) :
    CircuitUiState

  sealed interface Event : CircuitUiEvent {
    data class Click(val url: String) : Event
  }
}

@ContributesTo(AppScope::class)
@Module
object LicensesModule {
  @Provides fun provideAssets(@ApplicationContext context: Context): AssetManager = context.assets
}

@CircuitInject(LicensesScreen::class, AppScope::class)
class LicensesPresenter
@Inject
constructor(
  private val linkManager: LinkManager,
  private val licensesRepository: LicensesRepository,
) : Presenter<State> {
  @Composable
  override fun present(): State {
    // TODO use paging?
    val items by
      produceState<ImmutableList<OssBaseItem>?>(null) { value = licensesRepository.requestItems() }
    val scope = rememberStableCoroutineScope()
    return State(items) { event ->
      when (event) {
        is Click -> {
          scope.launch { linkManager.openUrl(event.url, Color.Black) }
        }
      }
    }
  }
}

private fun OssItem.toCatchUpItem(): CatchUpItem {
  return CatchUpItem(
    id = hashCode().toLong(),
    title = name,
    description = description,
    author = license,
    itemClickUrl = clickUrl,
    // Not summarizable
    contentType = ContentType.OTHER,
  )
}

@OptIn(ExperimentalFoundationApi::class)
@CircuitInject(LicensesScreen::class, AppScope::class)
@Composable
internal fun Licenses(state: State, modifier: Modifier = Modifier) {
  LazyColumn(modifier = modifier) {
    val items = state.items
    if (items == null) {
      item {
        Box(Modifier.fillParentMaxSize()) {
          CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
      }
    } else if (items.isEmpty()) {
      item {
        Box(Modifier.fillParentMaxSize()) {
          ErrorItem(
            text = stringResource(R.string.licenses_error),
            modifier = Modifier.align(Alignment.Center),
            onRetryClick = null,
          )
        }
      }
    } else {
      for (ossItem in items) {
        when (ossItem) {
          is OssItemHeader -> stickyHeader(ossItem.id) { OssItemHeaderUi(ossItem) }
          is OssItem ->
            item(ossItem.id) {
              val catchUpItem = ossItem.toCatchUpItem()
              ClickableItem(
                modifier = Modifier.animateItem(),
                onClick = { state.eventSink(Click(catchUpItem.clickUrl!!)) },
              ) {
                Row {
                  Spacer(Modifier.width(50.dp))
                  // TODO extract color and make it shared state
                  TextItem(catchUpItem, colorResource(R.color.colorAccent))
                }
              }
            }
        }
      }
    }
  }
}

@Composable
private fun OssItemHeaderUi(item: OssItemHeader) {
  Surface {
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val size = 40.dp
      val sizePx = with(LocalDensity.current) { size.toPx() }.toInt()
      val painter =
        rememberAsyncImagePainter(
          ImageRequest.Builder(LocalContext.current)
            .data(item.avatarUrl)
            .size(sizePx)
            .transformations(CircleCropTransformation())
            .build()
        )
      Image(painter = painter, contentDescription = item.name, modifier = Modifier.size(size))

      Spacer(modifier = Modifier.width(16.dp))

      Text(
        text = item.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Preview
@Composable
private fun PreviewHeaderUi() {
  OssItemHeaderUi(OssItemHeader("name", "Zac Sweers"))
}

interface LicensesRepository {
  suspend fun requestItems(): ImmutableList<OssBaseItem>
}

@ContributesBinding(AppScope::class)
class LicensesRepositoryImpl
@Inject
constructor(
  private val apolloClient: ApolloClient,
  private val markdownConverter: EmojiMarkdownConverter,
  private val moshi: Moshi,
  private val assets: AssetManager,
) : LicensesRepository {
  override suspend fun requestItems(): ImmutableList<OssBaseItem> {
    return try {
      requestItemsInner()
    } catch (e: ApolloException) {
      Timber.tag("LicensesRepository").e(e, "Failed to fetch OSS licenses")
      persistentListOf()
    }
  }

  /** I give you: the most over-engineered OSS licenses section ever. */
  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun requestItemsInner(): ImmutableList<OssBaseItem> {
    // Start with a fetch of our github entries from assets
    val githubEntries =
      withContext(Dispatchers.Default) {
        val adapter =
          moshi.adapter<List<OssGitHubEntry>>(
            Types.newParameterizedType(List::class.java, OssGitHubEntry::class.java)
          )
        val regular =
          withContext(Dispatchers.IO) {
            adapter.fromJson(assets.open("licenses_github.json").source().buffer())!!
          }
        val generated =
          withContext(Dispatchers.IO) {
            adapter.fromJson(assets.open("generated_licenses.json").source().buffer())!!
          }
        return@withContext regular + generated
      }
    // Fetch repos, send down a map of the ids to owner ids
    val idsToOwnerIds =
      githubEntries
        .asFlow()
        .map { RepositoryByNameAndOwnerQuery(it.owner, it.name) }
        .map {
          val response =
            apolloClient
              .newBuilder()
              .httpFetchPolicy(HttpFetchPolicy.CacheFirst)
              .build()
              .query(it)
              .execute()
          with(response.data!!.repository!!.onRepository) { id to owner.id }
        }
        .distinctUntilChangedBy { it }
        .fold(mutableMapOf()) { map: MutableMap<String, String>, (first, second) ->
          map.apply { put(first, second) }
        }

    // Fetch the users by their IDs
    val userIdToNameMap =
      withContext(Dispatchers.IO) {
        val response =
          apolloClient
            .newBuilder()
            .httpFetchPolicy(HttpFetchPolicy.CacheFirst)
            .build()
            .query(ProjectOwnersByIdsQuery(idsToOwnerIds.values.distinct()))
            .execute()

        response.data!!
          .nodes
          .filterNotNull()
          .asFlow()
          // Reduce into a map of the owner ID -> display name
          .fold(mutableMapOf<String, String>()) { map, node ->
            map.apply {
              node.onOrganization?.run { map[id] = (name ?: login) }
              node.onUser?.run { map[id] = (name ?: login) }
            }
          }
      }
    // Fetch the repositories by their IDs, map down to its
    return apolloClient
      .newBuilder()
      .httpFetchPolicy(HttpFetchPolicy.CacheFirst)
      .build()
      .query(RepositoriesByIdsQuery(idsToOwnerIds.keys.toList()))
      .execute()
      .data!!
      .nodes
      .asSequence()
      .mapNotNull { it?.onRepository }
      .asFlow()
      .map { it to userIdToNameMap.getValue(it.owner.id) }
      .map { (repo, ownerName) ->
        OssItem(
          avatarUrl = repo.owner.avatarUrl.toString(),
          author = ownerName,
          name = repo.name,
          clickUrl = repo.url.toString(),
          license = repo.licenseInfo?.name,
          description = repo.description,
        )
      }
      .onStart {
        moshi
          .adapter<List<OssItem>>(Types.newParameterizedType(List::class.java, OssItem::class.java))
          .fromJson(assets.open("licenses_mixins.json").source().buffer())!!
          .forEach { emit(it) }
      }
      .flowOn(Dispatchers.IO)
      .groupBy { it.author }
      .sortBy { it.first }
      .flatMapConcat { it.second.asFlow().sortBy { it.name } }
      .map {
        // TODO use CopyDynamic when 0.3.0 is out
        it.copy(
          author = markdownConverter.replaceMarkdownEmojisIn(it.author),
          name = markdownConverter.replaceMarkdownEmojisIn(it.name),
          description =
            it.description?.let { markdownConverter.replaceMarkdownEmojisIn(it) } ?: it.description,
        )
      }
      .flowOn(Dispatchers.IO)
      .toList()
      .let { runningItems ->
        buildList {
          with(runningItems[0]) { add(OssItemHeader(name = author, avatarUrl = avatarUrl)) }
          runningItems.fold(runningItems[0].author) { lastAuthor, currentItem ->
            if (currentItem.author != lastAuthor) {
              add(OssItemHeader(name = currentItem.author, avatarUrl = currentItem.avatarUrl))
            }
            add(currentItem)
            currentItem.author
          }
        }
      }
      .toImmutableList()
  }
}

@JsonClass(generateAdapter = true)
internal data class OssGitHubEntry(val owner: String, val name: String)

sealed class OssBaseItem {
  abstract fun itemType(): Int
}

internal data class OssItemHeader(val avatarUrl: String, val name: String) : OssBaseItem() {
  val id = name.hashCode().toLong()

  override fun itemType() = 0
}

@JsonClass(generateAdapter = true)
internal data class OssItem(
  val avatarUrl: String,
  val author: String,
  val name: String,
  val license: String?,
  val clickUrl: String,
  val description: String?,
  val authorUrl: String? = null,
) : OssBaseItem() {
  val id = Objects.hash(author, name).toLong()

  override fun itemType() = 1
}
