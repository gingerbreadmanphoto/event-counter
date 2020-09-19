package ru.code.bot.utils

import cats.effect.Sync
import org.http4s.Uri
import ru.code.bot.EnvArguments
import scala.language.higherKinds

trait WorkerResolver[F[_]] {
  def resolve: F[List[Uri]]
}

object WorkerResolver {
  def apply[F[_]](implicit F: Sync[F]): WorkerResolver[F] = new WorkerResolver[F] {
    override def resolve: F[List[Uri]] = F.delay(
      EnvArguments.workerHosts.map(Uri.unsafeFromString).toList
    )
  }
}