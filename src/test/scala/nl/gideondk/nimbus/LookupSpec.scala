/*
 * Copyright (c) 2017 Xebia Nederland B.V.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.gideondk.nimbus

import nl.gideondk.nimbus.api.CommitApi.CommitMode
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
        _ <- client.commit(Some(transactionId), mutations, CommitMode.Transactional)
        lookup <- client.lookup(ExplicitConsistency(ReadConsistency.Strong), keys)
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
        _ <- client.commit(Some(transactionId), mutations, CommitMode.Transactional)
        lookup <- client.lookup(ExplicitConsistency(ReadConsistency.Strong), keys)
      } yield {
        lookup.found.get.map(_.entity).apply(0) shouldEqual entities(0)
        lookup.missing.get.map(_.entity.key).apply(0) shouldEqual entities(1).key
        lookup.deferred shouldBe None
      }
    }
  }
}
