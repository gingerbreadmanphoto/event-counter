package ru.code.bot.service

import cats.effect.Sync
import org.http4s.client.Client
import ru.code.domain.NodeEventStat
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import cats.syntax.applicativeError._
import cats.instances.list._

import scala.language.higherKinds
import org.http4s.circe.CirceEntityDecoder._
import ru.code.bot.utils.{NodeEventStatMerger, WorkerResolver}

trait EventStatService[F[_]] {
  def get: F[List[NodeEventStat]]
}

object EventStatService {
  def apply[F[_]](client: Client[F], workerResolver: WorkerResolver[F])(implicit F: Sync[F]): EventStatService[F] = new EventStatService[F] {
    override def get: F[List[NodeEventStat]] = {
      workerResolver.resolve
        .flatMap { workers =>
          workers
            .traverse { workerUri =>
              client.expect[NodeEventStat](workerUri.addPath("stat")).attempt
            }
            .map { stats =>
              val successfulAnswers = stats.collect { case Right(stat) => stat }
              NodeEventStatMerger.merge(successfulAnswers)
            }
        }
    }
  }
}