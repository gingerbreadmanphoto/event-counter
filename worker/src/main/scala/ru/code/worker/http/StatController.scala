package ru.code.worker.http

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import cats.syntax.flatMap._
import scala.language.higherKinds
import org.http4s.circe.CirceEntityEncoder._
import ru.code.Controller
import ru.code.worker.service.EventStatService

object StatController extends {
  def apply[F[_]
    : ConcurrentEffect
    : ContextShift
    : Timer
  ](eventStatService: EventStatService[F]): Controller[F] = new Controller[F] with Http4sDsl[F] {
    override def routes: HttpRoutes[F] = {
      HttpRoutes.of[F] {
        case GET -> Root / "stat" =>
          eventStatService.get.flatMap(Ok(_))
      }
    }
  }
}