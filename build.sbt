lazy val deps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "0.2",
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
)

lazy val root = (project in file(".")).settings(
  name := "Snotify",
  organization := "com.xantoria",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.4",
  mainClass in Compile := Some("com.xantoria.snotify.Main"),
  libraryDependencies ++= deps,
  retrieveManaged := true
)
