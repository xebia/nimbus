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
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import com.xebia.nimbus.Connection
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future

object TransactionApi extends DefaultJsonProtocol {
  case class BeginTransactionResponse(transaction: String)

  implicit val transactionResponseFormat = jsonFormat1(BeginTransactionResponse.apply)

}

trait TransactionApi extends Connection {
  import TransactionApi._

  def beginTransaction(): Future[String] = {
    val uri: Uri = baseUri + ":beginTransaction"
    val request = HttpRequest.apply(HttpMethods.POST, uri)
    for {
      response <- singleRequest(request.addCredentials(OAuth2BearerToken(accessToken.accessToken)))
      transactionResponse <- handleErrorOrUnmarshal[BeginTransactionResponse](response)
    } yield transactionResponse.transaction
  }
}
