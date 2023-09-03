package catchup.app.home

import catchup.di.AppScope
import com.slack.circuit.runtime.screen.Screen
import com.squareup.anvil.annotations.ContributesTo
import dagger.BindsOptionalOf
import dagger.Module

@ContributesTo(AppScope::class)
@Module
interface ContributorModule {
  @BindsOptionalOf fun bind(): DrawerScreen
}

class DrawerScreen(val screen: Screen)
