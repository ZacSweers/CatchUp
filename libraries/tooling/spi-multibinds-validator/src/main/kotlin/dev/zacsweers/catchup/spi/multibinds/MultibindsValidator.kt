package dev.zacsweers.catchup.spi.multibinds

import com.google.auto.service.AutoService
import dagger.spi.model.Binding
import dagger.spi.model.BindingGraph
import dagger.spi.model.BindingGraphPlugin
import dagger.spi.model.DiagnosticReporter
import javax.tools.Diagnostic

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
          diagnosticReporter.reportBinding(
            Diagnostic.Kind.ERROR,
            binding,
            "Multibinding ${binding.key()} has no dependencies",
          )
        }
      }
  }
}
