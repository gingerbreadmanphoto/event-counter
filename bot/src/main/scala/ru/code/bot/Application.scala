package ru.code.bot

import java.util.concurrent.Executors
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource, SyncIO}
import cats.syntax.flatMap._
import scala.concurrent.ExecutionContext
import fs2._
import org.slf4j.{Logger, LoggerFactory}
import ru.code.bot.config.Config
import ru.code.bot.utils.WorkerResolver
import ru.code.bot.wire.Wiring

object Application extends IOApp.WithContext {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  override protected def executionContextResource: Resource[SyncIO, ExecutionContext] = Resource.liftF(SyncIO(scala.concurrent.ExecutionContext.global))

  override def run(args: List[String]): IO[ExitCode] = {
    (
      for {

        blocker                 <- Stream.bracket(IO.delay(Executors.newCachedThreadPool()))(executor => IO.delay(executor.shutdown()))
          .map(ExecutionContext.fromExecutor)
          .map(Blocker.liftExecutionContext)
        workerResolver          <- Stream.emit(WorkerResolver[IO])
        config                  <- Stream.emit(
          Config(
            kafkaBootstrapServers = "kafka:9092",
            botToken              = "1235449311:AAFJaaYu5IbhtrklInHAoriFspd7y316BwA"
          )
        )
        wiring                  <- Wiring.wiring[IO](
          executionContext = executionContext,
          config           = config,
          blocker          = blocker,
          workerResolver   = workerResolver
        )
        _                       <- Stream.eval(IO.delay(logger.debug(s"Bot started...")))
        _                       <- wiring.botStream
      } yield ()
    )
    .compile
    .drain
    .attempt
    .flatMap {
      case Right(_) =>
        IO.delay(logger.debug(s"Bot finished")) >>
        IO.pure(ExitCode.Success)
      case Left(err) =>
        IO.delay(logger.error(s"Bot stopped", err)) >>
        IO.pure(ExitCode.Error)
    }
  }
}