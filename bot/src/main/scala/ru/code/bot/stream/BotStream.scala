package ru.code.bot.stream

import ru.code.bot.service.{EventService, EventStatService}
import fs2._
import canoe.api._
import canoe.models.Update
import cats.effect.{ConcurrentEffect, Timer}
import ru.code.bot.scenario.{GetStatisticScenario, HelpScenario, PublishEventScenario}
import scala.language.higherKinds

object BotStream {
  def apply[F[_]
    : ConcurrentEffect
    : Timer
  ](eventService: EventService[F],
    eventStatService: EventStatService[F],
    botToken: String
  ): Stream[F, Update] = {

    def followStream(implicit client: TelegramClient[F]): Stream[F, Update] = {
      Bot.polling[F].follow(
        GetStatisticScenario(eventStatService),
        PublishEventScenario(eventService),
        HelpScenario()
      )
    }

    Stream
      .resource(TelegramClient.global[F](botToken))
      .flatMap(followStream(_))
  }
}