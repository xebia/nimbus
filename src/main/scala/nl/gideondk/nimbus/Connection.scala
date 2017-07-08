package nl.gideondk.nimbus

import akka.actor.ActorSystem
import akka.http.javadsl.model.ResponseEntity
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import spray.json.DefaultJsonProtocol

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object Connection extends DefaultJsonProtocol {

  trait ConnectionException

  final case class InputQueueClosed() extends Exception with ConnectionException

  final case class InputQueueUnavailable() extends Exception with ConnectionException

  final case class IncorrectEventType[A](event: A) extends Exception with ConnectionException

  final case class AccessToken(accessToken: String, expiresAt: Long)

  final case class Error(code: Int, message: String)

  final case class ErrorResponse(error: Error)

  implicit val errorFormat = jsonFormat2(Error.apply)
  implicit val errorResponesFormat = jsonFormat1(ErrorResponse.apply)

  final case class DataStoreException(error: Error) extends Exception(error.message)

  def handleErrorOrUnmarshal[A](response: HttpResponse)(implicit um: Unmarshaller[ResponseEntity, A]) = {
    (if (response.status.isSuccess()) {
      Unmarshal(response.entity).to[A].map(Right.apply)
    } else {
      Unmarshal(response.entity).to[ErrorResponse].map(Left.apply)
    }).map(x =>
      x match {
        case Left(errorResponse) => throw new DataStoreException(errorResponse.error)
        case Right(response) => response
      }
    )
  }

}

trait ConnectionSettings {
  def apiHost: String

  def apiPort: Int

  def datastoreAPIEndPoint: String

  def googleAPIEndPoint: String

  def projectId: String

  def baseUri: Uri = s"$datastoreAPIEndPoint/v1/projects/$projectId"

  def maximumInFlight: Int

  def overflowStrategy: OverflowStrategy
}

trait Connection extends ConnectionSettings {

  import Connection._

  def datastoreAPIEndPoint: String

  def googleAPIEndPoint: String

  def projectId: String

  def accessToken: AccessToken

  implicit def system: ActorSystem

  implicit def mat: Materializer

  implicit def dispatcher = system.dispatcher

  private lazy val poolClientFlow =
    if (apiPort == 443) Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](apiHost, apiPort)
    else Http().cachedHostConnectionPool[Promise[HttpResponse]](apiHost, apiPort)

  private lazy val requestQueue = Source.queue[(HttpRequest, Promise[HttpResponse])](maximumInFlight, overflowStrategy)
    .via(poolClientFlow)
    .toMat(Sink.foreach({
      case ((Success(resp), p)) => p.success(resp)
      case ((Failure(e), p)) => p.failure(e)
    }))(Keep.left)
    .run

  def singleRequest(request: HttpRequest) = {
    val context = Promise[HttpResponse]()
    requestQueue.offer((request, context)).flatMap { offerResult =>
      offerResult match {
        case QueueOfferResult.Dropped ⇒ Future.failed(InputQueueUnavailable())
        case QueueOfferResult.QueueClosed ⇒ Future.failed(InputQueueClosed())
        case QueueOfferResult.Failure(reason) ⇒ Future.failed(reason)
        case QueueOfferResult.Enqueued ⇒ context.future
      }
    }
  }
}
