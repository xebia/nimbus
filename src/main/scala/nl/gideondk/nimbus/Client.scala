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

package nl.gideondk.nimbus

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, OverflowStrategy }
import nl.gideondk.nimbus.Connection.AccessToken
import nl.gideondk.nimbus.api._

class Client(
  val accessToken:      AccessToken,
  val projectId:        String,
  val overflowStrategy: OverflowStrategy,
  val maximumInFlight:  Int
)(implicit val system: ActorSystem)
  extends Connection with TransactionApi
  with AllocateIdsApi
  with CommitApi
  with LookupApi
  with QueryApi {

  implicit val mat = ActorMaterializer()
  val apiHost = "www.googleapis.com"
  val apiPort = 443
  val datastoreAPIEndPoint = s"https://$apiHost/auth/datastore"
  val googleAPIEndPoint = s"https://$apiHost"
}

object Client {
  def apply(accessToken: AccessToken, projectId: String)(implicit system: ActorSystem) = {
    new Client(accessToken, projectId, OverflowStrategy.dropNew, 1024)
  }
}

