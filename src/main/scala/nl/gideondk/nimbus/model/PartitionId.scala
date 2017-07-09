package nl.gideondk.nimbus.model

final case class PartitionId(projectId: String, namespaceId: Option[String])

object PartitionId {
  def apply(projectId: String): PartitionId = PartitionId(projectId, None)
}