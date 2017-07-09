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

package com.xebia.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, Uri}
import com.xebia.nimbus.Connection
import com.xebia.nimbus.model._
import com.xebia.nimbus.serialization.NimbusSerialization
import spray.json.{RootJsonWriter, _}

import scala.concurrent.Future

object CommitApi extends NimbusSerialization {
  implicit val mutationResultFormat = jsonFormat3(MutationResult.apply)
  implicit val commitResponseFormat = jsonFormat2(CommitResponse.apply)

  implicit object CommitRequestJsonFormat extends RootJsonWriter[CommitRequest] {
    def write(c: CommitRequest) = {

      val mutationsAsJson = c.mutations.map { mutation =>
        mutation match {
          case x: Insert => JsObject("insert" -> x.entity.toJson)
          case x: Update => JsObject("update" -> x.entity.toJson)
          case x: Upsert => JsObject("upsert" -> x.entity.toJson)
          case x: Delete => JsObject("delete" -> x.key.toJson)
        }
      }

      JsObject(Map("mode" -> JsString(c.mode.toString), "mutations" -> JsArray(mutationsAsJson.toVector), "transaction" -> c.transactionId.map(x => JsString(x)).getOrElse(JsNull)).filter(x => x._2 != JsNull))
    }
  }

  object CommitMode extends Enumeration {
    val Transactional = Value("TRANSACTIONAL")
    val NonTransactional = Value("NON_TRANSACTIONAL")
  }

  case class MutationResult(key: Option[Key], version: String, conflictDetected: Option[Boolean])

  case class CommitRequest(mode: CommitMode.Value, mutations: Seq[Mutation], transactionId: Option[String])

  case class CommitResponse(mutationResults: Seq[MutationResult], indexUpdates: Option[Int])

}

trait CommitApi extends Connection {

  import CommitApi._

  def commit(transactionId: Option[String], mutations: Seq[Mutation], commitMode: CommitMode.Value): Future[CommitResponse] = {
    val uri: Uri = baseUri + ":commit"
    for {
      request <- Marshal(CommitRequest(commitMode, mutations, transactionId)).to[RequestEntity].map(x => HttpRequest(HttpMethods.POST, uri, entity = x))
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      entity <- handleErrorOrUnmarshal[CommitResponse](response)
    } yield {
      entity
    }
  }
}
