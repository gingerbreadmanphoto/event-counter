package ru.code.worker.service

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.flatMap._
import fs2.Chunk
import ru.code.domain.Event
import ru.code.worker.utils.LinearProbabilisticCounter
import sun.security.util.BitArray
import scala.language.higherKinds
import ru.code.domain._

trait EventService[F[_]] {
  def process(events: Chunk[(Long, Event)]): F[Unit]
}

object EventService {
  def apply[F[_]](ref: Ref[F, Map[EventName, (Offset, BitArray)]],
                  counter: LinearProbabilisticCounter)(implicit F: Sync[F]): EventService[F] = new EventService[F] {
    override def process(events: Chunk[(Long, Event)]): F[Unit] = {
      ref.get.flatMap { store =>
        F.delay {
          events.toList
            .groupBy { case (_, event) => event.name }
            .foldLeft(store) { case (accStore, (eventName, groupEvents)) =>
              val bitArray = accStore.get(eventName) match {
                case Some((_, array)) => array.clone().asInstanceOf[BitArray]
                case None             => new BitArray(counter.length)
              }

              groupEvents.foreach { case (_, event) =>
                counter.updateMask(
                  mask  = bitArray,
                  value = event.value
                )
              }

              val nextOffset = groupEvents.maxBy { case(offset, _) => offset }._1

              accStore + (eventName -> (nextOffset, bitArray))
            }
        }
      }.flatMap(ref.set)
    }
  }
}