name := """project"""

version := "1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.jsuereth" %% "scala-arm" % "1.4",
  "com.typesafe.akka" %% "akka-agent" % "2.4.11",
  "org.xerial" % "sqlite-jdbc" % "3.8.11.2",
  "org.springframework.security" % "spring-security-core" % "4.1.3.RELEASE"
)

