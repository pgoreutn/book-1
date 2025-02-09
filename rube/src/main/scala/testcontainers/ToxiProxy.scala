package testcontainers

import eu.rekawek.toxiproxy.model.ToxicDirection
import org.testcontainers.containers.{
  GenericContainer,
  KafkaContainer,
  MockServerContainer,
  Network,
  ToxiproxyContainer
}
import org.testcontainers.utility.DockerImageName
import zio.{Has, ZIO}

import scala.jdk.CollectionConverters.*
import zio.ZServiceBuilder

object ToxyProxyContainerZ:
  val TOXIPROXY_NETWORK_ALIAS = "toxiproxy"
  def apply(
      network: Network
  ): ToxiproxyContainer =
    val TOXIPROXY_IMAGE =
      DockerImageName
        .parse("shopify/toxiproxy:2.1.0");
    // Toxiproxy container, which will be used as
    // a TCP proxy
    new ToxiproxyContainer(TOXIPROXY_IMAGE)
      .nn
      .withNetwork(network)
      .nn
      .withNetworkAliases(
        TOXIPROXY_NETWORK_ALIAS
      )
      .nn

  def construct(): ZServiceBuilder[Has[
    Network
  ] & Has[NetworkAwareness], Throwable, Has[
    ToxiproxyContainer
  ]] =
    for
      network <-
        ZServiceBuilder.service[Network].map(_.get)
      container = apply(network)
      res <-
        GenericInteractionsZ
          .manageWithInitialization(
            container,
            "toxi"
          )
          .toServiceBuilder
    yield res

  def createProxiedLink(
      toxiproxyContainer: ToxiproxyContainer,
      mockServerContainer: MockServerContainer
  ) =

    println("Using")

    /* toxiproxyContainer .getBoundPortNumbers
     * .nn .forEach(x => println(x.nn)) while
     * (!toxiproxyContainer.isHealthy) {
     * println("Waiting for health check") } */

    val proxy
        : ToxiproxyContainer.ContainerProxy =
      toxiproxyContainer
        .getProxy(
          mockServerContainer,
          mockServerContainer.getServerPort.nn
        )
        .nn

//    proxy
//      .toxics()
//      .nn
//      .latency(
//        "latency",
//        ToxicDirection.DOWNSTREAM,
//        1_100
//      )

    println(
      "Hopefully setup Proxy target: " +
        mockServerContainer.getServerPort.nn
    )

    println(
      "Proxy target on underlying service: " +
        proxy.getOriginalProxyPort.nn
    )
    proxy.getProxyPort.nn
//    proxy.getOriginalProxyPort.nn
  end createProxiedLink
end ToxyProxyContainerZ
