package nl.gideondk.nimbus

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import nl.gideondk.nimbus.Connection.{AccessToken, InputQueueClosed, InputQueueUnavailable}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object Connection {

  trait ConnectionException

  final case class InputQueueClosed() extends Exception with ConnectionException

  final case class InputQueueUnavailable() extends Exception with ConnectionException

  final case class IncorrectEventType[A](event: A) extends Exception with ConnectionException

  final case class OAuthResponse(access_token: String, token_type: String, expires_in: Int)

  final case class AccessToken(accessToken: String, expiresAt: Long)

}

trait ConnectionSettings {
  def apiHost: String

  def datastoreAPIEndPoint: String

  def googleAPIEndPoint: String

  def projectId: String

  def baseUri: Uri = s"$datastoreAPIEndPoint/v1/projects/$projectId"

  def maximumInFlight: Int
}

trait Connection extends ConnectionSettings {

  def datastoreAPIEndPoint: String

  def googleAPIEndPoint: String

  def projectId: String

  def accessToken: AccessToken

  private val poolClientFlow = Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](apiHost)

  private val requestQueue = Source.queue[(HttpRequest, Promise[HttpResponse])](maximumInFlight, OverflowStrategy.dropNew)
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
