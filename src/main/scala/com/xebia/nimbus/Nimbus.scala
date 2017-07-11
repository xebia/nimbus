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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import com.xebia.nimbus.Query.QueryDSL
import com.xebia.nimbus.datastore.api.CommitApi.CommitMode
import com.xebia.nimbus.datastore.api.OAuthApi.Credentials
import com.xebia.nimbus.datastore.api.QueryApi.{EntityResultType, MoreResultsType}
import com.xebia.nimbus.datastore.model._

import scala.concurrent.Future
import scala.language.implicitConversions

case class MutationResult(path: Option[Path], version: String, conflictDetected: Option[Boolean])

object Nimbus {
  def apply(credentials: Credentials,
            projectId: String,
            namespace: String,
            overflowStrategy: OverflowStrategy,
            maximumRequestsInFlight: Int)(implicit system: ActorSystem) = {
    val client = new RawClient(credentials, projectId, overflowStrategy, maximumRequestsInFlight)
    new Nimbus(namespace, client)
  }

  def apply(credentials: Credentials,
            projectId: String,
            namespace: String)(implicit system: ActorSystem) = {
    val client = new RawClient(credentials, projectId, OverflowStrategy.backpressure, 1024)
    new Nimbus(namespace, client)
  }

  def apply(namespace: String,
            client: RawClient)(implicit system: ActorSystem) = {
    new Nimbus(namespace, client)
  }
}

class Nimbus(namespace: String, client: RawClient) {

  implicit val dispatcher = client.system.dispatcher

  val partitionId = PartitionId(client.projectId, Some(namespace))

  private implicit def pathToKey(p: Path): Key = Key(partitionId, p.elements)

  private implicit def keyToPath(k: Key): Path = Path(k.path)

  private implicit def entityToRawEntity(e: Entity): RawEntity = RawEntity(e.path, Some(e.properties))

  private implicit def rawEntityToEntity(e: RawEntity): Entity = Entity(e.key, e.properties.getOrElse(throw new Exception("Expected the properties of the entity to be set")))

  def beginTransaction() =
    client.beginTransaction()

  def commitTransactional(transactionId: String, mutations: Seq[Mutation]): Future[Seq[MutationResult]] =
    client.commit(Some(transactionId), mutations, CommitMode.Transactional).map(_.mutationResults.getOrElse(Seq.empty).map(x => MutationResult(x.key.map(x => x), x.version, x.conflictDetected)))

  def commitNonTransactional(mutations: Seq[Mutation]): Future[Seq[MutationResult]] =
    client.commit(None, mutations, CommitMode.NonTransactional).map(_.mutationResults.getOrElse(Seq.empty).map(x => MutationResult(x.key.map(x => x), x.version, x.conflictDetected)))

  /** CRUD **/
  def insertTransactional[A: EntityWriter](transactionId: String, entities: Seq[A]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(x => Insert(implicitly[EntityWriter[A]].write(x))))

  def insert[A: EntityWriter](entities: Seq[A]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(x => Insert(implicitly[EntityWriter[A]].write(x))))

  def updateTransactional[A: EntityWriter](transactionId: String, entities: Seq[A]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(x => Update(implicitly[EntityWriter[A]].write(x))))

  def update[A: EntityWriter](entities: Seq[A]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(x => Update(implicitly[EntityWriter[A]].write(x))))

  def upsertTransactional[A: EntityWriter](transactionId: String, entities: Seq[A]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, entities.map(x => Upsert(implicitly[EntityWriter[A]].write(x))))

  def upsert[A: EntityWriter](entities: Seq[A]): Future[Seq[MutationResult]] =
    commitNonTransactional(entities.map(x => Upsert(implicitly[EntityWriter[A]].write(x))))

  def deleteTransactional(transactionId: String, paths: Seq[Path]): Future[Seq[MutationResult]] =
    commitTransactional(transactionId, paths.map(x => Delete(x)))

  def deleteTransactional(transactionId: String, path: Path): Future[Seq[MutationResult]] =
    deleteTransactional(transactionId, Seq(path))

  def delete(paths: Seq[Path]): Future[Seq[MutationResult]] =
    commitNonTransactional(paths.map(x => Delete(x)))

  def delete(path: Path): Future[Seq[MutationResult]] =
    delete(Seq(path))

