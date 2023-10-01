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
