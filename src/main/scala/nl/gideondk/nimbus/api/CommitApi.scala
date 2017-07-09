package nl.gideondk.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import nl.gideondk.nimbus.Connection
import nl.gideondk.nimbus.model._
import nl.gideondk.nimbus.serialization.NimbusSerialization
import spray.json.{RootJsonWriter, _}

import scala.concurrent.Future

object CommitApi extends NimbusSerialization {
  implicit val mutationResultFormat = jsonFormat3(MutationResult.apply)
  implicit val commitResponseFormat = jsonFormat2(CommitResponse.apply)

  implicit object CommitRequestJsonFormat extends RootJsonWriter[CommitRequest] {
    def write(c: CommitRequest) = {

      val mutationsAsJson = c.mutations.map { mutation =>
        mutation match {
          case x: Insert => JsObject("insert" -> x.entity.toJson)
          case x: Update => JsObject("update" -> x.entity.toJson)
          case x: Upsert => JsObject("upsert" -> x.entity.toJson)
          case x: Delete => JsObject("delete" -> x.key.toJson)
        }
      }

      JsObject(Map("mode" -> JsString(c.mode.toString), "mutations" -> JsArray(mutationsAsJson.toVector), "transaction" -> c.transactionId.map(x => JsString(x)).getOrElse(JsNull)).filter(x => x._2 != JsNull))
    }
  }


  object CommitMode extends Enumeration {
    val Transactional = Value("TRANSACTIONAL")
    val NonTransactional = Value("NON_TRANSACTIONAL")
  }

  case class MutationResult(key: Option[Key], version: String, conflictDetected: Option[Boolean])

  case class CommitRequest(mode: CommitMode.Value, mutations: Seq[Mutation], transactionId: Option[String])

  case class CommitResponse(mutationResults: Seq[MutationResult], indexUpdates: Option[Int])

}

trait CommitApi extends Connection {

  import CommitApi._

  def commit(transactionId: Option[String], mutations: Seq[Mutation], commitMode: CommitMode.Value): Future[CommitResponse] = {
    val uri: Uri = baseUri + ":commit"
    for {
      request <- Marshal(HttpMethods.POST, uri, CommitRequest(commitMode, mutations, transactionId)).to[HttpRequest]
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      entity <- handleErrorOrUnmarshal[CommitResponse](response)
    } yield {
      entity
    }
  }
}
