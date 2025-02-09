package testcontainers

import zio.*
import org.testcontainers.containers.{
  GenericContainer,
  MockServerContainer,
  Network
}
import org.testcontainers.utility.DockerImageName
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import testcontainers.ServiceDataSets.{
  BackgroundData,
  CareerData,
  ExpectedData,
  LocationData
}
import testcontainers.proxy.{
  inconsistentFailuresZ,
  jitter,
  allProxies
}
import zio.ZServiceBuilder

trait CareerHistoryServiceT:
  def citizenInfo(
      person: String
  ): ZIO[Any, Throwable | String, String]

object CareerHistoryHardcoded:
  val live: ZServiceBuilder[Has[CareerData], Nothing, Has[
    CareerHistoryServiceT
  ]] =
    for
      careerData <- ZServiceBuilder.service[CareerData]
    yield Has(
      CareerHistoryHardcoded(
        careerData.get,
        inconsistentFailuresZ *> jitter
      )
    )

class CareerHistoryHardcoded private (
    pairs: CareerData,
    proxyZ: ZIO[Any, Throwable | String, Unit] =
      ZIO.unit
) extends CareerHistoryServiceT:

  def citizenInfo(
      person: String
  ): ZIO[Any, Throwable | String, String] =
    for
      _ <- proxyZ
      res <-
        ZIO
          .fromOption(
            pairs
              .expectedData
              .find(_.userRequest == s"/$person")
              .map(_.response)
          )
          .mapError(_ =>
            new NoSuchElementException(
              s"No response for Person: $person"
            )
          )
    yield res
end CareerHistoryHardcoded

case class CareerHistoryServiceContainer(
    mockServerContainerZ: MockServerContainerZBasic
) extends CareerHistoryServiceT:

  def citizenInfo(
      person: String
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ.get(s"/$person")

object CareerHistoryService:
  def citizenInfo(person: String): ZIO[Has[
    CareerHistoryServiceT
  ], Throwable | String, String] =
    for
      careerHistoryService <-
        ZIO.service[CareerHistoryServiceT]
      info <-
        careerHistoryService.citizenInfo(person)
    yield info

  def constructContainered[T](
      pairs: CareerData, // TODO Make this part of the environment
      proxyZ: ZIO[
        Any,
        Throwable | String,
        Unit
      ] = ZIO.unit
  ): ZServiceBuilder[Has[
    Network
  ] & Has[Clock], Throwable, Has[
    CareerHistoryServiceT
  ]] =
    MockServerContainerZBasic
      .construct(
        "Career History",
        pairs.expectedData,
        proxyZ
      )
      .flatMap(x =>
        ZServiceBuilder.succeed(
          CareerHistoryServiceContainer(x.get)
        )
      )

  val live: ZServiceBuilder[Has[
    CareerData
  ] & Has[Network], Throwable, Has[
    CareerHistoryServiceT
  ]] =
    for
      data <- ZServiceBuilder.service[CareerData]
      webserver: Has[
        MockServerContainerZBasic
      ] <-
        MockServerContainerZBasic.construct(
          "Career History",
          data.get.expectedData,
          allProxies
        )
    yield Has(
      CareerHistoryServiceContainer(
        webserver.get
      )
    )

end CareerHistoryService

// TODO Convert to trait that doesn't
// unconditionally depend on a container
class LocationService(
    mockServerContainerZ: MockServerContainerZBasic
):

  def locationOf(
      person: String
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ.get(s"/$person")

object LocationService:
  def locationOf(person: String): ZIO[Has[
    LocationService
  ], Throwable | String, String] =
    for
      locationService <-
        ZIO.service[LocationService]
      info <- locationService.locationOf(person)
    yield info

  val live: ZServiceBuilder[Has[
    LocationData
  ] & Has[Network], Throwable, Has[
    LocationService
  ]] =
    for
      data <- ZServiceBuilder.service[LocationData]
      webserver: Has[
        MockServerContainerZBasic
      ] <-
        MockServerContainerZBasic.construct(
          "Location Service",
          data.get.expectedData
        )
    yield Has(LocationService(webserver.get))

end LocationService

class BackgroundCheckService(
    mockServerContainerZ: MockServerContainerZBasic
):

  def criminalHistoryOf(
      person: String
  ): ZIO[Any, Throwable | String, String] =
    mockServerContainerZ.get(s"/$person")

object BackgroundCheckService:
  def criminalHistoryOf(person: String): ZIO[Has[
    BackgroundCheckService
  ], Throwable | String, String] =
    for
      locationService <-
        ZIO.service[BackgroundCheckService]
      info <-
        locationService.criminalHistoryOf(person)
    yield s"Criminal:$info"

  val live: ZServiceBuilder[Has[
    BackgroundData
  ] & Has[Network], Throwable, Has[
    BackgroundCheckService
  ]] =
    for
      data <- ZServiceBuilder.service[BackgroundData]
      webserver: Has[
        MockServerContainerZBasic
      ] <-
        MockServerContainerZBasic.construct(
          "BackgroundCheck Service",
          data.get.expectedData
        )
    yield Has(
      BackgroundCheckService(webserver.get)
    )
end BackgroundCheckService
