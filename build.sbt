name := "backlog-user-migration"

lazy val commonSettings = Seq(
  version := "0.0.1",
  scalaVersion := "2.12.4"
)

lazy val backlog4s_core = (project in file("backlog4s/backlog4s-core"))
  .settings(commonSettings)

lazy val backlog4s_akka = (project in file("backlog4s/backlog4s-akka"))
  .settings(commonSettings)
  .dependsOn(backlog4s_core)

lazy val commandLine = (project in file("command-line"))
    .settings(commonSettings)

lazy val backlogUserMigration = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"    % "10.0.11",
      "com.typesafe.akka" %% "akka-stream"  % "2.5.8",
      "org.scalatest"     %% "scalatest"    % "3.0.1"     % "test"
    )
  )
  .dependsOn(backlog4s_core, backlog4s_akka, commandLine)