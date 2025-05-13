package aphrodite.entity

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import aphrodite.http.HttpService

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AphroditeServer {

  def start(
    interface: String,
    port: Int,
    system: ActorSystem[_],
    httpService: HttpService,
  ): Unit = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext

    val service: HttpRequest => Future[HttpResponse] = httpService.topLevelRoutes

    val bound =
      Http()
        .newServerAt(interface, port)
        .bind(service)
        .map(_.addToCoordinatedShutdown(3.seconds))

    bound.onComplete({
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Aphrodite online at REST server {}:{}",
          address.getHostString,
          address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind REST endpoint, terminating system", ex)
        system.terminate()
    })

  }

}
