package mdoc

import zio.Runtime.default.unsafeRun
import zio.{Console, ZEnv, ZIO}

def wrapUnsafeZIO[E, A](
    z: => ZIO[zio.ZEnv, E, A]
): ZIO[
  zio.ZEnv,
  E | java.io.IOException,
  A | Unit
] =
  val commentPrefix = "// "
  val columnWidth =
    49 -
      commentPrefix
        .length // TODO Pull from scalafmt config file
  val defectPrefix = "Defect: "
  val topLineLength =
    columnWidth - defectPrefix.length
  z.catchAllDefect { case ex: Throwable =>
    for
      _ <-
        Console.printLine(
          s"$defectPrefix${ex.getClass}".take(
            topLineLength
          ) // Less, because we have to account for the comment prefix "// "
        )
      msg = ex.getMessage
      extractedMessage =
        if (msg != null && msg.nonEmpty)
          if (msg.contains("$"))
            msg
              .split("\\$")
              .nn
              .last
              .nn
              .replace(")", "")
              .nn
          else
            msg
        else
          ""
      indentedMsg =
        (" " * defectPrefix.length) +
          extractedMessage
      formattedMsg =
        if (indentedMsg.length > topLineLength)
          indentedMsg.take(topLineLength)
        else
          indentedMsg
      _ <- Console.printLine(formattedMsg)
    yield ()
  }
end wrapUnsafeZIO

// Needs to handle messages like this:
// repl.MdocSession$App$GpsException (of class
// repl.MdocSession$App$GpsException)
def unsafeRunTruncate[E, A](
    z: => ZIO[zio.ZEnv, E, A]
): A | Unit = unsafeRun(wrapUnsafeZIO(z))

// TODO Print successful result also
def wrapUnsafeZIOReportError[E, A](
    z: => ZIO[zio.ZEnv, E, A]
): ZIO[zio.ZEnv, java.io.IOException, A | Unit] =
  val commentPrefix = "// "
  val columnWidth =
    49 -
      commentPrefix
        .length // TODO Pull from scalafmt config file
  val defectPrefix = "Error: "
  val topLineLength =
    columnWidth - defectPrefix.length
  z.catchAll { case error: E =>
    val extractedMessage = error.toString
    val formattedMsg =
      if (
        extractedMessage.length > topLineLength
      )
        extractedMessage.take(topLineLength)
      else
        extractedMessage

    Console.printLine(formattedMsg)
  }
end wrapUnsafeZIOReportError

def unsafeRunPrettyPrint[E, A](
    z: => ZIO[zio.ZEnv, E, A]
): A | Unit =
  unsafeRun(wrapUnsafeZIOReportError(z))
