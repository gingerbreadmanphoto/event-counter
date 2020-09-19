package ru.code.worker

import java.util.concurrent.Executors
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource, SyncIO}
import fs2.Stream
import org.slf4j.{Logger, LoggerFactory}
import ru.code.KafkaTopic
import ru.code.worker.config.Config
import ru.code.worker.wire.Wiring
import scala.concurrent.ExecutionContext
import cats.syntax.flatMap._

object Application extends IOApp.WithContext {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  override protected def executionContextResource: Resource[SyncIO, ExecutionContext] = Resource.liftF(SyncIO(scala.concurrent.ExecutionContext.global))

  override def run(args: List[String]): IO[ExitCode] = {

    (
      for {
        blocker                 <- Stream.bracket(IO.delay(Executors.newCachedThreadPool()))(executor => IO.delay(executor.shutdown()))
          .map(ExecutionContext.fromExecutor)
          .map(Blocker.liftExecutionContext)
        config                  = Config("kafka:9092", 8090, "0.0.0.0")
        wiring                  <- Stream.eval(
          Wiring.wiring[IO](
            nodeIndex         = EnvArguments.nodeIndex,
            nodesCount        = EnvArguments.nodesCount,
            config            = config,
            executionContext  = executionContext,
            blocker           = blocker
          )
        )
        topicManager            <- Stream.resource(wiring.topicManager)
        _                       <- Stream.eval(topicManager.create(KafkaTopic.events))
        _                       <- Stream.eval {
          IO.delay {
            logger.debug(
              s"""
                |Worker ${EnvArguments.nodeIndex} started
                |NodesCount: ${EnvArguments.nodesCount}
              """.stripMargin
            )
          }
        }
        _                       <- wiring.streams.parJoinUnbounded.concurrently(wiring.server)
      } yield ()
    )
    .compile
    .drain
    .attempt
    .flatMap {
      case Right(_) =>
        IO.delay(logger.debug(s"Worker ${EnvArguments.nodeIndex} finished")) >>
        IO.pure(ExitCode.Success)
      case Left(err) =>
        IO.delay(logger.error(s"Worker ${EnvArguments.nodeIndex} stopped", err)) >>
        IO.pure(ExitCode.Error)
    }
  }
}