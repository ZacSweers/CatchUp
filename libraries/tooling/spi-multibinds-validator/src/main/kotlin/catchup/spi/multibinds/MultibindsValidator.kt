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
package catchup.spi.multibinds

import com.google.auto.service.AutoService
import dagger.spi.model.Binding
import dagger.spi.model.BindingGraph
import dagger.spi.model.BindingGraphPlugin
import dagger.spi.model.DiagnosticReporter
import javax.tools.Diagnostic

private val ALLOWED_LIST = setOf("AppConfigMetadataContributor", "okhttp3.Interceptor", "ViewModel")

/** A [BindingGraphPlugin] that validates that all multibinds have dependencies. */
@AutoService(BindingGraphPlugin::class)
class MultibindsValidator : BindingGraphPlugin {
  override fun visitGraph(bindingGraph: BindingGraph, diagnosticReporter: DiagnosticReporter) {
    bindingGraph
      .network()
      .nodes()
      .asSequence()
      .filterIsInstance<Binding>()
      .filter { it.kind().isMultibinding }
      .forEach { binding ->
        if (binding.dependencies().isEmpty()) {
          val bindingString = binding.toString()
          if (ALLOWED_LIST.any { it in bindingString }) {
            return@forEach
          }
          diagnosticReporter.reportBinding(
            Diagnostic.Kind.ERROR,
            binding,
            "Multibinding ${binding.key()} has no dependencies",
          )
        }
      }
  }
}
