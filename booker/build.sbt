import scala.util.Try

// for building in a docker container
//graalVMNativeImageGraalVersion := Some("21.2.0")

GraalVMNativeImage / mainClass := Some("booker.run")

graalVMNativeImageCommand := (
  if (System.getProperty("os.name").toLowerCase.contains("win")) {
    val f = Try(file(System.getenv("JAVA_HOME")) / "lib" / "svm" / "bin" / "native-image.exe")
    f.filter(_.exists()).fold(_ => "native-image.exe", _.absolutePath)
  } else {
    val f = Try(file(System.getenv("JAVA_HOME")) / "lib" / "svm" / "bin" / "native-image")
    f.filter(_.exists()).fold(_ => "native-image", _.absolutePath)
  }
)

graalVMNativeImageOptions ++= (
  if (!System.getProperty("os.name").toLowerCase.contains("mac"))
    { Seq("--static") }
  else
    { Seq.empty }
)

graalVMNativeImageOptions ++= Seq(
  "--verbose",
  "--no-fallback",
  "--install-exit-handlers",
  "-H:+ReportExceptionStackTraces",
  "-H:Name=booker",
)

run / fork := true
run / baseDirectory := file(".")

//run / javaOptions += s"-agentlib:native-image-agent=config-output-dir=booker/src/main/resources/META-INF/native-image"

/*
// todo: run task with agent

lazy val runWithAgent = inputKey[Unit]("runWithAgent")

// todo: verify graalvm is the vm and the native-image-agent exists
runWithAgent := {
  val option = s"-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image"
  val newState = state.value.appendWithSession(Seq(run / javaOptions += option))
  //val mySettings = state.value.
  //val mySettings = inScope() inConfig(Compile)() Compile
  //run.evaluate(state.value.)
  //println(inConfig(Compile)(Seq.empty).)
  (Compile / run).evaluate()
  //Project.extract(newState).runTask(t, newState)
  //val (newNewState, output) = Project.extract(newState).runInputTask(ThisBuild / Compile / run, "", newState)
  //println(newNewState.onFailure)
  //(Compile / run).evaluated
  //output
}
 */
