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

import nl.gideondk.nimbus.model.{ Key, PathElementId }
import scala.concurrent.ExecutionContext.Implicits.global

class AllocateIdsSpec extends NimbusSpec {
  "Allocation of IDs" should {
    "return the identity function of its input" in {
      val keys = Seq(Key.incomplete(projectId, "testEntity"), Key.incomplete(projectId, "testEntity"), Key.incomplete(projectId, "anotherTestEntity"))
      client.allocateIds(keys).map { response =>
        response.length shouldEqual 3
        response(0).path.length shouldEqual 1
        response(0).path(0).id.get.asInstanceOf[PathElementId].value should be > 0l
        response(1).path(0).id.get.asInstanceOf[PathElementId].value should be > 0l
        response(2).path(0).id.get.asInstanceOf[PathElementId].value should be > 0l
      }
    }
  }
}
