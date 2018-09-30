name := """airxpress-accout-api"""
organization := "com.maxsitu"

version := "1.0-SNAPSHOT"

lazy val V = new {
  val scalabcrypt        = "3.1"
  val scalacheck         = "1.13.5"
  val swaggerui          = "2.2.0"
  val reactivemongo      = "0.16.0"
  val deadboltscala      = "2.6.0"
  val scalatestplus_play = "3.1.2"
}

lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin)

swaggerDomainNameSpaces := Seq("model")
routesGenerator := InjectedRoutesGenerator

resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  guice,
  ws,
  ehcache,
  "org.webjars"            % "swagger-ui"            % "3.19.0",
  "io.swagger"             %% "swagger-scala-module" % "1.0.5-SNAPSHOT",
  "org.reactivemongo"      %% "reactivemongo"        % V.reactivemongo,
  "com.github.t3hnar"      %% "scala-bcrypt"         % V.scalabcrypt,
  "be.objectify"           %% "deadbolt-scala"       % V.deadboltscala,
  "org.scalatestplus.play" %% "scalatestplus-play"   % V.scalatestplus_play % Test,
)

swaggerV3 := true
dockerExposedPorts := Seq(9000)
(stage in Docker) := (stage in Docker).dependsOn(swagger).value

scalaVersion in compile := "2.12.6"
scalacOptions in compile += "-Ypartial-unification"
