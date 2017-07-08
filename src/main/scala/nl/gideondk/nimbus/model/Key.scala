package nl.gideondk.nimbus.model

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


