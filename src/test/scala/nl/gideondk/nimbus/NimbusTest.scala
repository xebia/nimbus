package nl.gideondk.nimbus

import akka.actor.ActorSystem
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers, WordSpec}
import org.scalatest.concurrent.AsyncAssertions

trait NimbusSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll  {
  val projectId = "nimbus-test"
  implicit val system = ActorSystem()

  val client = new TestClient(projectId)
}
