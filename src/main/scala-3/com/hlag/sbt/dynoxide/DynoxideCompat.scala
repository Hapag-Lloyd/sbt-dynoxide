package com.hlag.sbt.dynoxide

import com.hlag.dynoxide.core.DynoxideServer

import sbt._
import sbt.Keys._
import sbt.util.Uncached

import DynoxidePlugin.autoImport._

/**
 * sbt-version-specific wiring.
 *
 * On sbt 2.x, `Test / test` became an `InputKey[TestResult]` (it was a plain `TaskKey[Unit]` on
 * sbt 1.x — see the scala-2.12 variant of this file), so depending on it requires `.evaluated`
 * instead of `.value`.
 *
 * sbt 2.x also caches task results across command invocations within the same session (keyed by
 * task inputs). `startDynoxide` / `stopDynoxide` are side-effecting (they start/stop a real OS
 * process) and must always re-run, so their bodies are wrapped in `sbt.util.Uncached` to opt out
 * of that caching. Without it, invoking `test` twice in one sbt 2 session would skip the second
 * `startDynoxide` and leave no emulator running for the second test run.
 */
private[dynoxide] object DynoxideCompat {
  def taskSettings: Seq[Setting[?]] = Seq(
    startDynoxide := Uncached {
      val port    = dynoxidePort.value
      val version = dynoxideVersion.value
      val baseDir = (LocalRootProject / baseDirectory).value
      val log     = DynoxidePlugin.adapt(streams.value.log)
      DynoxideServer.ensureRunning(port = port, version = version, baseDir = baseDir, log = log)
    },

    stopDynoxide := Uncached {
      val log = DynoxidePlugin.adapt(streams.value.log)
      DynoxideServer.forceStop(log)
    },

    (Test / test)     := (Test / test).dependsOn(startDynoxide).evaluated,
    (Test / testOnly) := (Test / testOnly).dependsOn(startDynoxide).evaluated,
  )
}
