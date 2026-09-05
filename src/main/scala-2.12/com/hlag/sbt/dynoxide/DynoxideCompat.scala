package com.hlag.sbt.dynoxide

import com.hlag.dynoxide.core.DynoxideServer

import sbt._
import sbt.Keys._

import DynoxidePlugin.autoImport._

/**
 * sbt-version-specific wiring.
 *
 * On sbt 1.x, `Test / test` is a plain `TaskKey[Unit]`, so depending on it uses `.value`. On sbt
 * 2.x, `Test / test` became an `InputKey[TestResult]` (see the scala-3 variant of this file),
 * which requires `.evaluated` instead. sbt 1.x also has no task-result caching across command
 * invocations, so `startDynoxide` / `stopDynoxide` don't need the `sbt.util.Uncached` wrapper
 * required on sbt 2.x (see the scala-3 variant).
 */
private[dynoxide] object DynoxideCompat {
  def taskSettings: Seq[Setting[_]] = Seq(
    startDynoxide := {
      val port    = dynoxidePort.value
      val version = dynoxideVersion.value
      val baseDir = (LocalRootProject / baseDirectory).value
      val log     = DynoxidePlugin.adapt(streams.value.log)
      DynoxideServer.ensureRunning(port = port, version = version, baseDir = baseDir, log = log)
    },

    stopDynoxide := {
      val log = DynoxidePlugin.adapt(streams.value.log)
      DynoxideServer.forceStop(log)
    },

    (Test / test)     := (Test / test).dependsOn(startDynoxide).value,
    (Test / testOnly) := (Test / testOnly).dependsOn(startDynoxide).evaluated,
  )
}
