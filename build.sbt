val akkaCoreVersion = "2.5.14"
val akkaHttpVersion = "10.1.3"

val coreDeps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "0.18",
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.akka" %% "akka-actor" % akkaCoreVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaCoreVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaCoreVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "io.spray" %% "spray-json" % "1.3.4",
  "joda-time" % "joda-time" % "2.9.9"
)

val elasticsearchDeps = Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "7.3.0",
  "com.sksamuel.elastic4s" %% "elastic4s-client-akka" % "7.3.0"
)

val testDeps = Seq(
  "com.typesafe.akka" %% "akka-testkit" % "2.5.13" % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

val prodDeps = coreDeps ++ elasticsearchDeps

val allDeps = prodDeps ++ testDeps

val root = (project in file(".")).settings(
  name := "Snotify",
  organization := "com.xantoria",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.12.8",
  Compile / mainClass := Some("com.xantoria.snotify.Main"),
  libraryDependencies ++= allDeps,
  retrieveManaged := true,
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-Ywarn-unused:implicits",
    "-Ywarn-unused:imports",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates"
  ),
  testOptions += Tests.Setup { cl =>
    // Workaround to avoid slf4j complaining about multi-threading during initialisation
    // Gets a root logger at the start of test runs to force it to finish initialising first
    cl.loadClass("org.slf4j.LoggerFactory")
      .getMethod("getLogger", cl.loadClass("java.lang.String"))
      .invoke(null, "ROOT")
  }

)
