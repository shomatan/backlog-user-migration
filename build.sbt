name := "backlog-user-migration"

lazy val commonSettings = Seq(
  version := "0.0.1",
  scalaVersion := "2.12.4"
)

lazy val backlog4s = (project in file("backlog4s"))
  .settings(commonSettings)

lazy val backlogUserMigration = (project in file("."))
  .settings(commonSettings)
  .dependsOn(backlog4s)