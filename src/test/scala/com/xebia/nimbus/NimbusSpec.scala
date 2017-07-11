package com.xebia.nimbus

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.xebia.nimbus.Path._
import com.xebia.nimbus.Query._
import com.xebia.nimbus.datastore.model.Value._

class NimbusSpec extends WithClientSpec {
  def randomPostfix() = java.util.UUID.randomUUID().toString

  case class Person(name: String, age: Int)

  implicit val personEntityFormatter = new EntityConverter[Person] {
    override def write(p: Person): Entity = Entity('$TestObject, p.name, Map("name" -> p.name, "age" -> p.age))

    override def read(entity: Entity): Person = Person(entity.properties("name").as[String], entity.properties("age").as[Int])
  }

  val mike = Person("Mike" + randomPostfix, 8)
  val nikky = Person("Nikky" + randomPostfix, 12)
  val bob = Person("Bob" + randomPostfix, 48)

  "Nimbus basic DSL" should {
    "correctly store objects" in {
      for {
        _ <- nimbus.insert(Seq(mike, nikky, bob))
        m <- nimbus.lookup[Person]('$TestObject -> mike.name)
        b <- nimbus.lookup[Person]('$TestObject -> bob.name)
      } yield {
        m.get.age shouldBe 8
        b.get.age shouldBe 48
      }
    }

    "correctly delete objects" in {
      for {
        _ <- nimbus.upsert(Seq(mike, nikky, bob))
        m <- nimbus.lookup[Person]('$TestObject -> mike.name)
        b <- nimbus.lookup[Person]('$TestObject -> bob.name)
        _ <- nimbus.delete('$TestObject -> bob.name)
        m2 <- nimbus.lookup[Person]('$TestObject -> mike.name)
        b2 <- nimbus.lookup[Person]('$TestObject -> bob.name)
      } yield {
        m.get.age shouldBe 8
        b.get.age shouldBe 48

        m2 shouldBe defined
        b2 should not be defined
      }
    }
  }
  "Nimbus query DSL" should {
    "correctly retrieve objects" in {
      for {
        _ <- nimbus.upsert(Seq(mike, nikky, bob))
        q <- nimbus.query[Person](Q.kindOf('$TestObject).filterBy('age > 6))
        q2 <- nimbus.query[Person](Q.kindOf('$TestObject).filterBy('age > 6 and 'age < 20))
        q3 <- nimbus.query[Person](Q.kindOf('$TestObject).filterBy('age > 6 and 'age < 20 and 'age > 10))
      } yield {
        q.results should contain theSameElementsAs Seq(mike, nikky, bob)
        q2.results should contain theSameElementsAs Seq(mike, nikky)
        q3.results should contain theSameElementsAs Seq(nikky)
      }
    }

    "correctly handle offsets, cursors and limits" in {
      for {
        _ <- nimbus.upsert(Seq(mike, nikky, bob))
        q <- nimbus.query[Person](Q.kindOf('$TestObject).withLimit(1))
        q2 <- nimbus.query[Person](Q.kindOf('$TestObject).withOffset(1))
        cursor = q.endCursor.get
        q3 <- nimbus.query[Person](Q.kindOf('$TestObject).startFrom(cursor))
      } yield {
        q.results should contain theSameElementsAs Seq(bob)
        q2.results should contain theSameElementsAs Seq(mike, nikky)
        q3.results should contain theSameElementsAs Seq(mike, nikky)
      }
    }

    "correctly sort items" in {
      for {
        _ <- nimbus.upsert(Seq(mike, nikky, bob))
        q <- nimbus.query[Person](Q.kindOf('$TestObject).orderDescBy('age))
        q2 <- nimbus.query[Person](Q.kindOf('$TestObject).orderAscBy('age))
      } yield {
        q.results.head shouldBe bob
        q.results.last shouldBe mike
        q2.results.head shouldBe mike
        q2.results.last shouldBe bob
      }
    }

    "correctly stream query results" in {
      implicit val system = ActorSystem()
      implicit val mat = ActorMaterializer()

      val entities = (0 until 1000).toList.map(x => Entity('$TestObject, Map("number" -> 0)))
      for {
        _ <- nimbus.upsert(entities)
        qs <- nimbus.querySource[Entity](Q.kindOf('$TestObject)).runWith(Sink.seq)
      } yield {
        qs.length shouldBe 1000
      }
    }
  }
}
