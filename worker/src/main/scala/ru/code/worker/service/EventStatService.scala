package ru.code.worker.service

import cats.effect.concurrent.Ref
import cats.instances.list._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.Monad
import ru.code.domain.{EventStat, NodeEventStat}
import ru.code.worker.utils.LinearProbabilisticCounter
import sun.security.util.BitArray
import scala.language.higherKinds
import ru.code.domain._

trait EventStatService[F[_]] {
  def get: F[NodeEventStat]
}

object EventStatService {
  def apply[F[_] : Monad](nodeIndex: Int,
                          nodesCount: Int,
                          refs: List[Ref[F, Map[EventName, (Offset, BitArray)]]],
                          counter: LinearProbabilisticCounter): EventStatService[F] = new EventStatService[F] {
    override def get: F[NodeEventStat] = {
      refs
        .flatTraverse { ref =>
          ref.get
            .map { events =>
              events.map { case (eventName, (offset, mask)) =>
                EventStat(
                  name   = eventName,
                  count  = counter.count(mask),
                  offset = offset
                )
              }.toList
            }
        }
        .map { stats =>
          NodeEventStat(
            stats      = stats,
            nodeIndex  = nodeIndex,
            nodesCount = nodesCount
          )
        }
    }
  }
}