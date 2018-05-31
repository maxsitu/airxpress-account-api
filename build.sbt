name := """play-account-service"""
organization := "com.maxsitu"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.4"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.13.0",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.1",
  "be.objectify" %% "deadbolt-scala" % "2.6.0",
  "com.lightbend.akka" %% "akka-stream-alpakka-jms" % "0.19",
  "javax.jms" % "jms" % "1.1" % Provided,
  "org.apache.activemq" % "activemq-client" % "5.14.1"
)
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.maxsitu.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.maxsitu.binders._"
resolvers += Resolver.typesafeRepo("releases")
