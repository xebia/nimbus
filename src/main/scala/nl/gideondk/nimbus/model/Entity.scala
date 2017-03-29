package nl.gideondk.nimbus.model

final case class Entity(key: Key, properties: Map[String, Value])

final case class EmbeddedEntity(properties: Map[String, Value])

