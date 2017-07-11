package com.xebia.nimbus

import com.xebia.nimbus.datastore.model.{ PathElement, PathElementId, PathElementName, Value }
import scala.language.implicitConversions

case class Path(elements: Seq[PathElement])

object Path {

  implicit class ExtendablePath(s: Path) {
    def /(kind: Symbol, name: String): Path = Path(s.elements :+ PathElement(kind.name, Some(PathElementName(name))))

    def /(kind: Symbol, id: Long): Path = Path(s.elements :+ PathElement(kind.name, Some(PathElementId(id))))

    def /(tpl: Tuple2[Symbol, String]): Path = /(tpl._1, tpl._2)

    def /(kind: Symbol): Path = Path(s.elements :+ PathElement(kind.name, None))
  }

  implicit class ExtendableNameTuple(s: (Symbol, String)) {
    def /(kind: Symbol, name: String): Path = Path(Seq(PathElement(s._1.name, Some(PathElementName(s._2))), PathElement(kind.name, Some(PathElementName(name)))))

    def /(kind: Symbol, id: Long): Path = Path(Seq(PathElement(s._1.name, Some(PathElementName(s._2))), PathElement(kind.name, Some(PathElementId(id)))))

    def /(tpl: Tuple2[Symbol, String]): Path = /(tpl._1, tpl._2)

    def /(kind: Symbol): Path = Path(Seq(PathElement(s._1.name, Some(PathElementName(s._2)))) :+ PathElement(kind.name, None))

  }

  implicit class ExtendableIdTuple(s: (String, Long)) {
    def /(kind: Symbol, name: String): Path = Path(Seq(PathElement(s._1, Some(PathElementId(s._2))), PathElement(kind.name, Some(PathElementName(name)))))

    def /(kind: Symbol, id: Long): Path = Path(Seq(PathElement(s._1, Some(PathElementId(s._2))), PathElement(kind.name, Some(PathElementId(id)))))

    def /(tpl: Tuple2[Symbol, String]): Path = /(tpl._1, tpl._2)

    def /(kind: Symbol): Path = Path(Seq(PathElement(s._1, Some(PathElementId(s._2)))) :+ PathElement(kind.name, None))
  }

  implicit def SymbolToPath(kind: Symbol) = Path(Seq(PathElement(kind.name, None)))

  implicit def SymbolStringToPath(kindNameTuple: (Symbol, String)) = Path(Seq(PathElement(kindNameTuple._1.name, Some(PathElementName(kindNameTuple._2)))))
}

final case class Entity(path: Path, properties: Map[String, Value])

trait EntityWriter[A] {
  def write(value: A): Entity
}

trait EntityReader[A] {
  def read(entity: Entity): A
}

trait EntityConverter[A] extends EntityWriter[A] with EntityReader[A]

object Entity {
  implicit val passThroughEntityFormatter = new EntityConverter[Entity] {
    override def write(value: Entity): Entity = value

    override def read(entity: Entity): Entity = entity
  }

  def apply(kind: Symbol, name: String, properties: Map[String, Value]): Entity = Entity(Path.SymbolStringToPath((kind, name)), properties)
}