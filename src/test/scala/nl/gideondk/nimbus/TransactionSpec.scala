package nl.gideondk.nimbus

import nl.gideondk.nimbus.model.{Key, PathElementId}

class TransactionSpec extends NimbusSpec {
  "Allocation of IDs" should {
    "return the identity function of its input" in {
      val keys = Seq(Key.incomplete(projectId, "testEntity"), Key.incomplete(projectId, "testEntity"), Key.incomplete(projectId, "anotherTestEntity"))
      client.allocateIds(keys).map { response =>
        response.length shouldEqual 3
        response(0).path.length shouldEqual 1
        response(0).path(0).id.get.asInstanceOf[PathElementId].value should be > 0l
        response(1).path(0).id.get.asInstanceOf[PathElementId].value should be > 0l
        response(2).path(0).id.get.asInstanceOf[PathElementId].value should be > 0l
      }
    }
  }
}
