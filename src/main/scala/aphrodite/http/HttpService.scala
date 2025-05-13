package aphrodite.http

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import aphrodite.entity.AphroditeEntity

import scala.concurrent.duration._

class HttpService(implicit system: ActorSystem[_]) {

  implicit val timeout: Timeout = 3.seconds
  private val sharding = ClusterSharding(system)

  lazy val topLevelRoutes =
    pathPrefix("api") {
      concat(
        pathPrefix("v1") {
          imageRoutes
        }
      )
    }


  lazy val imageRoutes: Route =
    pathPrefix("images") {
      concat(
        pathEnd(
          concat(
/*            post {
              entity(as[AphroditeEntity.Image]) { entity =>
                val entityRef = sharding.entityRefFor(AphroditeEntity.EntityKey, "image")
                complete("success")
              }
            },*/
            get {
              complete("Hello World Again!")
            }
          )
        )
      )
    }


}
