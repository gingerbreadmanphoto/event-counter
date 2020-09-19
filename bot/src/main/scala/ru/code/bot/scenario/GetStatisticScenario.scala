package ru.code.bot.scenario

import scala.language.higherKinds
import canoe.api._
import canoe.syntax._
import io.circe.Printer
import ru.code.bot.service.EventStatService
import io.circe.syntax._

object GetStatisticScenario {
  def apply[F[_]: TelegramClient](eventStatService: EventStatService[F]): Scenario[F, Unit] = {
    for {
      chat  <- Scenario.expect(command("stat").chat)
      stats <- Scenario.eval(eventStatService.get)
      _     <- Scenario.eval(chat.send(Printer.spaces2.print(stats.asJson)))
    } yield ()
  }
}