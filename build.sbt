name := """play-account-service"""
organization := "com.maxsitu"

version := "1.0-SNAPSHOT"

lazy val V = new {
  val cats       = "1.2.0"
  val refined    = "0.9.0"
  val algebra    = "1.0.0"
  val atto       = "0.6.2"
  val kittens    = "1.1.0"
  val scalacheck = "1.13.5"
}

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    routesGenerator := InjectedRoutesGenerator,
    watchSources ++= (baseDirectory.value / "ui" ** "*").get,
    resolvers += Resolver.url("typesafe", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(
      Resolver.ivyStylePatterns),
  )

libraryDependencies ++= Seq(
  guice,
  ws,
  ehcache,
  "org.scalatestplus.play" %% "scalatestplus-play"      % "3.1.2" % Test,
  "org.reactivemongo"      %% "reactivemongo"           % "0.13.0",
  "com.github.t3hnar"      %% "scala-bcrypt"            % "3.1",
  "be.objectify"           %% "deadbolt-scala"          % "2.6.0",
  "com.lightbend.akka"     %% "akka-stream-alpakka-jms" % "0.19",
  "org.apache.activemq"    % "activemq-client"          % "5.14.1",
  "org.typelevel"          %% "cats-core"               % V.cats,
  "org.typelevel"          %% "cats-free"               % V.cats,
  "javax.jms"              % "jms"                      % "1.1" % Provided
)

resolvers += Resolver.typesafeRepo("releases")

scalaVersion in compile := "2.12.4"
scalacOptions in compile += "-Ypartial-unification"
