package zioBasics

import java.io.IOException
import zio.Console.{readLine, printLine}
import fakeEnvironmentInstances.FakeConsole
import zio.Console
import zio.{
  Has,
  IO,
  Runtime,
  ZIO,
  ZServiceBuilder,
  durationInt
}

@main
def hello =
  val a = printLine("1")
  val s: ZIO[Any, Nothing, String] =
    ZIO.succeed {
      println("2");
      "asdf";
    }
  val h: ZIO[Has[Console], IOException, Unit] =
    s.flatMap { ss =>
      println("3");
      printLine(ss);
    }
  println("4")
  Runtime.default.unsafeRunSync(h)

@main
def scheduling =
  import zio.Schedule
  import zio.Duration.*

  val scheduledCode =
    for
      _ <- printLine("Looping. Give me a word")
      input <- readLine
      _ <-
        if (input != "boom")
          printLine("Fine. Keep going.")
        else
          ZIO.fail("errrgggg")
    yield ()

  Runtime
    .default
    .unsafeRunSync(
      scheduledCode
        .provideServices(
          ZServiceBuilder.succeed(FakeConsole.word)
        )
        .repeat(
          Schedule.recurs(4) &&
            Schedule.spaced(3.seconds)
        )
    )
end scheduling

@main
def ValPassing(): Unit =

  val input
      : ZIO[Has[Console], IOException, String] =
    for
      _    <- printLine("What is your name?")
      name <- readLine
      _    <- printLine(s"Hello, ${name}")
    yield name

  def process(input: String): Boolean =
    if (input == "Bob")
      true
    else
      false

  val b
      : ZIO[Has[Console], IOException, Boolean] =
    input.map(i => process(i))

  println(
    Runtime
      .default
      .unsafeRunSync(
        b.provideServices(
            ZServiceBuilder.succeed(FakeConsole.name)
          )
          .exitCode
      )
  )
end ValPassing
