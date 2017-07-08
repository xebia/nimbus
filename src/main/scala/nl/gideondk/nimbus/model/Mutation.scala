package nl.gideondk.nimbus.model

trait Mutation

trait ContentMutation extends Mutation {
  def entity: Entity

  def baseVersion: Option[String]
}

case class Insert(entity: Entity, baseVersion: Option[String]) extends ContentMutation

object Insert {
  def apply(entity: Entity): Insert = Insert(entity, None)
}

case class Update(entity: Entity, baseVersion: Option[String]) extends ContentMutation

object Update {
  def apply(entity: Entity): Update = Update(entity, None)
}

case class Upsert(entity: Entity, baseVersion: Option[String]) extends ContentMutation

object Upsert {
  def apply(entity: Entity): Upsert = Upsert(entity, None)
}

case class Delete(key: Key) extends Mutation
