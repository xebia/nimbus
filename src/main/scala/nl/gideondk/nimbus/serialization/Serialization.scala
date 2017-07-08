package nl.gideondk.nimbus.serialization

import nl.gideondk.nimbus.model._
import spray.json.{DefaultJsonProtocol, _}

import scala.language.implicitConversions

trait KeySerialization extends DefaultJsonProtocol {

  implicit object PathElementJsonFormat extends RootJsonFormat[PathElement] {
    def write(c: PathElement) = {
      c.id match {
        case Some(PathElementId(value)) => JsObject("kind" -> JsString(c.kind), "id" -> JsString(value.toString))
        case Some(PathElementName(value)) => JsObject("kind" -> JsString(c.kind), "name" -> JsString(value))
        case None => JsObject("kind" -> JsString(c.kind))
      }
    }

    def read(value: JsValue) = value match {
      case JsObject(fields) if fields.isDefinedAt("id") =>
        PathElement(fields("kind").convertTo[String], Some(PathElementId(fields("id").convertTo[String].toLong)))
      case JsObject(fields) if fields.isDefinedAt("name") =>
        PathElement(fields("kind").convertTo[String], Some(PathElementName(fields("name").convertTo[String])))
      case JsObject(fields) => PathElement(fields("kind").convertTo[String], None)
      case _ => deserializationError("Expected PathElement")
    }
  }

  implicit val partitionIdFormatter = jsonFormat2(PartitionId.apply)
  implicit val keyFormatter = jsonFormat2(Key.apply)
}


trait ValueSerialization extends DefaultJsonProtocol with KeySerialization  {
  val NullValueKey = "nullValue"
  val BooleanValueKey = "booleanValue"
  val IntegerValueKey = "integerValue"
  val TimestampValueKey = "timestampValue"
  val KeyValueKey = "keyValue"
  val StringValueKey = "stringValue"
  val BlobValueKey = "blobValue"
  val GeoPointValueKey = "geoPointValue"
  val EntityValueKey = "entityValue"
  val ArrayValueKey = "arrayValue"

  def valueTypeToJsTuple(t: ValueType): (String, JsValue) = t match {
    case NullValue => NullValueKey -> JsNull
    case BooleanValue(v) => BooleanValueKey -> JsBoolean(v)
    case IntegerValue(v) => IntegerValueKey -> JsString(v.toString) // TODO: check if this flat conversion works
    case TimestampValue(v) => TimestampValueKey -> JsString(v)
    case KeyValue(v) => KeyValueKey -> v.toJson
    case StringValue(v) => StringValueKey -> JsString(v)
    case BlobValue(v) => BlobValueKey -> JsString(new String(v))
    case GeoPointValue(v) => ???
    case EntityValue(v) => EntityValueKey -> v.toJson
    case ArrayValue(v) => ArrayValueKey -> v.toJson
  }

  def valueTypeFromJsFields(fields: Map[String, JsValue]): ValueType = {
    fields.keySet.find(x => x.contains("Value")) match { // TODO: fix this monstrosity
      case Some(NullValueKey) => NullValue
      case Some(BooleanValueKey) => BooleanValue(fields(BooleanValueKey).convertTo[Boolean])
      case Some(IntegerValueKey) => IntegerValue(fields(IntegerValueKey).convertTo[String].toLong)
      case Some(TimestampValueKey) => TimestampValue(fields(TimestampValueKey).convertTo[String])
      case Some(KeyValueKey) => KeyValue(fields(KeyValueKey).convertTo[Key])
      case Some(StringValueKey) => StringValue(fields(StringValueKey).convertTo[String])
      case Some(BlobValueKey) => BlobValue(fields(BlobValueKey).convertTo[String].getBytes)
      case Some(GeoPointValueKey) => ???
      case Some(EntityValueKey) => EntityValue(fields(EntityValueKey).convertTo[EmbeddedEntity])
      case Some(ArrayValueKey) => ArrayValue(fields(ArrayValueKey).convertTo[JsArray].elements.map(x => x.convertTo[EmbeddedValue]))
    }
  }

  implicit object ValueJsonFormat extends RootJsonFormat[Value] {
    def write(c: Value) = {
      val m =
        c.meaning.map(x => Map("meaning" -> JsNumber(x))).getOrElse(Map.empty) ++
          c.excludeFromIndexes.map(x => Map("excludeFromIndexes" -> JsBoolean(x))).getOrElse(Map.empty) +
          valueTypeToJsTuple(c.value)

      JsObject(m)
    }

    def read(value: JsValue) = value match {
      case JsObject(fields) =>
        Value(fields.get("meaning").map(_.convertTo[Int]), fields.get("excludeFromIndexes").map(_.convertTo[Boolean]), valueTypeFromJsFields(fields))
      case _ => deserializationError("Expected Value")
    }
  }

  implicit object EmbeddedValueJsonFormat extends RootJsonFormat[EmbeddedValue] {
    def write(c: EmbeddedValue) = {
      JsObject(valueTypeToJsTuple(c.value))
    }

    def read(value: JsValue) = value match {
      case JsObject(fields) =>
        EmbeddedValue(valueTypeFromJsFields(fields))
      case _ => deserializationError("Expected Value")
    }
  }


  implicit val entityFormatter = jsonFormat2[Key, Option[Map[String,Value]], Entity](Entity.apply)

  implicit val embeddedEntityFormatter = jsonFormat1(EmbeddedEntity.apply)
}

trait EntityResultSerialization extends ValueSerialization {

  implicit object EntityResultFormat extends RootJsonFormat[EntityResult] {
    def write(c: EntityResult) = {
      JsObject("entity" -> c.entity.toJson, "version" -> JsString(c.version.toString), "cursor" -> JsString(new String(c.cursor)))
    }

    def read(value: JsValue) = value match {
      case JsObject(fields) =>
        EntityResult(fields("entity").convertTo[Entity], fields("version").convertTo[String].toLong, fields("cursor").convertTo[String].getBytes) // TODO: think of more performant deserialization
      case _ => deserializationError("Expected EntityResult")
    }
  }

}

object Serialization
  extends KeySerialization
    with ValueSerialization
    with EntityResultSerialization {}
