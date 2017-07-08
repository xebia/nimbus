package nl.gideondk.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import nl.gideondk.nimbus.Connection
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

trait TransactionApi extends Connection with DefaultJsonProtocol {

  case class BeginTransactionResponse(transaction: String)

  implicit val transactionResponseFormat = jsonFormat1(BeginTransactionResponse.apply)

  def beginTransaction(): Future[String] = {
    val uri: Uri = baseUri + ":beginTransaction"
    val request = HttpRequest.apply(HttpMethods.POST, uri)
    for {
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      transactionResponse <- handleErrorOrUnmarshal[BeginTransactionResponse](response)
    } yield transactionResponse.transaction
  }
}
