package aphrodite.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.DateTime

trait JsonSupport extends SprayJsonSupport {

  import spray.json._
  import DefaultJsonProtocol._
  import aphrodite.entity.AphroditeEntity._

  //implicit val dateTimeFormat: JsonFormat[DateTime] = jsonFormat1(DateTime.apply)
  //implicit val imageFormat: RootJsonFormat[Image] = jsonFormat4(Image.apply)

}
