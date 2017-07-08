package nl.gideondk.nimbus

import nl.gideondk.nimbus.model._

class CommitSpec extends NimbusSpec {
  def randomPostfix() = java.util.UUID.randomUUID().toString

  "A commit" should {
    "should throw an exception when the transaction ID isn't known" in {
      val entities = List(
        Entity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        Entity(Key.named(client.projectId, "Pet", "Cat" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
      )

      val mutations = entities.map(Insert.apply)

      recoverToSucceededIf[Exception] {
        client.commitTransactional("ABC", mutations)
      }
    }

    "should handle inserts correctly" in {
      val entities = List(
        Entity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        Entity(Key.named(client.projectId, "Pet", "Cat" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
      )

      val mutations = entities.map(Insert.apply)

      for {
        transactionId <- client.beginTransaction()
        action <- client.commitTransactional(transactionId, mutations)
      } yield {
        action.mutationResults.length shouldBe 2
        action.mutationResults.find(x => x.key.isDefined).isDefined shouldBe false
      }
    }

    "should handle inserts when no keys are supplied" in {
      val entities = List(
        Entity(Key.incomplete(client.projectId, "Pet"), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        Entity(Key.incomplete(client.projectId, "Pet"), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
      )

      val mutations = entities.map(Insert.apply)

      for {
        transactionId <- client.beginTransaction()
        action <- client.commitTransactional(transactionId, mutations)
      } yield {
        action.mutationResults.length shouldBe 2
        action.mutationResults.find(x => x.key.isDefined).isDefined shouldBe true
      }
    }

    "should handle deletes for existing keys" in {
      val entities = List(
        Entity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown"))))
      )

      val insertMutations = entities.map(Insert.apply)
      val deleteMutations = entities.map(_.key).map(Delete.apply)

      for {
        transactionId <- client.beginTransaction()
        insert <- client.commitTransactional(transactionId, insertMutations)
        transactionId <- client.beginTransaction()
        delete <- client.commitTransactional(transactionId, deleteMutations)
      } yield {
        insert.mutationResults.length shouldBe 1
        delete.mutationResults.length shouldBe 1
      }
    }

    "shouldn't apply changes for non-existing keys" in {
      val entities = List(
        Entity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown"))))
      )

      val deleteMutations = entities.map(_.key).map(Delete.apply)

      for {
        transactionId <- client.beginTransaction()
        delete <- client.commitTransactional(transactionId, deleteMutations)
      } yield {
        delete.indexUpdates shouldBe None
      }
    }
  }
}
