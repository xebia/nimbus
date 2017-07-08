package nl.gideondk.nimbus.api

import akka.http.javadsl.model.ResponseEntity
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import nl.gideondk.nimbus.Connection
import nl.gideondk.nimbus.Connection.{DataStoreException, ErrorResponse}
import nl.gideondk.nimbus.model.Key
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

trait AllocateIdsApi extends Connection with DefaultJsonProtocol {

  import nl.gideondk.nimbus.serialization.Serialization._

  case class AllocateIdsRequest(keys: Seq[Key])

  case class AllocateIdsResponse(keys: Seq[Key])

  implicit val allocateIdsRequestFormat = jsonFormat1(AllocateIdsRequest.apply)
  implicit val allocateIdsResponseFormat = jsonFormat1(AllocateIdsResponse.apply)

  def allocateIds(keys: Seq[Key]): Future[Seq[Key]] = {
    val uri: Uri = baseUri + ":allocateIds"
    for {
      request <- Marshal(HttpMethods.POST, uri, AllocateIdsRequest(keys)).to[HttpRequest]
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      entity <- Connection.handleErrorOrUnmarshal[AllocateIdsResponse](response)
    } yield {
      entity.keys
    }
  }
}
