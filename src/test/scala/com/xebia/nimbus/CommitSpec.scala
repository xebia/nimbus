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

package com.xebia.nimbus

import com.xebia.nimbus.datastore.model._
import com.xebia.nimbus.datastore_api.CommitApi.CommitMode
import com.xebia.nimbus.datastore_model._

class CommitSpec extends WithClientSpec {
  def randomPostfix() = java.util.UUID.randomUUID().toString

  "A commit" should {
    "should throw an exception when the transaction ID isn't known" in {
      val entities = List(
        RawEntity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        RawEntity(Key.named(client.projectId, "Pet", "Cat" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
      )

      val mutations = entities.map(Insert.apply)

      recoverToSucceededIf[Exception] {
        client.commit(Some("ABC"), mutations, CommitMode.Transactional)
      }
    }

    "should handle inserts correctly" in {
      val entities = List(
        RawEntity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        RawEntity(Key.named(client.projectId, "Pet", "Cat" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
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
        RawEntity(Key.incomplete(client.projectId, "Pet"), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown")))),
        RawEntity(Key.incomplete(client.projectId, "Pet"), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Black"))))
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
        RawEntity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown"))))
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
        RawEntity(Key.named(client.projectId, "Pet", "Dog" + randomPostfix), Map("feet" -> Value(IntegerValue(4)), "color" -> Value(StringValue("Brown"))))
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
