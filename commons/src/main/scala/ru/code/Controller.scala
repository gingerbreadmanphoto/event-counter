package ru.code

import org.http4s.HttpRoutes
import scala.language.higherKinds

trait Controller[F[_]] {
  def routes: HttpRoutes[F]
}