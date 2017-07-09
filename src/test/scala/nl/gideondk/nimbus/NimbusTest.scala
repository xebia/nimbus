package nl.gideondk.nimbus

import akka.actor.ActorSystem
import nl.gideondk.nimbus.api.QueryApi.Query
import nl.gideondk.nimbus.model.{Delete, ExplicitConsistency, PartitionId, ReadConsistency}
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._

trait NimbusSpec extends AsyncWordSpec with Matchers with BeforeAndAfter {
  val projectId = "nimbus-test"
  implicit val system = ActorSystem()

  val client = new TestClient(projectId)

}
