package nl.gideondk.nimbus.serialization

import nl.gideondk.nimbus.model._
import spray.json.{DefaultJsonProtocol, _}

trait KeySerialization extends DefaultJsonProtocol {
  implicit val keyFormatter = jsonFormat2(Key.apply)
}

trait PartitionIdSerialization extends DefaultJsonProtocol {
  implicit val partitionIdFormatter = jsonFormat2(PartitionId.apply)
}

trait PathElementSerialization {

  implicit object PathElementJsonFormat extends RootJsonFormat[PathElement] {
    def write(c: PathElement) = {
      val idType = c.id match {
        case PathElementId(value) => "id" -> JsString(value.toString)
        case PathElementName(value) => "name" -> JsString(value)
      }
      JsObject("kind" -> JsString(c.kind), idType)
    }

    def read(value: JsValue) = value match {
      case JsObject(fields) if (fields.isDefinedAt("id")) =>
        PathElement(fields("kind").convertTo[String], PathElementId(fields("id").convertTo[String].toLong))
      case JsObject(fields) if (fields.isDefinedAt("name")) =>
        PathElement(fields("kind").convertTo[String], PathElementName(fields("name").convertTo[String]))
      case _ => deserializationError("Expected PathElement")
    }
  }

}


trait ValueSerialization extends DefaultJsonProtocol with KeySerialization with EntitySerialization {
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

  def valueTypeToJsTuple(t: ValueType) = t match {
    case NullValue => (NullValueKey -> JsNull)
    case BooleanValue(v) => (BooleanValueKey -> JsBoolean(v))
    case IntegerValue(v) => (IntegerValueKey -> JsString(v.toString)) // TODO: check if this flat conversion works
    case TimestampValue(v) => (TimestampValueKey -> JsString(v))
    case KeyValue(v) => (KeyValueKey -> v.toJson)
    case StringValue(v) => (StringValueKey -> JsString(v))
    case BlobValue(v) => (BlobValueKey -> JsString(new String(v)))
    case GeoPointValue(v) => ???
    case EntityValue(v) => (EntityValueKey -> v.toJson)
    case ArrayValue(v) => (ArrayValueKey -> v.toJson)
  }

  def valueTypeFromJsFields(fields: Map[String, JsValue]) = {
    fields.keySet.filter(x => x.contains("Value")).headOption match { // TODO: fix this monstrosity
      case Some(NullValueKey) => NullValue
      case Some(BooleanValueKey) => BooleanValue(fields(BooleanValueKey).convertTo[Boolean])
      case Some(IntegerValueKey) => IntegerValue(fields(BooleanValueKey).convertTo[Long])
      case Some(TimestampValueKey) => TimestampValue(fields(TimestampValueKey).convertTo[String])
      case Some(KeyValueKey) => KeyValue(fields(KeyValueKey).convertTo[Key])
      case Some(StringValueKey) => StringValue(fields(StringValueKey).convertTo[String])
      case Some(BlobValueKey) => BlobValue(fields(BlobValueKey).convertTo[String].toArray)
      case Some(GeoPointValueKey) => ???
      case Some(EntityValueKey) => EntityValue(fields(EntityValueKey).convertTo[EmbeddedEntity])
      case Some(ArrayValueKey) => ArrayValue(fields(ArrayValueKey).convertTo[JsArray].elements.map(x => x.convertTo[EmbeddedValue]))
    }
  }

  implicit object ValueJsonFormat extends RootJsonFormat[Value] {
    def write(c: Value) = {
      JsObject("meaning" -> JsNumber(c.meaning), "excludeFromIndexes" -> JsBoolean(c.excludeFromIndexes), valueTypeToJsTuple(c.value))
    }

    def read(value: JsValue) = value match {
      case JsObject(fields) =>
        Value(fields("meaning").convertTo[Int], fields("excludeFromIndexes").convertTo[Boolean], valueTypeFromJsFields(fields))
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
}

trait EntitySerialization extends DefaultJsonProtocol {
  implicit val entityFormatter = jsonFormat2(Entity.apply)

  implicit val embeddedEntityFormatter = jsonFormat1(EmbeddedEntity.apply)
}

trait EntityResultSerialization extends EntitySerialization {
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
    with PartitionIdSerialization
    with PathElementSerialization
    with ValueSerialization
    with EntitySerialization
    with EntityResultSerialization {}
