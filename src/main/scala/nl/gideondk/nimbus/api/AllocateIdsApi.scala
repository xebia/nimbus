package nl.gideondk.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import nl.gideondk.nimbus.Connection
import nl.gideondk.nimbus.model.Key
import nl.gideondk.nimbus.serialization.NimbusSerialization
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

object AllocateIdsApi extends NimbusSerialization {

    implicit val allocateIdsRequestFormat = jsonFormat1(AllocateIdsRequest.apply)
    implicit val allocateIdsResponseFormat = jsonFormat1(AllocateIdsResponse.apply)

  case class AllocateIdsRequest(keys: Seq[Key])

  case class AllocateIdsResponse(keys: Seq[Key])
}
trait AllocateIdsApi extends Connection {
  import AllocateIdsApi._

  def allocateIds(keys: Seq[Key]): Future[Seq[Key]] = {
    val uri: Uri = baseUri + ":allocateIds"
    for {
      request <- Marshal(HttpMethods.POST, uri, AllocateIdsRequest(keys)).to[HttpRequest]
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      entity <- handleErrorOrUnmarshal[AllocateIdsResponse](response)
    } yield {
      entity.keys
    }
  }
}
