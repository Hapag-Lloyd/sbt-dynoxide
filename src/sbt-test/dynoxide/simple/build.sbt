ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .enablePlugins(DynoxidePlugin)
  .settings(
    name         := "dynoxide-scripted-simple",
    publish / skip := true,
    dynoxidePort := 18000,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++= Seq(
      "org.scalameta"          %% "munit"    % "1.0.4"  % Test,
      "software.amazon.awssdk"  % "dynamodb" % "2.29.52" % Test
    )
  )
