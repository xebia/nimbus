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
import com.xebia.nimbus.Nimbus.{NimbusQuery, Path}
import com.xebia.nimbus.api.CommitApi.{CommitMode, MutationResult}
import com.xebia.nimbus.api.QueryApi.Filter.{CompositeFilter, PropertyFilter}
import com.xebia.nimbus.api.QueryApi.{CompositeOperator, PropertyOperator, PropertyReference, Query, QueryResponse}
import com.xebia.nimbus.model._

import scala.concurrent.Future

trait NimbusDSL {
  case class Path(elements: Seq[PathElement])

  implicit class ExtendablePath(s: Path) {
    def /(kind: String, name: String) = Path(s.elements :+ PathElement(kind, Some(PathElementName(name))))

    def /(kind: String, id: Long) = Path(s.elements :+ PathElement(kind, Some(PathElementId(id))))
  }

  implicit class ExtendableNameTuple(s: (String, String)) {
    def /(kind: String, name: String) = Path(Seq(PathElement(s._1, Some(PathElementName(s._2))), PathElement(kind, Some(PathElementName(name)))))

    def /(kind: String, id: Long) = Path(Seq(PathElement(s._1, Some(PathElementName(s._2))), PathElement(kind, Some(PathElementId(id)))))
  }

  implicit class ExtendableIdTuple(s: (String, Long)) {
    def /(kind: String, name: String) = Path(Seq(PathElement(s._1, Some(PathElementId(s._2))), PathElement(kind, Some(PathElementName(name)))))

    def /(kind: String, id: Long) = Path(Seq(PathElement(s._1, Some(PathElementId(s._2))), PathElement(kind, Some(PathElementId(id)))))
  }

  implicit class stringToFieldNameToComparisonFilter(s: String) {
    def >[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.GreaterThan, implicitly[ToValue[A]].toValue(v))

    def >=[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.GreaterThanOrEqual, implicitly[ToValue[A]].toValue(v))

    def <[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.LessThan, implicitly[ToValue[A]].toValue(v))

    def <=[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.LessThanOrEqual, implicitly[ToValue[A]].toValue(v))

    def ===[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.Equal, implicitly[ToValue[A]].toValue(v))

    def \\(v: Key) = PropertyFilter(PropertyReference(s), PropertyOperator.HasAncestor, Value(v))

    def hasAncestor(v: Key) = PropertyFilter(PropertyReference(s), PropertyOperator.HasAncestor, Value(v))
  }

  implicit class propertyFilterToCombinable(filter: PropertyFilter) {
    def and(combineWith: PropertyFilter) = CompositeFilter(CompositeOperator.And, Seq(filter, combineWith))
  }

  implicit class compositeFilterToCombinable(filter: CompositeFilter) {
    def and(combineWith: PropertyFilter) = CompositeFilter(CompositeOperator.And, filter.filter :+ combineWith)
  }

  case class NimbusQuery(val inner: Query) {

    import com.xebia.nimbus.api.QueryApi._

    def kindOf(kind: String): NimbusQuery = NimbusQuery(inner.copy(kind = Some(Seq(kind)))) // Only one kind can be set in the current DS API

    def orderAscBy(field: String) = NimbusQuery(inner.copy(order = Some((inner.order.getOrElse(Seq.empty) :+ PropertyOrder(field, OrderDirection.Ascending)))))

    def orderDescBy(field: String) = NimbusQuery(inner.copy(order = Some((inner.order.getOrElse(Seq.empty) :+ PropertyOrder(field, OrderDirection.Descending)))))

    def filterBy(filter: Filter) = NimbusQuery(inner.copy(filter = Some(filter)))

    def projectOn(fields: String*) = NimbusQuery(inner.copy(projection = Some(fields.toSeq.map(x => Projection(PropertyReference(x))))))

    def startFrom(cursor: String) = NimbusQuery(inner.copy(startCursor = Some(cursor)))

    def endAt(cursor: String) = NimbusQuery(inner.copy(endCursor = Some(cursor)))

    def withOffset(offset: Int) = NimbusQuery(inner.copy(offset = Some(offset)))

    def withLimit(limit: Int) = NimbusQuery(inner.copy(limit = Some(limit)))
  }

  private case class QueryStart()

  private implicit def queryStartToNimbusQuery(q: QueryStart) = NimbusQuery(Query(None, None, None, None, None, None, None, None, None))

  final val Q = QueryStart()
}

object Nimbus extends NimbusDSL {
  def apply(accessToken: AccessToken,
            projectId: String,
            namespace: String,
            overflowStrategy: OverflowStrategy,
            maximumRequestsInFlight: Int) = {
    val client = new Client(accessToken, projectId, overflowStrategy, maximumRequestsInFlight)
    new Nimbus(namespace, client)
  }
}

class Nimbus(namespace: String, client: Client) {
  implicit val dispatcher = client.system.dispatcher

  val partitionId = PartitionId(client.projectId, Some(namespace))

  private implicit def pathToKey(p: Path): Key = Key(partitionId, p.elements)

  private implicit def keyToPath(k: Key): Path = Path(k.path)

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

  case class LookupResult(found: Seq[Entity], missing: Seq[Path])

  def lookupWithinTransaction(transactionId: String, paths: Seq[Path]): Future[LookupResult] =
    client.lookup(TransactionConsistency(transactionId), paths.map(pathToKey)).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))))

  def lookupWithinTransaction(transactionId: String, path: Path): Future[Option[Entity]] =
    client.lookup(TransactionConsistency(transactionId), Seq(path)).map(response => response.found.flatMap(_.headOption.map(_.entity)))

  def lookupWithEventualConsistency(paths: Seq[Path]): Future[LookupResult] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), paths.map(pathToKey)).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))))

  def lookupWithEventualConsistency(path: Path): Future[Option[Entity]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Eventual), Seq(path)).map(response => response.found.flatMap(_.headOption.map(_.entity)))

  def lookupWithStrongConsistency(paths: Seq[Path]): Future[LookupResult] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), paths.map(pathToKey)).map(x => LookupResult(x.found.toSeq.flatten.map(_.entity), x.missing.toSeq.flatten.map(x => keyToPath(x.entity.key))))

  def lookupWithStrongConsistency(path: Path): Future[Option[Entity]] =
    client.lookup(ExplicitConsistency(ReadConsistency.Strong), Seq(pathToKey(path))).map(response => response.found.flatMap(_.headOption.map(_.entity)))

  def lookup(paths: Seq[Path]) = lookupWithEventualConsistency(paths)

  def lookup(path: Path) = lookupWithEventualConsistency(path)

  /** Query **/

  def queryWithinTransaction(transactionId: String, q: NimbusQuery): Future[QueryResponse] =
    client.query(partitionId, TransactionConsistency(transactionId), q.inner)

  def queryWithEventualConsistency(q: NimbusQuery): Future[QueryResponse] =
    client.query(partitionId, ExplicitConsistency(ReadConsistency.Eventual), q.inner)

  def queryWitStrongConsistency(q: NimbusQuery): Future[QueryResponse] =
    client.query(partitionId, ExplicitConsistency(ReadConsistency.Strong), q.inner)

  def query(q: NimbusQuery) = queryWithEventualConsistency(q)
}
