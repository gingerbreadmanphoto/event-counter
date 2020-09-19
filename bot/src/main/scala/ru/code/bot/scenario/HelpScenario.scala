package ru.code.bot.scenario

import scala.language.higherKinds
import canoe.api._
import canoe.syntax._

object HelpScenario {
  def apply[F[_]: TelegramClient](): Scenario[F, Unit] = {
    for {
      chat   <- Scenario.expect(command("help").chat)
      _      <- Scenario.eval(chat.send(help))
    } yield ()
  }

  private[this] val help =
    """
      |/post Posting new event string:integer (e.g. eventName:10)
      |/stat Get event stats from all available nodes
    """.stripMargin
}