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

final case class Value(meaning: Option[Int], excludeFromIndexes: Option[Boolean], value: ValueType)

object Value {
  def apply(value: ValueType): Value = Value(None, None, value)

  def apply(value: Boolean): Value = Value(None, None, BooleanValue(value))

  def apply(value: Boolean, excludeFromIndexes: Boolean): Value = Value(None, Some(excludeFromIndexes), BooleanValue(value))

  def apply(value: Long): Value = Value(None, None, IntegerValue(value))

  def apply(value: Long, excludeFromIndexes: Boolean): Value = Value(None, Some(excludeFromIndexes), IntegerValue(value))

  def apply(value: Double): Value = Value(None, None, DoubleValue(value))

  def apply(value: Double, excludeFromIndexes: Boolean): Value = Value(None, Some(excludeFromIndexes), DoubleValue(value))

  def apply(value: Key): Value = Value(None, None, KeyValue(value))

  def apply(value: Key, excludeFromIndexes: Boolean): Value = Value(None, Some(excludeFromIndexes), KeyValue(value))

  def apply(value: String): Value = Value(None, None, StringValue(value))

  def apply(value: String, excludeFromIndexes: Boolean): Value = Value(None, Some(excludeFromIndexes), StringValue(value))

  def apply(value: Array[Byte]): Value = Value(None, None, BlobValue(value))

  def apply(value: Array[Byte], excludeFromIndexes: Boolean): Value = Value(None, Some(excludeFromIndexes), BlobValue(value))

  implicit def booleanToValue(v: Boolean) = apply(v)
  implicit def longToValue(v: Long) = apply(v)
  implicit def intToValue(v: Int) = apply(v.toLong)
  implicit def doubleToValue(v: Double) = apply(v)
  implicit def keyToValue(v: Key) = apply(v)
  implicit def stringToValue(v: String) = apply(v)
  implicit def byteArrayToValue(v: Array[Byte]) = apply(v)
}

final case class EmbeddedValue(value: ValueType)

