package nl.gideondk.nimbus

import nl.gideondk.nimbus.model.{Key, PathElementId}

class TransactionSpec extends NimbusSpec {
  "A transaction" should {
    "should initialize correctly with a transaction identifier" in {
      client.beginTransaction.map { response =>
        response.length should be > 0
      }
    }
  }
}
