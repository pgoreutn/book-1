package handlingErrors

import zio.*
import zio.Console
import scala.util.Random
// A useful way of dealing with errors is by
// using the
// `orElse()` method.

case class file(name: String)

def loadFile(fileName: String) =
  if (Random.nextBoolean())
    println("First Attempt Successful")
    ZIO.succeed(file(fileName))
  else
    println("First Attemp Not Successful")
    ZIO.fail("File not found")

def loadBackupFile() =
  println("Backup file used")
  ZIO.succeed(file("BackupFile"))

object fallback extends zio.App:

  // orElse is a combinator that can be used to
  // handle
  // effects that can fail.

  def run(args: List[String]) =
    val loadedFile: UIO[file] =
      loadFile("TargetFile")
        .orElse(loadBackupFile())
    loadedFile.exitCode
