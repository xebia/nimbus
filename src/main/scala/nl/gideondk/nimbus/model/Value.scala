package nl.gideondk.nimbus.model

sealed trait ValueType

final case object NullValue extends ValueType

final case class BooleanValue(value: Boolean) extends ValueType

final case class IntegerValue(value: Long) extends ValueType

final case class DoubleValue(value: Double) extends ValueType

final case class TimestampValue(value: String) extends ValueType

final case class KeyValue(value: Key) extends ValueType

final case class StringValue(value: String) extends ValueType

final case class BlobValue(value: Array[Byte]) extends ValueType

final case class GeoPointValue(value: (Double, Double)) extends ValueType

final case class EntityValue(value: EmbeddedEntity) extends ValueType

final case class ArrayValue(value: Seq[EmbeddedValue]) extends ValueType

final case class Value(meaning: Int, excludeFromIndexes: Boolean, value: ValueType)

final case class EmbeddedValue(value: ValueType)

