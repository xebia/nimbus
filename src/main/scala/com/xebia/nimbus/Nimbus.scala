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

import com.xebia.nimbus.api.CommitApi.{CommitMode, MutationResult}
import com.xebia.nimbus.model._

import scala.concurrent.Future

class Nimbus(client: Client) {
  implicit val dispatcher = client.system.dispatcher

  def beginTransaction() =
    client.beginTransaction()

  def commitTransactional(transactionId: String, mutations: Seq[Mutation]): Future[Seq[MutationResult]] =
    client.commit(Some(transactionId), mutations, CommitMode.Transactional).map(_.mutationResults)

  def commitNonTransactional(mutations: Seq[Mutation]): Future[Seq[MutationResult]] =
    client.commit(None, mutations, CommitMode.NonTransactional).map(_.mutationResults)

  /** CRUD **/

  def insertTransactional(transactionId: String, entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(Insert.apply))

  def insert(entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(Insert.apply))

  def updateTransactional(transactionId: String, entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(Update.apply))

  def update(entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(Update.apply))

  def upsertTransactional(transactionId: String, entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(Upsert.apply))

  def upsert(entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(Upsert.apply))

  def deleteTransactional(transactionId: String, keys: Seq[Key]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, keys.map(Delete.apply))

  def delete(keys: Seq[Key]): Future[Seq[MutationResult]] =
    commitNonTransactional(keys.map(Delete.apply))

  /** Lookup **/

  case class LookupResult(found: Seq[Entity], missing: Seq[Key])

  def lookupWithinTransaction(transactionId: String, keys: Seq[Key]): Future[LookupResult] =
    client.lookup(TransactionConsistency(transactionId), keys).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(_.entity.key)))

  def lookupWithinTransaction(transactionId: String, key: Key): Future[Option[Entity]] =
    client.lookup(TransactionConsistency(transactionId), Seq(key)).map(response => response.found.flatMap(_.headOption.map(_.entity)))

  def lookupWithEventualConsistency(keys: Seq[Key]): Future[LookupResult] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), keys).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(_.entity.key)))

  def lookupWithEventualConsistency(key: Key): Future[Option[Entity]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), Seq(key)).map(response => response.found.flatMap(_.headOption.map(_.entity)))

  def lookupWithStrongConsistency(keys: Seq[Key]): Future[LookupResult] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), keys).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(_.entity.key)))

  def lookupWithStrongConsistency(key: Key): Future[Option[Entity]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), Seq(key)).map(response => response.found.flatMap(_.headOption.map(_.entity)))

  def lookup(keys: Seq[Key]) = lookupWithEventualConsistency(keys)

  def lookup(key: Key) = lookupWithEventualConsistency(key)

  /** Query **/

  def queryWithinTransaction(transactionId: String, keys: Seq[Key]): Future[LookupResult] =
    client.lookup(TransactionConsistency(transactionId), keys).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(_.entity.key)))

  def queryWithEventualConsistency(keys: Seq[Key]): Future[LookupResult] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), keys).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(_.entity.key)))

  def queryWithStrongConsistency(keys: Seq[Key]): Future[LookupResult] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), keys).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(_.entity.key)))

  def query(keys: Seq[Key]) = lookupWithEventualConsistency(keys)

}
