package ru.code.worker.wire

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, Resource, Timer}
import cats.effect.concurrent.Ref
import fs2.Stream
import fs2.kafka._
import ru.code.KafkaTopic
import ru.code.worker.stream.CounterStream
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.instances.list._
import org.http4s.server.blaze.BlazeServerBuilder
import ru.code.worker.http.StatController
import org.http4s.implicits._
import ru.code.worker.config.Config
import ru.code.worker.service.{EventService, EventStatService}
import ru.code.worker.utils.{HashFunctions, LinearProbabilisticCounter, TopicManager}
import sun.security.util.BitArray
import ru.code.domain._
import scala.concurrent.ExecutionContext
import scala.language.higherKinds

case class Wiring[F[_]](streams: Stream[F, Stream[F, Unit]],
                        server: Stream[F, ExitCode],
                        topicManager: Resource[F, TopicManager[F]])

object Wiring {

  def wiring[F[_]
    : Timer
    : ContextShift
  ](nodeIndex: Int,
    nodesCount: Int,
    config: Config,
    executionContext: ExecutionContext,
    blocker: Blocker
  )(implicit F: ConcurrentEffect[F]): F[Wiring[F]] = {
    val settings = ConsumerSettings(Deserializer[F], Deserializer[F])
      .withAllowAutoCreateTopics(false)
      .withEnableAutoCommit(false)
      .withBootstrapServers(config.kafkaBootstrapServers)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withGroupId("event-consumers")

    val counter = new LinearProbabilisticCounter(
      length = 95000, // according to LinearProbabilisticCounterSpec test
      hash   = HashFunctions.murmur2
    )

    (0 until KafkaTopic.events.partitions)
      .filter(_ % nodesCount == nodeIndex % nodesCount)
      .toList
      .traverse { partition =>
        Ref.of(Map.empty[EventName, (Offset, BitArray)]).map { ref =>
          val eventService  = EventService(ref, counter)
          val stream        = CounterStream(
            settings      = settings,
            partition     = partition,
            topic         = KafkaTopic.events,
            eventService  = eventService
          )

          ref -> stream
        }
      }
      .map { refsWithStreams =>
        val (refs, streams)     = refsWithStreams.unzip
        val eventStatService    = EventStatService(
          nodeIndex   = nodeIndex % nodesCount,
          nodesCount  = nodesCount,
          refs        = refs,
          counter     = counter
        )
        val httpRoute           = StatController(eventStatService).routes.orNotFound
        val adminClientSettings = AdminClientSettings[F]
          .withBlocker(blocker)
          .withBootstrapServers(config.kafkaBootstrapServers)

        val adminClient         = adminClientResource(adminClientSettings)
        val topicManager        = adminClient.map(TopicManager(_))
        val server              = BlazeServerBuilder[F]
            .bindHttp(config.httpPort, config.httpHost)
            .withHttpApp(httpRoute)
            .withExecutionContext(executionContext)
            .serve

        Wiring(
          streams      = Stream.emits(streams),
          server       = server,
          topicManager = topicManager
        )
      }
  }
}