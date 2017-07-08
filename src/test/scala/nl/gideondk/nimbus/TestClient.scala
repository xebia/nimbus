package nl.gideondk.nimbus

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.{ActorMaterializer, OverflowStrategy}
import nl.gideondk.nimbus.Connection.AccessToken
import nl.gideondk.nimbus.api.{AllocateIdsApi, TransactionApi}

class TestClient(val projectId: String, val maximumInFlight: Int = 1024)(implicit val system: ActorSystem) extends Connection with TransactionApi with AllocateIdsApi {

  implicit val mat = ActorMaterializer()

  val overflowStrategy = OverflowStrategy.backpressure
  val accessToken = AccessToken("LOCAL", 0)
  val apiHost = "localhost"
  val apiPort = 8080
  val datastoreAPIEndPoint = s"http://localhost:8080"
  val googleAPIEndPoint = s"http://localhost:8080"
}