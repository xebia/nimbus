package nl.gideondk.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import nl.gideondk.nimbus.Connection
import nl.gideondk.nimbus.model.{Entity, Key}
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.Future

trait LookupApi extends Connection with DefaultJsonProtocol {

  import nl.gideondk.nimbus.serialization.Serialization._

  trait ReadOption

  object ReadConsistency extends Enumeration {
    val Strong = Value("STRONG")
    val Eventual = Value("EVENTUAL")
  }

  case class TransactionConsistency(transaction: String) extends ReadOption

  case class ExplicitConsistency(readConsistency: ReadConsistency.Value) extends ReadOption


  implicit object ReadOptionJsonFormat extends RootJsonFormat[ReadOption] {
    def write(c: ReadOption) = {
      c match {
        case x: TransactionConsistency => JsObject("transaction" -> JsString(x.transaction))
        case x: ExplicitConsistency => JsObject("readConsistency" -> JsString(x.readConsistency.toString))
      }
    }

    def read(js: JsValue) = ???
  }

  case class LookupRequest(readOptions: ReadOption, keys: Seq[Key])

  case class EntityResult(entity: Entity, version: String, cursor: Option[String])

  case class LookupResponse(found: Option[Seq[EntityResult]], missing: Option[Seq[EntityResult]], deferred: Option[Seq[Key]])

  implicit val lookupRequestFormat = jsonFormat2(LookupRequest.apply)

  implicit val entityResultFormat = jsonFormat3(EntityResult.apply)

  implicit val lookupResponseFormat = jsonFormat3(LookupResponse.apply)

  private def lookup(readOption: ReadOption, keys: Seq[Key]): Future[LookupResponse] = {
    val uri: Uri = baseUri + ":lookup"
    for {
      request <- Marshal(HttpMethods.POST, uri, LookupRequest(readOption, keys)).to[HttpRequest]
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      entity <- handleErrorOrUnmarshal[LookupResponse](response)
    } yield {
      entity
    }
  }

  def lookupWithinTransaction(transactionId: String, keys: Seq[Key]): Future[LookupResponse] =
    lookup(TransactionConsistency(transactionId), keys)

  def lookupWithEventualConsistency(keys: Seq[Key]): Future[LookupResponse] =
    lookup(ExplicitConsistency(ReadConsistency.Eventual), keys)

  def lookupWithStrongConsistency(keys: Seq[Key]): Future[LookupResponse] =
    lookup(ExplicitConsistency(ReadConsistency.Strong), keys)
}