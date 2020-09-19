package ru.code.bot.service

import cats.effect.Sync
import fs2.kafka.{KafkaProducer, ProducerRecords}
import ru.code.KafkaTopic
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import ru.code.bot.domain.PublishEvent
import ru.code.domain.Event
import scala.language.higherKinds

trait EventService[F[_]] {
  def create(event: Event): F[PublishEvent]
}

object EventService {
  def apply[F[_]](producer: KafkaProducer[F, Array[Byte], Array[Byte]])(implicit F: Sync[F]): EventService[F] = new EventService[F] {
    override def create(event: Event): F[PublishEvent] = {
      val record = KafkaTopic.events.encode(event)
      producer.produce(ProducerRecords.one(record))
        .flatten
        .attempt
        .map {
          case Right(r)  => PublishEvent.Success(event)
          case Left(err) => PublishEvent.Failure(event, err)
        }
    }
  }
}