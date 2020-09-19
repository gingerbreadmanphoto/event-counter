package ru.code.bot.scenario

import ru.code.bot.service.EventService
import scala.language.higherKinds
import canoe.api._
import canoe.models.Chat
import canoe.models.outgoing.TextContent
import canoe.syntax._
import cats.effect.Sync
import ru.code.bot.parser.syntax._
import ru.code.domain.Event
import cats.syntax.flatMap._
import cats.syntax.functor._
import ru.code.bot.domain.{PublishEvent, PublishEventRequest}

object PublishEventScenario {
  def apply[F[_]: Sync: TelegramClient](eventService: EventService[F]): Scenario[F, Unit] = {
    def post(event: Event, chat: Chat): F[Unit] = {
      eventService.create(event)
        .flatMap {
          case PublishEvent.Success(ev) =>
            chat.send(TextContent(s"$ev has been posted"))
          case PublishEvent.Failure(ev, err) =>
            chat.send(TextContent(s"$ev hasn't been published because of ${err.getMessage}"))
        }
        .void
    }

    for {
      message <- Scenario.expect(textMessage.when(_.text.startsWith("/post")))
      chat    = message.chat
      _       <-  message.text.parse[PublishEventRequest] match {
        case Right(event) => Scenario.eval(post(PublishEventRequest.toEvent(event), chat))
        case Left(err)    => Scenario.eval(chat.send(err.message).void)
      }
    } yield ()
  }
}