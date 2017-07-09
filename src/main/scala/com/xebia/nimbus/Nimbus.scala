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

import akka.stream.OverflowStrategy
import com.xebia.nimbus.Connection.AccessToken
import com.xebia.nimbus.Query.QueryDSL
import com.xebia.nimbus.datastore.api.CommitApi.{CommitMode, MutationResult}
import com.xebia.nimbus.datastore.api.QueryApi.QueryResponse
import com.xebia.nimbus.datastore.model._

import scala.concurrent.Future

trait NimbusDSL {


}

object Nimbus extends NimbusDSL {
  def apply(accessToken: AccessToken,
            projectId: String,
            namespace: String,
            overflowStrategy: OverflowStrategy,
            maximumRequestsInFlight: Int) = {
    val client = new RawClient(accessToken, projectId, overflowStrategy, maximumRequestsInFlight)
    new Nimbus(namespace, client)
  }
}

class Nimbus(namespace: String, client: RawClient) {

  implicit val dispatcher = client.system.dispatcher

  val partitionId = PartitionId(client.projectId, Some(namespace))

  private implicit def pathToKey(p: Path): Key = Key(partitionId, p.elements)

  private implicit def keyToPath(k: Key): Path = Path(k.path)

  private implicit def nimbusEntityToDSEntity(e: Entity): RawEntity = RawEntity(e.path, Some(e.properties))

  private implicit def dsEntityToNimbusEntity(e: RawEntity): Entity = Entity(e.key, e.properties.getOrElse(throw new Exception("Expected the properties of the entity to be set")))

  def beginTransaction() =
    client.beginTransaction()

  def commitTransactional(transactionId: String, mutations: Seq[Mutation]): Future[Seq[MutationResult]] =
    client.commit(Some(transactionId), mutations, CommitMode.Transactional).map(_.mutationResults)

  def commitNonTransactional(mutations: Seq[Mutation]): Future[Seq[MutationResult]] =
    client.commit(None, mutations, CommitMode.NonTransactional).map(_.mutationResults)

  /** CRUD **/

  def insertTransactional(transactionId: String, entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(x => Insert(x)))

  def insert(entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(x => Insert(x)))

  def updateTransactional(transactionId: String, entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(x => Update(x)))

  def update(entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(x => Update(x)))

  def upsertTransactional(transactionId: String, entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(x => Upsert(x)))

  def upsert(entities: Seq[Entity]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(x => Upsert(x)))

  def deleteTransactional(transactionId: String, keys: Seq[Key]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, keys.map(x => Delete(x)))

  def delete(keys: Seq[Key]): Future[Seq[MutationResult]] =
    commitNonTransactional(keys.map(x => Delete(x)))

  /** Lookup **/

  case class LookupResult(found: Seq[Entity], missing: Seq[Path])

  def lookupWithinTransaction(transactionId: String, paths: Seq[Path]): Future[LookupResult] =
    client.lookup(TransactionConsistency(transactionId), paths.map(pathToKey)).map(x => LookupResult(x.found.toSeq.flatten.map(x => dsEntityToNimbusEntity(x.entity)), x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))))

  def lookupWithinTransaction(transactionId: String, path: Path): Future[Option[Entity]] =
    client.lookup(TransactionConsistency(transactionId), Seq(path)).map(response => response.found.flatMap(_.headOption.map(x => dsEntityToNimbusEntity(x.entity))))

  def lookupWithEventualConsistency(paths: Seq[Path]): Future[LookupResult] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), paths.map(pathToKey)).map(x => LookupResult(x.found.toSeq.flatten.map(x => dsEntityToNimbusEntity(x.entity)), x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))))

  def lookupWithEventualConsistency(path: Path): Future[Option[Entity]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), Seq(path)).map(response => response.found.flatMap(_.headOption.map(x => dsEntityToNimbusEntity(x.entity))))

  def lookupWithStrongConsistency(paths: Seq[Path]): Future[LookupResult] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), paths.map(pathToKey)).map(x => LookupResult(x.found.toSeq.flatten.map(x => dsEntityToNimbusEntity(x.entity)), x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))))

  def lookupWithStrongConsistency(path: Path): Future[Option[RawEntity]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), Seq(pathToKey(path))).map(response => response.found.flatMap(_.headOption.map(_.entity)))

  def lookup(paths: Seq[Path]) = lookupWithEventualConsistency(paths)

  def lookup(path: Path) = lookupWithEventualConsistency(path)

  /** Query **/

  def queryWithinTransaction(transactionId: String, q: QueryDSL): Future[QueryResponse] =
    client.query(partitionId, TransactionConsistency(transactionId), q.inner)

  def queryWithEventualConsistency(q: QueryDSL): Future[QueryResponse] =
    client.query(partitionId, ExplicitConsistency(ReadConsistency.Eventual), q.inner)

  def queryWitStrongConsistency(q: QueryDSL): Future[QueryResponse] =
    client.query(partitionId, ExplicitConsistency(ReadConsistency.Strong), q.inner)

  def query(q: QueryDSL) = queryWithEventualConsistency(q)
}
