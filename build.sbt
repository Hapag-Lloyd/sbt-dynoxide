Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization     := "com.hlag"
ThisBuild / organizationName := "Hapag-Lloyd"
ThisBuild / homepage         := Some(uri("https://github.com/Hapag-Lloyd/sbt-dynoxide"))
ThisBuild / licenses         := List("Apache-2.0" -> uri("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers       := List(
  Developer("baldram", "Marcin Szałomski", "", uri("https://github.com/baldram"))
)
ThisBuild / scmInfo          := Some(
  ScmInfo(uri("https://github.com/Hapag-Lloyd/sbt-dynoxide"), "scm:git:git@github.com:Hapag-Lloyd/sbt-dynoxide.git")
)
ThisBuild / description      := "sbt plugin managing a Dynoxide DynamoDB emulator for integration tests"
ThisBuild / versionScheme    := Some("early-semver")

// the artifact `version` is managed by sbt-ci-release/sbt-dynver

val scalaForSbt1 = "2.13.18"
val scalaForSbt2 = "3.9.0"

val dynoxideScalaCoreVersion = "0.8.1"

// Synced with github.com/nubo-db/dynoxide releases
val dynoxideVersion = IO.read(file("dynoxide.version")).trim

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    name                          := "sbt-dynoxide",
    sbtPlugin                     := true,
    scalaVersion                  := scalaForSbt2,
    crossScalaVersions            := Seq(scalaForSbt2, scalaForSbt1),
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.12.15"
        case _      => "2.0.6"
      }
    },
    scriptedBufferLog             := false,
    scriptedLaunchOpts += s"-Dplugin.version=${version.value}",

    libraryDependencies += "com.hlag" %% "dynoxide-scala-core" % dynoxideScalaCoreVersion,

    buildInfoKeys    := Seq[BuildInfoKey]("dynoxideDefaultVersion" -> dynoxideVersion),
    buildInfoPackage := "com.hlag.sbt.dynoxide",
  )
