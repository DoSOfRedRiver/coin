name := "coin"
version := "0.1"

scalaOrganization := "org.typelevel"
scalaVersion := "2.12.4-bin-typelevel-4"

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-Yinduction-heuristics" ,
  "-Xfatal-warnings",
  "-Xstrict-patmat-analysis",
  "-language:higherKinds",
  "-deprecation"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")

val monocleVersion = "1.5.1-cats"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

mainClass in assembly := Some("coin.Main")

val monocle = Seq(
  "com.github.julien-truffaut" %%  "monocle-core"  % monocleVersion,
  "com.github.julien-truffaut" %%  "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %%  "monocle-law"   % monocleVersion % "test"
)

libraryDependencies ++= (
  Seq(
    "org.typelevel" %% "cats-core" % "1.2.0",
    "org.typelevel" %% "cats-effect" % "1.0.0-RC3",
    "com.chuusai" %% "shapeless" % "2.3.3",
    "org.scalactic" %% "scalactic" % "3.0.5",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  ) ++ monocle
)
