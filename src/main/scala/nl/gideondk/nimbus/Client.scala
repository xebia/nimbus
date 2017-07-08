package nl.gideondk.nimbus

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.{ActorMaterializer, OverflowStrategy}
import nl.gideondk.nimbus.Connection.AccessToken
import nl.gideondk.nimbus.api.TransactionApi

class Client(val accessToken: AccessToken,
             val projectId: String,
             val overflowStrategy: OverflowStrategy,
             val maximumInFlight: Int)(implicit val system: ActorSystem) extends Connection with TransactionApi {

  implicit val mat = ActorMaterializer()
  val apiHost = "www.googleapis.com"
  val apiPort = 443
  val datastoreAPIEndPoint = s"https://$apiHost/auth/datastore"
  val googleAPIEndPoint = s"https://$apiHost"
}

object Client {
  def apply(accessToken: AccessToken, projectId: String)(implicit system: ActorSystem) = {
    new Client(accessToken, projectId, OverflowStrategy.dropNew, 1024)
  }
}



