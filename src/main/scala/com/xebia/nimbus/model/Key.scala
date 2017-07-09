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

package com.xebia.nimbus.model

final case class Key(partitionId: PartitionId, path: Seq[PathElement])

object Key {
  def named(projectId: String, entityKind: String, name: String) =
    Key(PartitionId(projectId, None), Seq(PathElement(entityKind, Some(PathElementName(name)))))

  def named(projectId: String, namespace: String, entityKind: String, name: String) =
    Key(PartitionId(projectId, Some(namespace)), Seq(PathElement(entityKind, Some(PathElementName(name)))))

  def incomplete(projectId: String, entityKind: String) =
    Key(PartitionId(projectId, None), Seq(PathElement(entityKind, None)))

  def incomplete(projectId: String, namespace: String, entityKind: String) =
    Key(PartitionId(projectId, Some(namespace)), Seq(PathElement(entityKind, None)))
}

