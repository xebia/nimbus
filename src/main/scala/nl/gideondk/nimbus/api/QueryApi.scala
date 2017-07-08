package nl.gideondk.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import nl.gideondk.nimbus.Connection
import nl.gideondk.nimbus.model.Key
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

trait QueryApi extends Connection with DefaultJsonProtocol {


}
