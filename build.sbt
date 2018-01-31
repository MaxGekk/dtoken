name := "dtoken"

version := "0.1"

scalaVersion := "2.12.4"

resolvers += Resolver.bintrayRepo("maxgekk", "maven")

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.github.scopt" %% "scopt" % "3.7.0",
  "default" %% "libnetrc" % "1.1",
  "default" %% "libricks" % "0.2"
)

enablePlugins(AssemblyPlugin)

mainClass in assembly := Some("DToken")
assemblyJarName in assembly := "dtoken.jar"
assemblyMergeStrategy in assembly := {
  case PathList("javax", "transaction", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", xs @ _*) => MergeStrategy.first
  case "plugin.xml" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
