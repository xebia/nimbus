package nl.gideondk.nimbus

import nl.gideondk.nimbus.api.CommitApi.CommitMode
import nl.gideondk.nimbus.api.QueryApi.Filter.PropertyFilter
import nl.gideondk.nimbus.api.QueryApi._
import nl.gideondk.nimbus.model.Value._
import nl.gideondk.nimbus.model._

class QuerySpec extends NimbusSpec {
  def randomPostfix() = java.util.UUID.randomUUID().toString

  "Queries" should {
    "return found items on basis of property filters" in {
      val entities = List(
        Entity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        Entity(Key.named(client.projectId, "Pet", "Cat" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
      )

      val mutations = entities.map(Insert.apply)
      val keys = entities.map(_.key)

      val query = Query(None, Some(Seq("Pet")), Some(PropertyFilter("color", PropertyOperator.Equal, "Brown")), None, None, None, None, None, None)
      for {
        transactionId <- client.beginTransaction()
        _ <- client.commit(transactionId, mutations, CommitMode.Transactional)
        query <- client.query(PartitionId(client.projectId), ExplicitConsistency(ReadConsistency.Strong), query)
      } yield {
        val entityResults = query.batch.entityResults.get
        entityResults.find(x => x.entity.key == entities(0).key).map(_.entity) shouldEqual Some(entities(0))
        entityResults.find(x => x.entity.key == entities(1).key).isDefined shouldEqual false
      }
    }
  }
}
