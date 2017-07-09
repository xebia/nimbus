import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Keys.{version, _}

val AkkaVersion = "2.5.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "com.typesafe.akka" %% "akka-http-core" % "10.0.5",
  "com.typesafe.akka" %% "akka-http" % "10.0.5",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.5",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test")

def common: Seq[Setting[_]] = SbtScalariform.scalariformSettings ++ Seq(
  organization := "com.xebia",
  organizationName := "Xebia Nederland B.V.",
  startYear := Some(2017),
  licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT"))),

  crossScalaVersions := Seq("2.11.11", "2.12.2"),
  scalaVersion := crossScalaVersions.value.head,
  crossVersion := CrossVersion.binary,

  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    //"-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Xfuture"
  ),

  // show full stack traces and test case durations
  testOptions in Test += Tests.Argument("-oDF"),

  // -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
  // -a Show stack traces and exception class name for AssertionErrors.
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),

  // disable parallel tests
  parallelExecution in Test := false,

  ScalariformKeys.preferences in Compile := formattingPreferences,
  ScalariformKeys.preferences in Test := formattingPreferences
)

lazy val root = (project in file("."))
  .settings(common: _*)
  .settings(
    name := """nimbus""",

    version := "0.1"
  )


def formattingPreferences = {
  import scalariform.formatter.preferences._
  FormattingPreferences()
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(SpacesAroundMultiImports, true)
}