package nl.gideondk.nimbus.model

final case class Entity(key: Key, properties: Option[Map[String, Value]])

object Entity {
  def apply(key: Key, properties: Map[String, Value]): Entity = Entity(key, Some(properties))
}

final case class EmbeddedEntity(properties: Map[String, Value])

