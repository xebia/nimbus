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

package com.xebia.nimbus.datastore.model

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

  implicit val booleanToValue = new ValueFormatter[Boolean] {
    def write(v: Boolean) = apply(v)

    def read(v: Value) = v.value match {
      case BooleanValue(v) => v
      case x               => throw new Exception("Expected Boolean value, but got " + x)
    }
  }

  implicit val longToValue = new ValueFormatter[Long] {
    def write(v: Long) = apply(v)

    def read(v: Value) = v.value match {
      case IntegerValue(v) => v
      case x               => throw new Exception("Expected Integer value, but got " + x)
    }
  }

  implicit val intToValue = new ValueFormatter[Int] {
    def write(v: Int) = apply(v.toLong)

    def read(v: Value) = v.value match {
      case IntegerValue(v) => v.toInt
      case x               => throw new Exception("Expected Integer value, but got " + x)
    }
  }

  implicit val doubleToValue = new ValueFormatter[Double] {
    def write(v: Double) = apply(v)

    def read(v: Value) = v.value match {
      case DoubleValue(v) => v
      case x              => throw new Exception("Expected Double value, but got " + x)
    }
  }

  implicit val keyToValue = new ValueFormatter[Key] {
    def write(v: Key) = apply(v)

    def read(v: Value) = v.value match {
      case KeyValue(v) => v
      case x           => throw new Exception("Expected Key value, but got " + x)
    }
  }

  implicit val stringToValue = new ValueFormatter[String] {
    def write(v: String) = apply(v)

    def read(v: Value) = v.value match {
      case StringValue(v) => v
      case x              => throw new Exception("Expected String value, but got " + x)
    }
  }

  implicit val baToValue = new ValueFormatter[Array[Byte]] {
    def write(v: Array[Byte]) = apply(v)

    def read(v: Value) = v.value match {
      case BlobValue(v) => v
      case x            => throw new Exception("Expected Blob value, but got " + x)
    }
  }

  implicit val passthrough = new ValueFormatter[Value] {
    def write(v: Value) = v

    def read(v: Value) = v
  }

  implicit def toValue[A: ValueWriter](v: A) = implicitly[ValueWriter[A]].write(v)

  implicit class ReadableValue(v: Value) {
    def as[A: ValueReader] = implicitly[ValueReader[A]].read(v)
  }
}

trait ValueWriter[A] {
  def write(v: A): Value
}

trait ValueReader[A] {
  def read(v: Value): A
}

trait ValueFormatter[A] extends ValueWriter[A] with ValueReader[A]

final case class EmbeddedValue(value: ValueType)

