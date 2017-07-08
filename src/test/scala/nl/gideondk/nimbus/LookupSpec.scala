package nl.gideondk.nimbus

import nl.gideondk.nimbus.model._

class LookupSpec extends NimbusSpec {
  def randomPostfix() = java.util.UUID.randomUUID().toString

  "Lookups" should {
    "should return found items for existing items" in {
      val entities = List(
        Entity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        Entity(Key.named(client.projectId, "Pet", "Cat" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
      )

      val mutations = entities.map(Insert.apply)
      val keys = entities.map(_.key)

      for {
        transactionId <- client.beginTransaction()
        _ <- client.commitTransactional(transactionId, mutations)
        lookup <- client.lookupWithStrongConsistency(keys)
      } yield {
        lookup.found.get.map(_.entity) shouldEqual entities
        lookup.missing shouldBe None
        lookup.deferred shouldBe None
      }
    }

    "should return missing items for non items" in {
      val entities = List(
        Entity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        Entity(Key.named(client.projectId, "Pet", "Cat" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
      )

      val mutations = entities.take(1).map(Insert.apply)
      val keys = entities.map(_.key)

      for {
        transactionId <- client.beginTransaction()
        _ <- client.commitTransactional(transactionId, mutations)
        lookup <- client.lookupWithStrongConsistency(keys)
      } yield {
        lookup.found.get.map(_.entity).apply(0) shouldEqual entities(0)
        lookup.missing.get.map(_.entity.key).apply(0) shouldEqual entities(1).key
        lookup.deferred shouldBe None
      }
    }
  }
}
