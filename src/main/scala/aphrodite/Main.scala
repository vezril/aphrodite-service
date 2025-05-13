package aphrodite

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import aphrodite.entity.{AphroditeEntity, AphroditeServer}
import aphrodite.http.HttpService
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

object Main {
  val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "aphrodite-service")

    try {
      // val aphroditeService = ???
      init(system)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization error.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_]): Unit = {
    logger.info("Starting Aphrodite Service...")
    logger.info("Starting Akka Management...")
    AkkaManagement(system).start()
    logger.info("Starting Cluster Bootstrap...")
    ClusterBootstrap(system).start()

    logger.info("Starting Aphrodite Entity...")
    AphroditeEntity.init(system)

    logger.info("Starting HTTP Service...")
    val httpInterface = system.settings.config.getString("aphrodite-service.http.interface")
    val httpPort = system.settings.config.getInt("aphrodite-service.http.port")

    val httpService = new HttpService()(system)
    logger.info("Starting Aphrodite Server...")
    AphroditeServer.start(httpInterface, httpPort, system, httpService)
  }

}
