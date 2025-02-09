package Hubs

import fakeEnvironmentInstances.FakeConsole
import zio.ZIO
import zio.*
import zio.Duration.*
import zio.Clock.*
import zio.Console.*

object ReadIntAndMultiply extends zio.App:

  def run(
      args: List[String]
  ) = // Use App's run function
    val logic =
      for
        hub <- Hub.bounded[Int](2)
        _ <-
          hub
            .subscribe
            .use { case hubSubscription =>
              val getAndStoreInput =
                for
                  _ <-
                    Console.printLine(
                      "Please provide an int"
                    )
                  input <- Console.readLine
                  nextInt = input.toInt
                  _ <- hub.publish(nextInt)
                yield ()

              val processNextIntAndPrint =
                for
                  nextInt <- hubSubscription.take
                  _ <-
                    Console.printLine(
                      "Multiplied Int: " +
                        nextInt * 5
                    )
                yield ()

              val reps = 5
              for
                _ <-
                  ZIO
                    .collectAllPar(
                      Set(
                        getAndStoreInput
                          .repeatN(reps),
                        processNextIntAndPrint
                          .forever
                      )
                    )
                    .timeout(5.seconds)
              yield ()
            }
      yield ()

    (
      for
        fakeConsole <-
          FakeConsole.withInput(
            "3",
            "5",
            "7",
            "9",
            "11",
            "13"
          )
        _ <-
          logic.provideCustomServices(
            Clock.live ++
              ZServiceBuilder.succeed(fakeConsole)
          )
      yield ()
    ).exitCode
  end run
end ReadIntAndMultiply
