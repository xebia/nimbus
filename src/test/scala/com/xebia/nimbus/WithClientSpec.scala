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

package com.xebia.nimbus

import akka.actor.ActorSystem
import org.scalatest._
import Query._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.xebia.nimbus.datastore.api.OAuthApi
import com.xebia.nimbus.datastore.api.OAuthApi.Credentials

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait WithClientSpec extends AsyncWordSpec with Matchers {
  val projectId = "test-project"
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val client = new EmulatorPointedRawClient(projectId)

  val namespace = "NimbusTest"

  val nimbus = new Nimbus(namespace, client)

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

    val futureTestResult = (for {
      _ <- nimbus.querySource[Entity](Q.kindOf('$TestObject)).mapAsync(64)(x => nimbus.delete(x.path)).runWith(Sink.ignore)
    } yield {
    }) flatMap { result =>
      super.withFixture(test).toFuture
    }
    new FutureOutcome(futureTestResult)
  }
}

object EmulatorPointedRawClient {

  import java.security.{KeyPairGenerator, SecureRandom}

  def generatePrivateKey() = {
    val keyGen = KeyPairGenerator.getInstance("DSA", "SUN")
    val random = SecureRandom.getInstance("SHA1PRNG", "SUN")
    keyGen.initialize(1024, random)
    val pair = keyGen.generateKeyPair
    pair.getPrivate
  }
}

class EmulatorPointedRawClient(override val projectId: String, override val maximumInFlight: Int = 1024)(implicit val sys: ActorSystem) extends
  RawClient(Credentials("testuser", EmulatorPointedRawClient.generatePrivateKey), projectId, OverflowStrategy.backpressure, maximumInFlight)(sys) {
  override val apiHost = "localhost"
  override val apiPort = 8080
  override val datastoreAPIEndPoint = s"http://localhost:8080"
  override val googleAPIEndPoint = s"http://localhost:8080"

  override lazy val requestQueue = Source.queue[(HttpRequest, Promise[HttpResponse])](maximumInFlight, overflowStrategy)
    .via(poolClientFlow)
    .toMat(Sink.foreach({
      case ((Success(resp), p)) => p.success(resp)
      case ((Failure(e), p))    => p.failure(e)
    }))(Keep.left)
    .run
}