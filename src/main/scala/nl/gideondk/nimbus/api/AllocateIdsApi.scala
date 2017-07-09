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

package nl.gideondk.nimbus.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, Uri}
import nl.gideondk.nimbus.Connection
import nl.gideondk.nimbus.api.LookupApi.LookupRequest
import nl.gideondk.nimbus.model.Key
import nl.gideondk.nimbus.serialization.NimbusSerialization
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

object AllocateIdsApi extends NimbusSerialization {

  implicit val allocateIdsRequestFormat = jsonFormat1(AllocateIdsRequest.apply)
  implicit val allocateIdsResponseFormat = jsonFormat1(AllocateIdsResponse.apply)

  case class AllocateIdsRequest(keys: Seq[Key])

  case class AllocateIdsResponse(keys: Seq[Key])
}
trait AllocateIdsApi extends Connection {
  import AllocateIdsApi._

  def allocateIds(keys: Seq[Key]): Future[Seq[Key]] = {
    val uri: Uri = baseUri + ":allocateIds"
    for {
      request <- Marshal(AllocateIdsRequest(keys)).to[RequestEntity].map(x => HttpRequest(HttpMethods.POST, uri, entity = x))
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      entity <- handleErrorOrUnmarshal[AllocateIdsResponse](response)
    } yield {
      entity.keys
    }
  }
}
