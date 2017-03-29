package nl.gideondk.nimbus.model

import spray.json.DefaultJsonProtocol



final case class Key(partitionId: PartitionId, path: Seq[PathElement])




