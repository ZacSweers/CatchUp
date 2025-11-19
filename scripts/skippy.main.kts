#!/usr/bin/env kotlin
// Gradle plugin repo required for the module-graph-assert plugin dep
@file:Repository("https://plugins.gradle.org/m2")
@file:DependsOn("com.slack.foundry:cli:0.31.3")
@file:DependsOn("com.slack.foundry:skippy:0.33.0")
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.3")

import com.github.ajalt.clikt.command.main
import foundry.skippy.ComputeAffectedProjectsCli
import kotlinx.coroutines.runBlocking

runBlocking { ComputeAffectedProjectsCli().main(args) }
