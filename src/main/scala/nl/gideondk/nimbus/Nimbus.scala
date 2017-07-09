package nl.gideondk.nimbus

import nl.gideondk.nimbus.api.CommitApi.{CommitMode, MutationResult}
import nl.gideondk.nimbus.model._

import scala.concurrent.Future

class Nimbus(client: Client) {
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
