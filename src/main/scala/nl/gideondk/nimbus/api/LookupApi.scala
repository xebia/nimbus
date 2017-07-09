package nl.gideondk.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import nl.gideondk.nimbus.Connection
import nl.gideondk.nimbus.model._
import nl.gideondk.nimbus.serialization.NimbusSerialization
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

object LookupApi extends NimbusSerialization {

  implicit val lookupRequestFormat = jsonFormat2(LookupRequest.apply)

  implicit val entityResultFormat = jsonFormat3(EntityResult.apply)

  implicit val lookupResponseFormat = jsonFormat3(LookupResponse.apply)


  case class LookupRequest(readOptions: ReadOption, keys: Seq[Key])

  case class EntityResult(entity: Entity, version: String, cursor: Option[String])

  case class LookupResponse(found: Option[Seq[EntityResult]], missing: Option[Seq[EntityResult]], deferred: Option[Seq[Key]])

}

trait LookupApi extends Connection {

  import LookupApi._

  def lookup(readOption: ReadOption, keys: Seq[Key]): Future[LookupResponse] = {
    val uri: Uri = baseUri + ":lookup"
    for {
      request <- Marshal(HttpMethods.POST, uri, LookupRequest(readOption, keys)).to[HttpRequest]
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      entity <- handleErrorOrUnmarshal[LookupResponse](response)
    } yield {
      entity
    }
  }
}