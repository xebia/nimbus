package com.xebia.nimbus

import com.xebia.nimbus.datastore.api.QueryApi.Filter.{ CompositeFilter, PropertyFilter }
import com.xebia.nimbus.datastore.api.QueryApi.{ CompositeOperator, PropertyOperator, PropertyReference, RawQuery }
import com.xebia.nimbus.datastore.model.{ Key, ValueWriter, Value }

object Query {

  implicit class symbolToFieldNameToComparisonFilter(s: Symbol) {
    def >[A: ValueWriter](v: A) = PropertyFilter(PropertyReference(s.name), PropertyOperator.GreaterThan, implicitly[ValueWriter[A]].write(v))

    def >=[A: ValueWriter](v: A) = PropertyFilter(PropertyReference(s.name), PropertyOperator.GreaterThanOrEqual, implicitly[ValueWriter[A]].write(v))

    def <[A: ValueWriter](v: A) = PropertyFilter(PropertyReference(s.name), PropertyOperator.LessThan, implicitly[ValueWriter[A]].write(v))

    def <=[A: ValueWriter](v: A) = PropertyFilter(PropertyReference(s.name), PropertyOperator.LessThanOrEqual, implicitly[ValueWriter[A]].write(v))

    def ===[A: ValueWriter](v: A) = PropertyFilter(PropertyReference(s.name), PropertyOperator.Equal, implicitly[ValueWriter[A]].write(v))

    def \\(v: Key) = PropertyFilter(PropertyReference(s.name), PropertyOperator.HasAncestor, Value(v))

    def hasAncestor(v: Key) = PropertyFilter(PropertyReference(s.name), PropertyOperator.HasAncestor, Value(v))
  }

  implicit class propertyFilterToCombinable(filter: PropertyFilter) {
    def and(combineWith: PropertyFilter) = CompositeFilter(CompositeOperator.And, Seq(filter, combineWith))
  }

  implicit class compositeFilterToCombinable(filter: CompositeFilter) {
    def and(combineWith: PropertyFilter) = CompositeFilter(CompositeOperator.And, filter.filters :+ combineWith)
  }

  case class QueryDSL(val inner: RawQuery) {

    import com.xebia.nimbus.datastore.api.QueryApi._

    def kindOf(kind: Symbol) = QueryDSL(inner.copy(kind = Some(Seq(kind.name)))) // Only one kind can be set in the current DS API

    def orderAscBy(field: Symbol) = QueryDSL(inner.copy(order = Some((inner.order.getOrElse(Seq.empty) :+ PropertyOrder(field.name, OrderDirection.Ascending)))))

    def orderDescBy(field: Symbol) = QueryDSL(inner.copy(order = Some((inner.order.getOrElse(Seq.empty) :+ PropertyOrder(field.name, OrderDirection.Descending)))))

    def filterBy(filter: Filter) = QueryDSL(inner.copy(filter = Some(filter)))

    def projectOn(fields: Symbol*) = QueryDSL(inner.copy(projection = Some(fields.toSeq.map(x => Projection(PropertyReference(x.name))))))

    def startFrom(cursor: String) = QueryDSL(inner.copy(startCursor = Some(cursor)))

    def startFrom(cursorOpt: Option[String]) = QueryDSL(inner.copy(startCursor = cursorOpt))

    def endAt(cursor: String) = QueryDSL(inner.copy(endCursor = Some(cursor)))

    def withOffset(offset: Int) = QueryDSL(inner.copy(offset = Some(offset)))

    def withLimit(limit: Int) = QueryDSL(inner.copy(limit = Some(limit)))
  }

  val Q = QueryDSL(RawQuery(None, None, None, None, None, None, None, None, None))
}