package ru.code.bot.utils

import ru.code.domain.NodeEventStat

object NodeEventStatMerger {
  def merge(stats: List[NodeEventStat]): List[NodeEventStat] = {
    stats
      .groupBy { nodeStat => (nodeStat.nodeIndex, nodeStat.nodesCount) }
      .map {
        case ((_, _), List(nodeStat))                     =>
          nodeStat
        case ((nodeIndex, nodesCount), replicaNodesStats) =>
          val mergedStats = replicaNodesStats
            .flatMap(_.stats)
            .groupBy(_.name)
            .map { case (_, replicaStats) => replicaStats.maxBy(_.offset) }
            .toList

          NodeEventStat(
            stats      = mergedStats,
            nodeIndex  = nodeIndex,
            nodesCount = nodesCount
          )
      }
      .toList
  }
}