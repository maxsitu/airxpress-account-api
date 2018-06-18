name := """play-account-service"""
organization := "com.maxsitu"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  watchSources ++= (baseDirectory.value / "ui" ** "*").get
)

resolvers += Resolver.typesafeRepo("releases")

scalaVersion := "2.12.4"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo"  % "0.13.0",
  "com.github.t3hnar" %% "scala-bcrypt"   % "3.1",
  "be.objectify"      %% "deadbolt-scala" % "2.6.0",
  "com.lightbend.akka"  %% "akka-stream-alpakka-jms" % "0.19",
  "org.apache.activemq" % "activemq-client" % "5.14.1",
  "javax.jms"           % "jms"             % "1.1" % Provided,
)
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.maxsitu.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.maxsitu.binders._"
