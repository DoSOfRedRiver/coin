name := "coin"

version := "0.1"

scalaVersion := "2.12.4"

scalacOptions += "-Ypartial-unification"

libraryDependencies += "org.typelevel" %% "cats-core" % "1.0.1"
libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")
