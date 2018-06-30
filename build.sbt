val akkaHttpVersion = "10.1.1"

val coreDeps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "0.18",
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "io.spray" %% "spray-json" % "1.3.4",
  "joda-time" % "joda-time" % "2.9.9"
)

val elasticsearchDeps = Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "6.2.9",
  "com.sksamuel.elastic4s" %% "elastic4s-http" % "6.2.9"
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

val prodDeps = coreDeps ++ elasticsearchDeps

val allDeps = prodDeps ++ testDeps

val root = (project in file(".")).settings(
  name := "Snotify",
  organization := "com.xantoria",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.4",
  mainClass in Compile := Some("com.xantoria.snotify.Main"),
  libraryDependencies ++= allDeps,
  retrieveManaged := true,
  scalacOptions := Seq("-unchecked", "-deprecation")
)
