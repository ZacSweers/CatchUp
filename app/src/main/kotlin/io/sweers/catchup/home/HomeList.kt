package io.sweers.catchup.home

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.sweers.catchup.R
import io.sweers.catchup.home.HomeScreen.Event.Selected

@Composable
fun HomeList(state: HomeScreen.State, modifier: Modifier = Modifier) {
  Surface(modifier = modifier.fillMaxHeight()) {
    LazyColumn(modifier = Modifier.systemBarsPadding().padding(16.dp)) {
      // Title
      item {
        // TODO changelog present icon?
        Text(
          text = stringResource(id = R.string.app_name),
          style = MaterialTheme.typography.displayLarge,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(bottom = 32.dp, start = 12.dp)
        )
      }
      // Services
      for ((index, meta) in state.serviceMetas.withIndex()) {
        item {
          HomeListItemEntry(
            icon = meta.icon,
            title = meta.name,
            serviceTint = meta.themeColor,
            description = "",
            isSelected = state.selectedIndex == index,
            onClick = { state.eventSink(Selected(index)) }
          )
        }
      }
      // TODO settings
    }
  }
}

@Composable
private fun HomeListItemEntry(
  @DrawableRes icon: Int,
  @ColorRes serviceTint: Int,
  @StringRes title: Int,
  description: String,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val color =
    if (isSelected) {
      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.padding(16.dp).size(48.dp),
        tint = colorResource(serviceTint)
      )
      Column(
        modifier = Modifier.align(Alignment.CenterVertically),
        verticalArrangement = spacedBy(8.dp)
      ) {
        Text(
          text = stringResource(title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black
        )
        if (description.isNotBlank()) {
          Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    }
  }
}
