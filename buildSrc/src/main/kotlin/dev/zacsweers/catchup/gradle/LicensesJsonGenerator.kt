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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.Locale
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

class LicensesJsonGeneratorPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.extensions.findByType<AppExtension>()!!.applicationVariants.configureEach {
      if (buildType.name == "release") {
        project.tasks.register<LicensesJsonGenerator>("generateLicensesJson") {
          group = "licenses"
          artifactFiles.from(runtimeConfiguration.artifactView().artifacts.artifactFiles)
          configuration = runtimeConfiguration
          jsonFile.set(project.file("licenses.json"))
        }
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
    val result = project.dependencies.createArtifactResolutionQuery()
        .forComponents(componentIds)
        .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
        .execute()

    val poms = result.resolvedComponents
        .flatMap { component ->
          component.getArtifacts(MavenPomArtifact::class.java)
              .filterIsInstance<ResolvedArtifactResult>()
              .onEach {
                println("POM file for ${component.id}: ${it.file}")
              }
              .map { it.file }
        }
        .distinct()

    val githubDetails = poms.mapNotNull { readGithubProjectsFromPom(it.absolutePath) }
        .map { it.substringAfter(".com/") }
        .associate { repo ->
          val (owner, name) = repo.split("/")
          owner to name
        }

    JsonWriter.of(jsonFile.get().asFile.sink().buffer()).use { writer ->
      writer.beginArray()
      githubDetails.entries.sortedBy { it.toString().toLowerCase(Locale.US) }
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
}

/*
 * Below bits borrowed from https://github.com/kropp/gradle-plugin-thanks.
 */

private fun readGithubProjectsFromPom(filename: String): String? {
  val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filename)
  val scm = document.getElementsByTagName("scm").asSequence().firstOrNull() ?: return null
  val url = scm.childNodes.asSequence()
      .filter { it.textContent.contains("github.com") }
      .firstOrNull()?.textContent
  return url?.substringAfter("github.com")
      ?.removePrefix("/")
      ?.removePrefix(":")
      ?.removeSuffix(".git")
      ?.removeSuffix("/issues")
}

private fun NodeList.asSequence() = NodeListSequence(this)

private class NodeListSequence(private val nodes: NodeList) : Sequence<Node> {
  override fun iterator() = NodeListIterator(nodes)
}

private class NodeListIterator(private val nodes: NodeList) : Iterator<Node> {
  private var i = 0
  override fun hasNext() = nodes.length > i
  override fun next(): Node = nodes.item(i++)
}
