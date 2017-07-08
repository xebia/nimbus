package nl.gideondk.nimbus.model

trait PathElementIdType

final case class PathElementId(value: Long) extends PathElementIdType

final case class PathElementName(value: String) extends PathElementIdType

final case class PathElement(kind: String, id: Option[PathElementIdType])