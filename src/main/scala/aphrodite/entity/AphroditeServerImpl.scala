package aphrodite.entity

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class AphroditeServerImpl(system: ActorSystem[_]) {

  import system.executionContext

  private val logger = LoggerFactory.getLogger(getClass)

  implicit private val timeout: Timeout = Timeout.create(system.settings.config.getDuration("aphrodite-service.ask-timeout"))

  private val sharding = ClusterSharding(system)

  def addImage(in: String): Future[String] = {
    logger.info("addImage {} to aphrodite", in)
    Future.successful(in)
  }

}
