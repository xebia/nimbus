package com.xebia.nimbus

import com.xebia.nimbus.datastore.api.QueryApi.Filter.{CompositeFilter, PropertyFilter}
import com.xebia.nimbus.datastore.api.QueryApi.{CompositeOperator, PropertyOperator, PropertyReference, RawQuery}
import com.xebia.nimbus.datastore.model.{Key, ToValue, Value}

object Query {

  implicit class stringToFieldNameToComparisonFilter(s: String) {
    def >[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.GreaterThan, implicitly[ToValue[A]].toValue(v))

    def >=[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.GreaterThanOrEqual, implicitly[ToValue[A]].toValue(v))

    def <[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.LessThan, implicitly[ToValue[A]].toValue(v))

    def <=[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.LessThanOrEqual, implicitly[ToValue[A]].toValue(v))

    def ===[A: ToValue](v: A) = PropertyFilter(PropertyReference(s), PropertyOperator.Equal, implicitly[ToValue[A]].toValue(v))

    def \\(v: Key) = PropertyFilter(PropertyReference(s), PropertyOperator.HasAncestor, Value(v))

    def hasAncestor(v: Key) = PropertyFilter(PropertyReference(s), PropertyOperator.HasAncestor, Value(v))
  }

  implicit class propertyFilterToCombinable(filter: PropertyFilter) {
    def and(combineWith: PropertyFilter) = CompositeFilter(CompositeOperator.And, Seq(filter, combineWith))
  }

  implicit class compositeFilterToCombinable(filter: CompositeFilter) {
    def and(combineWith: PropertyFilter) = CompositeFilter(CompositeOperator.And, filter.filter :+ combineWith)
  }

  case class QueryDSL(val inner: RawQuery) {

    import com.xebia.nimbus.datastore.api.QueryApi._

    def kindOf(kind: String) = QueryDSL(inner.copy(kind = Some(Seq(kind)))) // Only one kind can be set in the current DS API

    def orderAscBy(field: String) = QueryDSL(inner.copy(order = Some((inner.order.getOrElse(Seq.empty) :+ PropertyOrder(field, OrderDirection.Ascending)))))

    def orderDescBy(field: String) = QueryDSL(inner.copy(order = Some((inner.order.getOrElse(Seq.empty) :+ PropertyOrder(field, OrderDirection.Descending)))))

    def filterBy(filter: Filter) = QueryDSL(inner.copy(filter = Some(filter)))

    def projectOn(fields: String*) = QueryDSL(inner.copy(projection = Some(fields.toSeq.map(x => Projection(PropertyReference(x))))))

    def startFrom(cursor: String) = QueryDSL(inner.copy(startCursor = Some(cursor)))

    def endAt(cursor: String) = QueryDSL(inner.copy(endCursor = Some(cursor)))

    def withOffset(offset: Int) = QueryDSL(inner.copy(offset = Some(offset)))

    def withLimit(limit: Int) = QueryDSL(inner.copy(limit = Some(limit)))
  }

  val Query = QueryDSL(RawQuery(None, None, None, None, None, None, None, None, None))
}