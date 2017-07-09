package com.xebia.nimbus

import com.xebia.nimbus.datastore.model.{PathElement, PathElementId, PathElementName, Value}

case class Path(elements: Seq[PathElement])

object Path {

  implicit class ExtendablePath(s: Path) {
    def /(kind: String, name: String) = Path(s.elements :+ PathElement(kind, Some(PathElementName(name))))

    def /(kind: String, id: Long) = Path(s.elements :+ PathElement(kind, Some(PathElementId(id))))
  }

  implicit class ExtendableNameTuple(s: (String, String)) {
    def /(kind: String, name: String) = Path(Seq(PathElement(s._1, Some(PathElementName(s._2))), PathElement(kind, Some(PathElementName(name)))))

    def /(kind: String, id: Long) = Path(Seq(PathElement(s._1, Some(PathElementName(s._2))), PathElement(kind, Some(PathElementId(id)))))
  }

  implicit class ExtendableIdTuple(s: (String, Long)) {
    def /(kind: String, name: String) = Path(Seq(PathElement(s._1, Some(PathElementId(s._2))), PathElement(kind, Some(PathElementName(name)))))

    def /(kind: String, id: Long) = Path(Seq(PathElement(s._1, Some(PathElementId(s._2))), PathElement(kind, Some(PathElementId(id)))))
  }
}

final case class Entity(path: Path, properties: Map[String, Value])