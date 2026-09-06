# sbt-dynoxide

sbt plugin: manages a [Dynoxide](https://github.com/nubo-db/dynoxide) DynamoDB emulator for
integration tests. No Docker, no JVM, no npm.

![Maven Central](https://img.shields.io/maven-central/v/com.hlag/sbt-dynoxide_sbt2_3.svg)](https://central.sonatype.com/artifact/com.hlag/sbt-dynoxide_sbt2_3)
[![CI](https://github.com/Hapag-Lloyd/sbt-dynoxide/actions/workflows/ci.yml/badge.svg)](https://github.com/Hapag-Lloyd/sbt-dynoxide/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![sbt](https://img.shields.io/badge/sbt-1.x%20%7C%202.x-informational)](#sbt-version-support)

## Features

- Auto-downloads the correct pre-built Dynoxide binary for your platform (macOS/Linux, arm64/x86_64) from GitHub
  Releases.
- Caches the binary locally under `.dynoxide/<version>/` — no re-download on later runs.
- One Dynoxide process shared across subprojects, with reference counting so the first subproject to finish doesn't kill
  it while another still needs it.
- Automatically starts before `test`/`testOnly` and stops after, only in subprojects with the
  plugin enabled — other subprojects never trigger it.
- Zero runtime dependencies — the plugin only uses the sbt API and the JDK (HTTP client, zip/tar extraction, process
  control).

## Requirements

- JDK 17+
- sbt 1.x or sbt 2.x (see [sbt version support](#sbt-version-support))
- Network access to GitHub Releases on first run (to download the Dynoxide binary)

## Install

Add to `project/plugins.sbt`:

```scala
addSbtPlugin("com.hlag" % "sbt-dynoxide" % "<version>")
```

## Usage

Enable the plugin on any subproject that needs a live DynamoDB endpoint for its tests:

```scala
lazy val integrationTests = project
  .enablePlugins(DynoxidePlugin)
```

Point your test code's AWS SDK v2 `DynamoDbClient` at the emulator. This snippet is minimal —
you'll also need dummy credentials and a region, since the SDK requires both to be present:

```scala
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

val client = DynamoDbClient.builder()
  .endpointOverride(URI.create("http://localhost:8000"))
  .build()
```

For IDE runs (bypassing `sbt test`), start/stop the emulator yourself; the plugin only hooks
`sbt`'s `test`/`testOnly` tasks.

## Configuration / Keys

| Key               | Type                 | Default  | Description                                                                                 |
|-------------------|----------------------|----------|---------------------------------------------------------------------------------------------|
| `dynoxidePort`    | `SettingKey[Int]`    | `8000`   | Port the Dynoxide emulator listens on.                                                      |
| `dynoxideVersion` | `SettingKey[String]` | `v1.1.0` | Dynoxide release version to download.                                                       |
| `startDynoxide`   | `TaskKey[Unit]`      | —        | Downloads (if needed) and starts the emulator. Runs automatically before `test`/`testOnly`. |
| `stopDynoxide`    | `TaskKey[Unit]`      | —        | Stops the emulator process explicitly.                                                      |

## How it works

On first run, the plugin downloads the Dynoxide binary for your OS/architecture from GitHub
Releases and caches it under `<project-root>/.dynoxide/<version>/dynoxide[.exe]`. Subsequent runs
reuse the cached binary. The emulator is started once — before the first enabled subproject's
tests run — and stopped only after the last one finishes, using a reference count so concurrent
subprojects don't race to stop it.

The binary download/cache, process start/stop, and readiness-probing logic lives in
[`dynoxide-scala-core`](https://github.com/Hapag-Lloyd/dynoxide-scala-core), a build-tool-agnostic
library with no dependency on sbt. This plugin is a thin sbt-specific adapter around it — the same
core is also used by [`mill-dynoxide`](https://github.com/Hapag-Lloyd/mill-dynoxide) for Mill
builds, so the emulator-management logic is written and tested once, not duplicated per build tool.

## sbt version support

| sbt version | Plugin Scala | Published artifact      |
|-------------|--------------|-------------------------|
| sbt 1.x     | Scala 2.12   | `sbt-dynoxide_2.12_1.0` |
| sbt 2.x     | Scala 3      | `sbt-dynoxide_sbt2_3`   |

You always add the same `addSbtPlugin("com.hlag" % "sbt-dynoxide" % "<version>")` — sbt resolves
the correct variant for the sbt version you're running.

## Contributing

Dependencies are kept up to date via Dependabot (GitHub Actions) and Scala Steward (library/sbt
versions). To run the test suite locally:

```shell
sbt +scripted
```

This cross-builds and runs the scripted tests against both sbt 1.x and sbt 2.x.

## License

Apache-2.0 — see [LICENSE](LICENSE).

