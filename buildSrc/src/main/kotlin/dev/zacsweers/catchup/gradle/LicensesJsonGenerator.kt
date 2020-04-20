/*
 * Copyright (C) 2020. Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package dev.zacsweers.catchup.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.squareup.moshi.JsonWriter
import okio.buffer
import okio.sink
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

private const val TAG = "LicensesJsonGenerator"

class LicensesJsonGeneratorPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.extensions.findByType<AppExtension>()!!.applicationVariants.configureEach {
      if (buildType.name == "release") {
        val licensesTask = project.tasks.register<LicensesJsonGenerator>("generateLicensesJson") {
          group = "licenses"
          artifactFiles.from(runtimeConfiguration.artifactView().artifacts.artifactFiles)
          configuration = runtimeConfiguration

          // TODO should we just generate this into an intermediate and not check it in?
          jsonFile.set(project.file("src/main/assets/generated_licenses.json"))
        }

        // Make the release assemble task depend on this to be safe
        project.tasks.named("assemble${name.capitalize(Locale.US)}")
            .dependsOn(licensesTask)
      }
    }
  }

  private fun Configuration.artifactView(): ArtifactView {
    return incoming.artifactView {
      attributes {
        attribute(Attribute.of("artifactType", String::class.java), "android-classes")
      }
    }
  }
}

abstract class LicensesJsonGenerator : DefaultTask() {
  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise
   * unused. See [configuration].
   */
  @get:Classpath
  abstract val artifactFiles: ConfigurableFileCollection

  /**
   * This is what the task actually uses as its input.
   */
  @get:Internal
  lateinit var configuration: Configuration

  @get:OutputFile
  abstract val jsonFile: RegularFileProperty

  @TaskAction
  fun generateFile() {
    val componentIds = configuration.incoming.resolutionResult.allDependencies.map { it.from.id }
        .filterIsInstance<ModuleComponentIdentifier>()

    val githubDetails = fetchComponents(componentIds)
        .map { it.substringAfter(".com/") }
        .map { repo ->
          val (owner, name) = repo.split("/")
          owner to name
        }
        .distinct()

    JsonWriter.of(jsonFile.get().asFile.sink().buffer()).use { writer ->
      writer.beginArray()
      githubDetails.sortedBy { it.toString().toLowerCase(Locale.US) }
          .forEach { (owner, name) ->
            writer.beginObject()
            writer.name("owner")
                .value(owner)
                .name("name")
                .value(name)
            writer.endObject()
          }
      writer.endArray()
    }
  }

  private fun fetchComponents(componentIds: List<ModuleComponentIdentifier>): Sequence<String> {
    return project.dependencies.createArtifactResolutionQuery()
        .forComponents(componentIds)
        .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
        .execute()
        .resolvedComponents
        .asSequence()
        .flatMap { component ->
          component.getArtifacts(MavenPomArtifact::class.java)
              .filterIsInstance<ResolvedArtifactResult>()
              .onEach {
                logger.debug("$TAG: POM file for ${component.id}: ${it.file}")
              }
              .asSequence()
              .flatMap { result ->
                val pomFile = result.file
                pomFile.readGithubProjectsFromPom() ?: run {
                  val id = component.id as ModuleComponentIdentifier
                  val group = id.group
                  sequenceOf(knownMapping(group))
                }
              }
        }
        .filterNotNull()
  }

  private fun File.readGithubProjectsFromPom(): Sequence<String>? {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(absolutePath)
    document.parseScm()?.let { return sequenceOf(it) }

    // If there's a parent tag, try to fetch that instead
    val parent = document.getElementsByTagName("parent").asSequence().firstOrNull() ?: return null
    val parentNodes = parent.childNodes.asSequence()
        .associateBy { it.nodeName }

    val groupId = parentNodes["groupId"]?.textContent ?: return null
    val artifactId = parentNodes["artifactId"]?.textContent ?: return null
    val version = parentNodes["version"]?.textContent ?: return null

    val component = DefaultModuleComponentIdentifier
        .newId(DefaultModuleIdentifier.newId(groupId, artifactId), version)

    logger.debug("$TAG: Fetching parent $component")

    return fetchComponents(listOf(component))
        .onEach {
          logger.debug("$TAG: Resolved parent version for '$component': $it")
        }
  }

  private fun Document.parseScm(): String? {
    val scm = getElementsByTagName("scm").asSequence().firstOrNull() ?: return null
    val url = scm.childNodes.asSequence()
        .filter { it.textContent.contains("github.com") }
        .firstOrNull()?.textContent
    return url?.substringAfter("github.com")
        ?.removePrefix("/")
        ?.removePrefix(":")
        ?.removeSuffix(".git")
        ?.removeSuffix("/issues")
  }
}

private fun knownMapping(group: String): String? {
  if (group.startsWith("androidx.") || group.startsWith("com.google.android.gms")) {
    // These are handled via licenses_mixins.json
    return null
  }
  return when (group) {
    "com.google.firebase" -> "firebase/firebase-android-sdk"
    else -> error("Unrecognized group! $group")
  }
}

/*
 * Below bits borrowed from https://github.com/kropp/gradle-plugin-thanks.
 */

private fun NodeList.asSequence() = NodeListSequence(this)

private class NodeListSequence(private val nodes: NodeList) : Sequence<Node> {
  override fun iterator() = NodeListIterator(nodes)
}

private class NodeListIterator(private val nodes: NodeList) : Iterator<Node> {
  private var i = 0
  override fun hasNext() = nodes.length > i
  override fun next(): Node = nodes.item(i++)
}
