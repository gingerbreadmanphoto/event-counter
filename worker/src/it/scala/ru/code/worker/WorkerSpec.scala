package ru.code.worker

import cats.effect.{Blocker, ContextShift, IO, Resource, Timer}
import org.http4s.client.blaze.BlazeClientBuilder
import ru.code.KafkaTopic
import ru.code.worker.config.Config
import ru.code.worker.wire.Wiring
import utest._
import cats.instances.list._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import com.dimafeng.testcontainers.KafkaContainer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import fs2.kafka._
import org.http4s.Uri
import ru.code.domain.{Event, EventStat, NodeEventStat}
import org.http4s.circe.CirceEntityDecoder._
import org.testcontainers.containers.wait.strategy.{HostPortWaitStrategy, LogMessageWaitStrategy}

object WorkerSpec extends TestSuite {
  implicit val timer: Timer[IO]               = IO.timer(ExecutionContext.global)
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private def retry[T](count: Int, expression: IO[T])(expected: T): IO[Unit] = {
    def go(counter: Int): IO[Unit] = {
      expression
        .flatMap(result => IO.delay(result ==> expected))
        .recoverWith {
          case _: java.lang.AssertionError if counter < count => IO.sleep(500.millis) >> go(counter + 1)
        }
    }

    go(0)
  }

  override def tests: Tests = Tests {
    test("service should process events and correctly count them") {
      val kafkaContainer        = new KafkaContainer()
        .configure(_.waitingFor(new HostPortWaitStrategy()))
      val blocker               = Blocker.liftExecutionContext(ExecutionContext.global)

      val wireResources = for {
        _      <- Resource.make(IO.delay(kafkaContainer.start()))(_ => IO.delay(kafkaContainer.stop()))
        client <- BlazeClientBuilder[IO](ExecutionContext.global).resource
        wiring <- Resource.liftF(
          Wiring.wiring[IO](
            nodeIndex   = 0,
            nodesCount  = 1,
            config      = Config(
              kafkaBootstrapServers = kafkaContainer.bootstrapServers,
              httpPort              = 8090,
              httpHost              = "localhost"
            ),
            executionContext = ExecutionContext.global,
            blocker          = blocker
          )
        )
        topicManager <- wiring.topicManager
        _            <- Resource.liftF(topicManager.create(KafkaTopic.events))
        _            <- Resource.make(wiring.server.compile.drain.start)(_.cancel)
        _            <- Resource.make(wiring.streams.parJoinUnbounded.compile.drain.start)(_.cancel)
        producer     <- producerResource[IO, Array[Byte], Array[Byte]](
          ProducerSettings[IO, Array[Byte], Array[Byte]]
            .withBlocker(blocker)
            .withBootstrapServers(kafkaContainer.bootstrapServers)
        )
      } yield (client, producer)

      wireResources.use { case (client, producer) =>

        val eventRecords = List(
          Event("first", 1),
          Event("first", 1),
          Event("first", 2),
          Event("first", 3),
          Event("second", 10),
          Event("second", 10),
          Event("second", 10),
          Event("second", 13)
        ).map(KafkaTopic.events.encode)

        val sortedStat: NodeEventStat => NodeEventStat = stat => {
          stat.copy(stats = stat.stats.sortBy(_.name))
        }

        val expectedResult = sortedStat(
          NodeEventStat(
            stats       = List(
              EventStat("first", 3, 4),
              EventStat("second", 2, 4)
            ),
            nodeIndex   = 0,
            nodesCount  = 1
          )
        )

        producer.produce(ProducerRecords(eventRecords)).flatten >> retry(
          5, client.expect[NodeEventStat](Uri.unsafeFromString("http://localhost:8090/stat"))
            .map(sortedStat)
        )(expectedResult)
      }.unsafeToFuture()
    }
  }
}