/*
 * Copyright (C) 2026. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package catchup.app.service

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.LocalCircuit
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface CircuitModule {
  @Multibinds fun presenterFactories(): Set<Presenter.Factory>

  @Multibinds fun viewFactories(): Set<Ui.Factory>

  companion object {
    @Provides
    fun provideCircuit(
      presenterFactories: Set<Presenter.Factory>,
      uiFactories: Set<Ui.Factory>,
    ): Circuit {
      return Circuit.Builder()
        .addPresenterFactories(presenterFactories)
        .addUiFactories(uiFactories)
        .setOnUnavailableContent { screen, modifier ->
          val circuit = LocalCircuit.current
          BasicText(
            """
              Route not available: ${screen.javaClass.name}.
              Presenter: ${circuit?.presenter(screen, Navigator.NoOp)?.javaClass}
              UI: ${circuit?.ui(screen)?.javaClass}
              All presenterFactories: ${circuit?.newBuilder()?.presenterFactories}
              All uiFactories: ${circuit?.newBuilder()?.uiFactories}
              """
              .trimIndent(),
            modifier.background(Color.Red),
            style = TextStyle(color = Color.Yellow),
          )
        }
        .build()
    }
  }
}