  /** Lookup **/

  case class LookupResult[A](found: Seq[A], missing: Seq[Path])

  def lookupWithinTransaction[A: EntityReader](transactionId: String, paths: Seq[Path]): Future[LookupResult[A]] =
    client.lookup(TransactionConsistency(transactionId), paths.map(pathToKey))
      .map(x =>
        LookupResult(
          x.found.toSeq.flatten.map(x => implicitly[EntityReader[A]].read(rawEntityToEntity(x.entity))),
          x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))
        ))

  def lookupWithinTransaction[A: EntityReader](transactionId: String, path: Path): Future[Option[A]] =
    client.lookup(TransactionConsistency(transactionId), Seq(path))
      .map(response => response.found.flatMap(_.headOption.map(x => implicitly[EntityReader[A]].read(rawEntityToEntity(x.entity)))))

  def lookupWithEventualConsistency[A: EntityReader](paths: Seq[Path]): Future[LookupResult[A]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), paths.map(pathToKey))
      .map(x => LookupResult(x.found.toSeq.flatten.map(x => implicitly[EntityReader[A]].read(rawEntityToEntity(x.entity))), x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))))

  def lookupWithEventualConsistency[A: EntityReader](path: Path): Future[Option[A]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), Seq(path)).map(response => response.found.flatMap(_.headOption.map(x => implicitly[EntityReader[A]].read(rawEntityToEntity(x.entity)))))

  def lookupWithStrongConsistency[A: EntityReader](paths: Seq[Path]): Future[LookupResult[A]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), paths.map(pathToKey))
      .map(x => LookupResult(x.found.toSeq.flatten.map(x => implicitly[EntityReader[A]].read(rawEntityToEntity(x.entity))), x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))))

  def lookupWithStrongConsistency[A: EntityReader](path: Path): Future[Option[A]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), Seq(path)).map(response => response.found.flatMap(_.headOption.map(x => implicitly[EntityReader[A]].read(rawEntityToEntity(x.entity)))))

  def lookup[A: EntityReader](paths: Seq[Path]) = lookupWithEventualConsistency[A](paths)

  def lookup[A: EntityReader](path: Path) = lookupWithEventualConsistency[A](path)

  /** Query **/

  case class QueryResult[A](resultType: EntityResultType.Value, moreResults: MoreResultsType.Value, results: Seq[A], endCursor: Option[String], snapshotVersion: Option[String])

  def queryWithinTransaction[A: EntityReader](transactionId: String, q: QueryDSL): Future[QueryResult[A]] =
    client.query(partitionId, TransactionConsistency(transactionId), q.inner)
      .map(x => QueryResult(x.batch.entityResultType, x.batch.moreResults, x.batch.entityResults.toSeq.flatten.map(x => implicitly[EntityReader[A]].read(x.entity)), x.batch.endCursor, x.batch.snapshotVersion))

  def queryWithEventualConsistency[A: EntityReader](q: QueryDSL): Future[QueryResult[A]] =
    client.query(partitionId, ExplicitConsistency(ReadConsistency.Eventual), q.inner)
      .map(x => QueryResult(x.batch.entityResultType, x.batch.moreResults, x.batch.entityResults.toSeq.flatten.map(x => implicitly[EntityReader[A]].read(x.entity)), x.batch.endCursor, x.batch.snapshotVersion))

  def queryWitStrongConsistency[A: EntityReader](q: QueryDSL): Future[QueryResult[A]] =
    client.query(partitionId, ExplicitConsistency(ReadConsistency.Strong), q.inner)
      .map(x => QueryResult(x.batch.entityResultType, x.batch.moreResults, x.batch.entityResults.toSeq.flatten.map(x => implicitly[EntityReader[A]].read(x.entity)), x.batch.endCursor, x.batch.snapshotVersion))

  def query[A: EntityReader](q: QueryDSL) = queryWithEventualConsistency(q)

  def querySource[A: EntityReader](q: QueryDSL): Source[A, NotUsed] =
    Source.unfoldAsync(q.inner.startCursor)(cursor =>
      queryWithEventualConsistency(q.startFrom(cursor))
        .map { res =>
          if (res.results.length > 0) {
            res.endCursor.map(x => Some(x) -> res.results)
          } else {
            None
          }
        }).mapConcat[A](x => x.toList)
}
