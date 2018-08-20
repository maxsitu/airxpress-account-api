import play.sbt.PlayImport.PlayKeys._

name := """play-account-service"""
organization := "com.maxsitu"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  routesGenerator := InjectedRoutesGenerator,
  watchSources ++= (baseDirectory.value / "ui" ** "*").get,
  resolvers += Resolver.url("typesafe", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns),
)

libraryDependencies ++= Seq(guice, ws, ehcache,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.reactivemongo" %% "reactivemongo"  % "0.13.0",
  "com.github.t3hnar" %% "scala-bcrypt"   % "3.1",
  "be.objectify"      %% "deadbolt-scala" % "2.6.0",
  "com.lightbend.akka"  %% "akka-stream-alpakka-jms" % "0.19",
  "org.apache.activemq" % "activemq-client" % "5.14.1",
  "javax.jms"           % "jms"             % "1.1" % Provided
)

resolvers += Resolver.typesafeRepo("releases")

scalaVersion in compile := "2.12.4"
