package ru.code.bot.wire

import canoe.models.Update
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Timer}
import scala.language.higherKinds
import fs2._
import org.http4s.client.blaze.BlazeClientBuilder
import fs2.kafka._
import ru.code.bot.service.{EventService, EventStatService}
import ru.code.bot.config.Config
import ru.code.bot.stream.BotStream
import ru.code.bot.utils.WorkerResolver
import scala.concurrent.ExecutionContext

case class Wiring[F[_]](botStream: Stream[F, Update])

object Wiring {
  def wiring[F[_]
    : ConcurrentEffect
    : Timer
    : ContextShift
  ](executionContext: ExecutionContext,
    workerResolver: WorkerResolver[F],
    config: Config,
    blocker: Blocker
  ): Stream[F, Wiring[F]] = {
    val client           = BlazeClientBuilder(executionContext).resource
    val producerSettings = ProducerSettings[F, Array[Byte], Array[Byte]]
      .withBootstrapServers(config.kafkaBootstrapServers)
      .withBlocker(blocker)
    val producer         = producerStream(producerSettings)

    val eventServiceStream     = producer.map(EventService(_))
    val eventStatServiceStream = Stream.resource(client)
      .map(EventStatService(_, workerResolver))

    for {
      eventService     <- eventServiceStream
      eventStatService <- eventStatServiceStream
      botStream        <- Stream.emit(
        BotStream(
          eventService     = eventService,
          eventStatService = eventStatService,
          botToken         = config.botToken
        )
      )
    } yield Wiring(
      botStream = botStream
    )
  }
}