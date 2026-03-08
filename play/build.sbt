name := "slinq-play-demo"
organization := "io.github.karimagnusson"

version := "0.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.3.7"

libraryDependencies ++= Seq(
  guice,
  "org.postgresql" % "postgresql" % "42.7.3",
  "com.zaxxer"     % "HikariCP"   % "7.0.2"
  // "io.github.karimagnusson" %% "slinq-play" % "0.9.6-RC2"
  // Slinq JARs are provided as unmanaged dependencies in the lib/ folder.
)
