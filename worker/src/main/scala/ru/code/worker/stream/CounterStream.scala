package ru.code.worker.stream

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.kafka.ConsumerSettings
import fs2.kafka._
import fs2.Stream
import ru.code.domain.Event
import ru.code.KafkaTopic
import cats.instances.int._
import ru.code.worker.service.EventService
import scala.language.higherKinds
import cats.data.NonEmptySet
import org.slf4j.{Logger, LoggerFactory}

object CounterStream {
  private[this] val logger: Logger = LoggerFactory.getLogger(getClass)

  def apply[F[_]
    : ContextShift
    : Timer
  ](settings: ConsumerSettings[F, Array[Byte], Array[Byte]],
    partition: Int,
    topic: KafkaTopic[Event],
    eventService: EventService[F]
  )(implicit F: ConcurrentEffect[F]): Stream[F, Unit] = {

    for {
      consumer <- consumerStream(settings)
      _        <- Stream.bracket(consumer.assign(topic.name, NonEmptySet.one(partition)))(_ => consumer.unsubscribe)
      _        <- Stream.eval(consumer.seekToBeginning)
      _        <- Stream.eval(F.delay(logger.info(s"Partition $partition has been assigned")))
      _        <- consumer.stream
        .map { ccr =>
          topic
            .decode(ccr.record)
            .map(ccr.offset.offsetAndMetadata.offset() -> _)
        }
        .evalTap(r => F.delay(logger.info(r.toString)))
        .collect { case Right(value) => value }
        .chunks
        .evalMap(eventService.process)
    } yield ()
  }
}