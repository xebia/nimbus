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

package com.xebia.nimbus.datastore.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, RequestEntity, Uri }
import com.xebia.nimbus.Connection
import com.xebia.nimbus.datastore.api.QueryApi.Filter.{ CompositeFilter, PropertyFilter }
import com.xebia.nimbus.datastore.api.QueryApi.GlqQueryParameter.{ GlqCursorQueryParameter, GlqValueQueryParameter }
import com.xebia.nimbus.datastore.api.QueryApi.{ CompositeOperator, EntityResultType, Filter, GlqQueryParameter, KindExpression, MoreResultsType, OrderDirection, Projection, PropertyOperator, PropertyOrder, PropertyReference, QueryRequest, QueryResponse, QueryResultBatch, RawGlqQuery, RawQuery }
import com.xebia.nimbus.datastore.model.{ EntityResult, PartitionId, ReadOption, Value }
import com.xebia.nimbus.datastore.serialization.Serializers
import spray.json.{ JsArray, JsObject, JsString, RootJsonFormat, _ }

import scala.concurrent.Future
import scala.language.implicitConversions

trait QueryApiConversions {
  implicit def stringToPropertyReference(name: String) = PropertyReference(name)

  implicit def stringToKindExpression(name: String) = KindExpression(name)
}

trait QueryApiFormatters extends Serializers {
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
        case GlqValueQueryParameter(value)   => JsObject("value" -> value.toJson)
        case GlqCursorQueryParameter(cursor) => JsObject("cursor" -> JsString(cursor))
      }
    }

    def read(js: JsValue) = ???
  }

  implicit object FilterJsonFormat extends RootJsonFormat[Filter] {
    def write(c: Filter) = {
      c match {
        case CompositeFilter(op, filter) =>
          JsObject("compositeFilter" -> JsObject("op" -> JsString(op.toString), "filters" -> JsArray(filter.map(_.toJson).toVector)))
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

  implicit val queryFormat = jsonFormat9(RawQuery.apply)
  implicit val glqQueryFormat = jsonFormat4(RawGlqQuery.apply)
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

    case class CompositeFilter(op: CompositeOperator.Value, filters: Seq[Filter]) extends Filter

    case class PropertyFilter(property: PropertyReference, op: PropertyOperator.Value, value: Value) extends Filter

  }

  case class PropertyReference(name: String)

  case class KindExpression(name: String)

  case class Projection(property: PropertyReference)

  case class PropertyOrder(property: PropertyReference, direction: OrderDirection.Value)

  case class RawQuery(projection: Option[Seq[Projection]], kind: Option[Seq[KindExpression]], filter: Option[Filter], order: Option[Seq[PropertyOrder]], distinctOn: Option[Seq[PropertyReference]], startCursor: Option[String], endCursor: Option[String], offset: Option[Int], limit: Option[Int])

  trait GlqQueryParameter

  object GlqQueryParameter {

    case class GlqValueQueryParameter(value: Value) extends GlqQueryParameter

    case class GlqCursorQueryParameter(cursor: String) extends GlqQueryParameter

  }

  case class RawGlqQuery(queryString: String, allowLiterals: Boolean, nameBindings: Map[String, GlqQueryParameter], positionalBindings: Seq[GlqQueryParameter])

  case class QueryRequest(partitionId: PartitionId, readOptions: ReadOption, query: Option[RawQuery], gqlQuery: Option[RawGlqQuery])

  case class QueryResultBatch(skippedResults: Option[Int], skippedCursor: Option[String], entityResultType: EntityResultType.Value, entityResults: Option[Seq[EntityResult]], endCursor: Option[String], moreResults: MoreResultsType.Value, snapshotVersion: Option[String])

  case class QueryResponse(batch: QueryResultBatch, query: Option[RawQuery])

}

trait QueryApi extends Connection {

  import QueryApi._

  private def query(partitionId: PartitionId, readOption: ReadOption, query: Option[RawQuery], glqQuery: Option[RawGlqQuery]): Future[QueryResponse] = {
    val uri: Uri = baseUri + ":runQuery"
    for {
      request <- Marshal(QueryRequest(partitionId, readOption, query, glqQuery)).to[RequestEntity].map(x => HttpRequest(HttpMethods.POST, uri, entity = x))
      response <- singleRequest(request)
      entity <- handleErrorOrUnmarshal[QueryResponse](response)
    } yield {
      entity
    }
  }

  def query(partitionId: PartitionId, readOption: ReadOption, query: RawQuery): Future[QueryResponse] = {
    this.query(partitionId, readOption, Some(query), None)
  }

  def query(partitionId: PartitionId, readOption: ReadOption, glqQuery: RawGlqQuery): Future[QueryResponse] = {
    this.query(partitionId, readOption, None, Some(glqQuery))
  }
}
