package nl.gideondk.nimbus

import nl.gideondk.nimbus.api.CommitApi.CommitMode
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
        client.commit(Some("ABC"), mutations, CommitMode.Transactional)
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
        action <- client.commit(Some(transactionId), mutations, CommitMode.Transactional)
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
        action <- client.commit(Some(transactionId), mutations, CommitMode.Transactional)
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
        insert <- client.commit(Some(transactionId), insertMutations, CommitMode.Transactional)
        transactionId <- client.beginTransaction()
        delete <- client.commit(Some(transactionId), deleteMutations, CommitMode.Transactional)
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
        delete <- client.commit(Some(transactionId), deleteMutations, CommitMode.Transactional)
      } yield {
        delete.indexUpdates shouldBe None
      }
    }
  }
}
