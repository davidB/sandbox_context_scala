val play24Version     = "2.4.8"
val braveVersion      = "4.0.6"
val opentracingVersion = "0.20.7"
val akkaVersion        = "2.4.16"

//play 2.4.x
val playTest24        = "org.scalatestplus"         %%  "play"                  % "1.4.0"
val scalatest         = "org.scalatest"             %%  "scalatest"             % "2.2.6"

lazy val root = Project("context-lib0", file("."))
  .settings(Seq(
      moduleName := "aop-play-2.4",
      scalaVersion := "2.11.8",
      //crossScalaVersions := Seq("2.11.8"),
      testGrouping in Test := singleTestPerJvm((definedTests in Test).value, (javaOptions in Test).value)
  ))
  .settings(aspectJSettings: _*)
  .settings(
    libraryDependencies ++=
      compileScope(
        "io.zipkin.brave" % "brave-mysql" % braveVersion,
        "io.zipkin.brave" % "brave-okhttp" % braveVersion,
        "io.zipkin.brave" % "brave-p6spy" % braveVersion,
        "io.opentracing.brave" % "brave-opentracing" % "0.18.1",
        "io.opentracing" % "opentracing-api" % opentracingVersion,
        "io.opentracing" % "opentracing-noop" % opentracingVersion,
        "io.opentracing" % "opentracing-impl" % opentracingVersion
      ) ++
      providedScope(
          aspectJ,
          //"com.typesafe" % "config" % "1.2.1"
          "com.typesafe.akka" %% "akka-actor" % akkaVersion,
          "com.typesafe.play" %% "play" % play24Version,
          "com.typesafe.play" %% "play-ws" % play24Version
      ) ++
      testScope(playTest24))


 import sbt.Tests._
 def singleTestPerJvm(tests: Seq[TestDefinition], jvmSettings: Seq[String]): Seq[Group] =
   tests map { test =>
     Group(
       name = test.name,
       tests = Seq(test),
       runPolicy = SubProcess(ForkOptions(runJVMOptions = jvmSettings)))
   }
