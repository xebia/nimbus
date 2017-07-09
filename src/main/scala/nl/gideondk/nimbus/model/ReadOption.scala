package nl.gideondk.nimbus.model

import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}

trait ReadOption

object ReadConsistency extends Enumeration {
  val Strong = Value("STRONG")
  val Eventual = Value("EVENTUAL")
}

case class TransactionConsistency(transaction: String) extends ReadOption

case class ExplicitConsistency(readConsistency: ReadConsistency.Value) extends ReadOption