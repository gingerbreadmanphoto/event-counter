package ru.code.bot.utils

import ru.code.domain.{EventStat, NodeEventStat}
import utest._

object NodeEventStatMergerSpec extends TestSuite {
  override def tests: Tests = Tests {
    test("service should merge stat from replica nodes according to the event offset") {
      val fistMainNodeStat = NodeEventStat(
        stats       = List(
          EventStat(
            name = "first",
            count = 2,
            offset = 1
          ),
          EventStat(
            name = "second",
            count = 3,
            offset = 10
          ),
          EventStat(
            name = "third",
            count = 9,
            offset = 99
          )
        ),
        nodeIndex   = 0,
        nodesCount  = 2
      )
      val firstReplicaNodeStat = NodeEventStat(
        stats       = List(
          EventStat(
            name = "first",
            count = 3,
            offset = 2
          ),
          EventStat(
            name = "second",
            count = 2,
            offset = 9
          ),
          EventStat(
            name = "forth",
            count = 12,
            offset = 100
          )
        ),
        nodeIndex   = 0,
        nodesCount  = 2
      )
      val secondsMainStat = NodeEventStat(
        stats       = List(
          EventStat(
            name = "fifth",
            count = 8,
            offset = 98
          )
        ),
        nodeIndex   = 1,
        nodesCount  = 2
      )


      val expectedResult = List(
        NodeEventStat(
          stats       = List(
            EventStat(
              name = "first",
              count = 3,
              offset = 2
            ),
            EventStat(
              name = "second",
              count = 3,
              offset = 10
            ),
            EventStat(
              name = "third",
              count = 9,
              offset = 99
            ),
            EventStat(
              name = "forth",
              count = 12,
              offset = 100
            )
          ),
          nodeIndex   = 0,
          nodesCount  = 2
        ),
        NodeEventStat(
          stats       = List(
            EventStat(
              name = "fifth",
              count = 8,
              offset = 98
            )
          ),
          nodeIndex   = 1,
          nodesCount  = 2
        )
      )

      val rawStat = List(
        fistMainNodeStat,
        firstReplicaNodeStat,
        secondsMainStat
      )

      val sortedStat: NodeEventStat => NodeEventStat = stat => {
        stat.copy(stats = stat.stats.sortBy(_.name))
      }

      NodeEventStatMerger.merge(rawStat).map(sortedStat) ==> expectedResult.map(sortedStat)
    }
  }
}