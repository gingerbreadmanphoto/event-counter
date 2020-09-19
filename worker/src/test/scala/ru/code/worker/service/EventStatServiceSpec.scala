package ru.code.worker.service

import cats.effect.IO
import cats.effect.concurrent.Ref
import ru.code.domain.{EventName, EventStat, NodeEventStat, Offset}
import ru.code.worker.utils.{HashFunctions, LinearProbabilisticCounter}
import sun.security.util.BitArray
import utest._

object EventStatServiceSpec extends TestSuite {
  override def tests: Tests = Tests {
    test("service should return stat according to stored events") {
      val firstMask   = new BitArray(50)
      firstMask.set(2, true)
      firstMask.set(4, true)
      val secondMask  = new BitArray(50)
      secondMask.set(3, true)
      secondMask.set(5, true)
      secondMask.set(7, true)

      val firstRef      = Ref.unsafe[IO, Map[EventName, (Offset, BitArray)]](Map("first" -> (4L, firstMask)))
      val secondRef     = Ref.unsafe[IO, Map[EventName, (Offset, BitArray)]](Map("second" -> (14L, secondMask)))
      val counter       = new LinearProbabilisticCounter(50, HashFunctions.murmur2)

      val eventStatService = EventStatService(
        nodeIndex   = 0,
        nodesCount  = 1,
        refs        = List(firstRef, secondRef),
        counter     = counter
      )

      val sortedStat: NodeEventStat => NodeEventStat = stat => {
        stat.copy(stats = stat.stats.sortBy(_.name))
      }

      val expectedResult = sortedStat(
        NodeEventStat(
          stats       = List(
            EventStat(
              name    = "first",
              count   = 2,
              offset  = 4
            ),
            EventStat(
              name    = "second",
              count   = 3,
              offset  = 14
            )
          ),
          nodeIndex   = 0,
          nodesCount  = 1
        )
      )

      eventStatService.get.flatMap { stat =>
        IO.delay(sortedStat(stat) ==> expectedResult)
      }.unsafeToFuture()
    }
  }
}
