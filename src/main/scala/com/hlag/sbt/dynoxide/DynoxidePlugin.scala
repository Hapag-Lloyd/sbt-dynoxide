package com.hlag.sbt.dynoxide

import com.hlag.dynoxide.core.DynoxideServer

import sbt.*
import sbt.Keys.*

/**
 * SBT AutoPlugin that manages a shared Dynoxide DynamoDB emulator process for
 * integration tests.
 *
 * On first run, the plugin downloads the correct pre-built Dynoxide binary for
 * the current platform from GitHub Releases and caches it under
 * `<project-root>/.dynoxide/<version>/dynoxide[.exe]`. Subsequent runs reuse
 * the cached binary — no JVM, no Docker, no npm required.
 *
 * A single in-memory Dynoxide instance is started before the first subproject's
 * tests run and destroyed only after the last subproject completes. Reference
 * counting ensures that when multiple subprojects enable this plugin (e.g.
 * `integrationTests` and `examples`), the first one to finish does not kill the
 * process while the other is still running.
 *
 * The actual process lifecycle (binary download/cache, start/stop, readiness
 * probing) lives in the build-tool-agnostic `dynoxide-scala-core` library
 * (`com.hlag.dynoxide.core.DynoxideServer`); this plugin only wires it into
 * sbt's task/setting model.
 *
 * Enable per subproject:
 * {{{
 *   lazy val integrationTests = project
 *     .enablePlugins(DynoxidePlugin)
 * }}}
 *
 * Test code should point the AWS SDK v2 `DynamoDbClient` at
 * `http://localhost:<port>` via `endpointOverride`.
 */
object DynoxidePlugin extends AutoPlugin {

  private val DefaultVersion = "v0.13.0"

  object autoImport {
    val dynoxidePort: SettingKey[Int] =
      settingKey[Int]("Port for Dynoxide DynamoDB emulator (default: 8000)")

    val dynoxideVersion: SettingKey[String] =
      settingKey[String](s"Dynoxide release version to download (default: $DefaultVersion)")

    val startDynoxide: TaskKey[Unit] =
      taskKey[Unit]("Download (if needed) and start Dynoxide DynamoDB emulator")

    val stopDynoxide: TaskKey[Unit] =
      taskKey[Unit]("Stop the Dynoxide DynamoDB emulator process")
  }

  import autoImport.*

  override val trigger: PluginTrigger = noTrigger
  override val requires: Plugins      = plugins.JvmPlugin

  override lazy val projectSettings = Seq(
    dynoxidePort    := 8000,
    dynoxideVersion := DefaultVersion,

    (Test / testOptions) += Tests.Cleanup(() => DynoxideServer.release()),
  ) ++ DynoxideCompat.taskSettings

  /** Adapts sbt's `sbt.util.Logger` to `dynoxide-scala-core`'s build-tool-agnostic logger. */
  private[dynoxide] def adapt(log: sbt.util.Logger): com.hlag.dynoxide.core.DynoxideLogger =
    new com.hlag.dynoxide.core.DynoxideLogger {
      def info(message: String): Unit  = log.info(message)
      def debug(message: String): Unit = log.debug(message)
      def warn(message: String): Unit  = log.warn(message)
    }
}
