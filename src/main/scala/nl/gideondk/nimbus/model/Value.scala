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

package nl.gideondk.nimbus.model

import scala.language.implicitConversions

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

