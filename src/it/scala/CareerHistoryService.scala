package mdoc

import zio.*
import org.testcontainers.containers.{
  GenericContainer,
  Network
}
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.utility.DockerImageName
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response

class CareerHistoryService(mockServerContainerZ: MockServerContainerZ):

    def citizenInfo(person: Person): ZIO[Any, Throwable | String, String] =
      mockServerContainerZ.get(s"/person/${person.firstName}")

object CareerHistoryService:
  def citizenInfo(person: Person): ZIO[Has[CareerHistoryService], Throwable | String, String] =
    for {
      careerHistoryService <- ZIO.service[CareerHistoryService]
      info <- careerHistoryService.citizenInfo(person)
    } yield  info

  def construct[T](
      pairs: List[RequestResponsePair],
  ): ZLayer[Has[Network], Throwable, Has[
    CareerHistoryService
  ]] =

    MockServerContainerZ.construct(pairs).flatMap(x => ZLayer.succeed(CareerHistoryService(x.get)))