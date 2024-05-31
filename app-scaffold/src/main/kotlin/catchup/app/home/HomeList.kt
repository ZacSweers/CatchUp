package catchup.app.home

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import catchup.app.home.HomeScreen.Event.Selected
import catchup.app.home.HomeScreen.Event.ShowChangelog
import catchup.app.home.HomeScreen.State
import catchup.compose.dynamicAwareColor
import dev.zacsweers.catchup.app.scaffold.R

@Composable
fun HomeList(state: State, modifier: Modifier = Modifier) {
  Surface(modifier = modifier.fillMaxHeight()) {
    LazyColumn(modifier = Modifier.statusBarsPadding().padding(16.dp)) {
      // Title & changelog
      item {
        Box(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 32.dp, start = 12.dp),
          )
          if (state.changelogAvailable) {
            ChangelogButton(Modifier.align(Alignment.TopEnd)) { state.eventSink(ShowChangelog) }
          }
        }
      }
      // Services
      for ((index, meta) in state.serviceMetas.withIndex()) {
        item {
          HomeListItemEntry(
            icon = painterResource(meta.icon),
            title = stringResource(meta.name),
            serviceTint = colorResource(meta.themeColor),
            description = "",
            isSelected = state.selectedIndex == index,
            onClick = { state.eventSink(Selected(index)) },
          )
        }
      }
      item {
        // This is "bonus" index, one beyond service metas
        // TODO merge this with service metas instead?
        val index = state.serviceMetas.size
        HomeListItemEntry(
          icon = rememberVectorPainter(Icons.Filled.Settings),
          title = stringResource(R.string.title_activity_settings),
          serviceTint = MaterialTheme.colorScheme.onPrimaryContainer,
          description = "Miscellaneous CatchUp settings",
          isSelected = state.selectedIndex == index,
          onClick = { state.eventSink(Selected(index)) },
        )
      }
    }
  }
}

@Composable
private fun HomeListItemEntry(
  icon: Painter,
  serviceTint: Color,
  title: String,
  description: String,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val tintColor =
    dynamicAwareColor(
      regularColor = { serviceTint },
      dynamicColor = { MaterialTheme.colorScheme.primary },
    )
  // TODO animate this on changes?
  val color =
    if (isSelected) {
      tintColor.copy(alpha = 0.2f)
    } else {
      Color.Unspecified
    }
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    color = color,
    shape = RoundedCornerShape(24.dp),
  ) {
    // A row with an icon followed by two lines of text
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        painter = icon,
        contentDescription = null,
        modifier = Modifier.padding(16.dp).size(48.dp),
        tint = tintColor,
      )
      Column(
        modifier = Modifier.align(Alignment.CenterVertically),
        verticalArrangement = spacedBy(4.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black,
        )
        if (description.isNotBlank()) {
          Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}
