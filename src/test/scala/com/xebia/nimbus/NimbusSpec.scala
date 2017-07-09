package com.xebia.nimbus

import com.xebia.nimbus.datastore_api.CommitApi.CommitMode
import com.xebia.nimbus.datastore_model._

class NimbusSpec extends WithClientSpec {
  def randomPostfix() = java.util.UUID.randomUUID().toString

  val namespace = "NimbusTest"

  val nimbus = new Nimbus(namespace, client)

  "The Nimbus client" should {
    "correctly store objects" in {
      for {
        nimbus.insert()
      }
    }
  }
}
