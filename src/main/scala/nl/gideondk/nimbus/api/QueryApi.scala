package nl.gideondk.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import nl.gideondk.nimbus.Connection
import nl.gideondk.nimbus.api.QueryApi.{CompositeOperator, EntityResultType, Filter, GlqCursorQueryParameter, GlqQuery, GlqQueryParameter, GlqValueQueryParameter, KindExpression, MoreResultsType, OrderDirection, Projection, PropertyOperator, PropertyOrder, PropertyReference, Query, QueryRequest, QueryResponse, QueryResultBatch}
import nl.gideondk.nimbus.model._
import nl.gideondk.nimbus.serialization.NimbusSerialization
import spray.json.{JsArray, JsObject, JsString, RootJsonFormat, _}

import scala.concurrent.Future

trait QueryApiConversions {
  implicit def stringToPropertyReference(name: String) = PropertyReference(name)

  implicit def stringToKindExpression(name: String) = KindExpression(name)
}

trait QueryApiFormatters extends NimbusSerialization {
  implicit val compositeOperatorFormat = new EnumJsonConverter(CompositeOperator)
  implicit val propertyOperatorFormat = new EnumJsonConverter(PropertyOperator)
  implicit val orderDirectionFormat = new EnumJsonConverter(OrderDirection)
  implicit val entityResultFormat = new EnumJsonConverter(EntityResultType)
  implicit val moreResultsTypeFormat = new EnumJsonConverter(MoreResultsType)

  implicit val propertyReferenceFormat = jsonFormat1(PropertyReference.apply)
  implicit val kindExpressionFormat = jsonFormat1(KindExpression.apply)
  implicit val projectionFormat = jsonFormat1(Projection.apply)
  implicit val propertyOrderFormat = jsonFormat2(PropertyOrder.apply)

  implicit object GlqQueryJsonFormat extends RootJsonFormat[GlqQueryParameter] {
    def write(c: GlqQueryParameter) = {
      c match {
        case GlqValueQueryParameter(value) => JsObject("value" -> value.toJson)
        case GlqCursorQueryParameter(cursor) => JsObject("cursor" -> JsString(cursor))
      }
    }

    def read(js: JsValue) = ???
  }

  implicit object FilterJsonFormat extends RootJsonFormat[Filter] {
    def write(c: Filter) = {
      c match {
        case CompositeFilter(op, filter) =>
          JsObject("compositeFilter" -> JsObject("op" -> JsString(op.toString), "filter" -> JsArray(filter.map(_.toJson).toVector)))
        case PropertyFilter(property, op, value) =>
          JsObject("propertyFilter" -> JsObject("property" -> property.toJson, "op" -> JsString(op.toString), "value" -> value.toJson))
      }
    }

    def read(c: JsValue): Filter = {
      val jsObjectFields = c.asJsObject.fields
      if (jsObjectFields.get("property").isDefined) {
        PropertyFilter(jsObjectFields("property").convertTo[PropertyReference], PropertyOperator.withName(jsObjectFields("op").convertTo[String]), jsObjectFields("value").convertTo[Value])
      } else {
        CompositeFilter(CompositeOperator.withName(jsObjectFields("op").convertTo[String]), (jsObjectFields("filter").convertTo[Seq[Filter]]))
      }
    }
  }

  implicit val queryFormat = jsonFormat9(Query.apply)
  implicit val glqQueryFormat = jsonFormat4(GlqQuery.apply)
  implicit val queryRequestFormat = jsonFormat4(QueryRequest.apply)
  implicit val queryResultBatchFormat = jsonFormat7(QueryResultBatch.apply)
  implicit val queryResponseFormat = jsonFormat2(QueryResponse.apply)
}

object QueryApi extends QueryApiConversions with QueryApiFormatters {

  object CompositeOperator extends Enumeration {
    val And = Value("AND")
  }

  object PropertyOperator extends Enumeration {
    val LessThan = Value("LESS_THAN")
    val LessThanOrEqual = Value("LESS_THAN_OR_EQUAL")
    val GreaterThan = Value("GREATER_THAN")
    val GreaterThanOrEqual = Value("GREATER_THAN_OR_EQUAL")
    val Equal = Value("EQUAL")
    val HasAncestor = Value("HAS_ANCESTOR")
  }

  object OrderDirection extends Enumeration {
    val Ascending = Value("ASCENDING")
    val Descending = Value("DESCENDING")
  }

  object EntityResultType extends Enumeration {
    val Full = Value("FULL")
    val Projection = Value("PROJECTION")
    val KeyOnly = Value("KEY_ONLY")
  }

  object MoreResultsType extends Enumeration {
    val NotFinished = Value("NOT_FINISHED")
    val MoreResultsAfterLimit = Value("MORE_RESULTS_AFTER_LIMIT")
    val MoreResultsAfterCursor = Value("MORE_RESULTS_AFTER_CURSOR")
    val NoMoreResults = Value("NO_MORE_RESULTS")
  }

  sealed trait Filter

  object Filter {

    case class CompositeFilter(op: CompositeOperator.Value, filter: Seq[Filter]) extends Filter

    case class PropertyFilter(property: PropertyReference, op: PropertyOperator.Value, value: Value) extends Filter

  }

  case class PropertyReference(name: String)

  case class KindExpression(name: String)

  case class Projection(property: PropertyReference)


  case class PropertyOrder(property: PropertyReference, direction: OrderDirection.Value)

  case class Query(property: Option[Seq[PropertyReference]], kind: Option[Seq[KindExpression]], filter: Option[Filter], order: Option[Seq[PropertyOrder]], distinctOn: Option[Seq[PropertyReference]], startCursor: Option[String], endCursor: Option[String], offset: Option[Int], limit: Option[Int])

  trait GlqQueryParameter

  case class GlqValueQueryParameter(value: Value) extends GlqQueryParameter

  case class GlqCursorQueryParameter(cursor: String) extends GlqQueryParameter

  case class GlqQuery(queryString: String, allowLiterals: Boolean, nameBindings: Map[String, GlqQueryParameter], positionalBindings: Seq[GlqQueryParameter])

  case class QueryRequest(partitionId: PartitionId, readOptions: ReadOption, query: Option[Query], gqlQuery: Option[GlqQuery])

  case class QueryResultBatch(skippedResults: Option[Int], skippedCursor: Option[String], entityResultType: EntityResultType.Value, entityResults: Option[Seq[EntityResult]], endCursor: Option[String], moreResults: MoreResultsType.Value, snapshotVersion: Option[String])

  case class QueryResponse(batch: QueryResultBatch, query: Option[Query])

}

trait QueryApi extends Connection {

  import QueryApi._

  private def query(partitionId: PartitionId, readOption: ReadOption, query: Option[Query], glqQuery: Option[GlqQuery]): Future[QueryResponse] = {
    val uri: Uri = baseUri + ":runQuery"
    for {
      request <- Marshal(HttpMethods.POST, uri, QueryRequest(partitionId, readOption, query, glqQuery)).to[HttpRequest]
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      entity <- handleErrorOrUnmarshal[QueryResponse](response)
    } yield {
      entity
    }
  }

  def query(partitionId: PartitionId, readOption: ReadOption, query: Query): Future[QueryResponse] = {
    this.query(partitionId, readOption, Some(query), None)
  }

  def query(partitionId: PartitionId, readOption: ReadOption, glqQuery: GlqQuery): Future[QueryResponse] = {
    this.query(partitionId, readOption, None, Some(glqQuery))
  }
}
