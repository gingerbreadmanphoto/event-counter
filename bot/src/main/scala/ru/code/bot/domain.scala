package ru.code.bot

import cats.Show
import io.circe.Printer
import ru.code.domain.Event
import io.circe.syntax._

object domain {
  sealed trait PublishEvent {
    def event: Event
  }
  object PublishEvent {
    case class Success(event: Event) extends PublishEvent
    case class Failure(event: Event, err: Throwable) extends PublishEvent

    private[this] def printEvent(event: Event): String = {
      Printer.spaces2.print(event.asJson)
    }
    implicit val show: Show[PublishEvent] = {
      case Success(event)      => s"${printEvent(event)} has been published"
      case Failure(event, err) => s"${printEvent(event)} hasn't been published because of ${err.getMessage}"
    }
  }

  sealed trait RequestParsingError {
    def message: String
  }

  case object WrongRequestFormatError extends RequestParsingError {
    override val message: String = "Wrong request format"
  }

  case class EventValueTypeError(value: String) extends RequestParsingError {
    override val message: String = s"Expected: Integer\nGot: $value"
  }

  case class PublishEventRequest(eventName: String,
                                 eventValue: Long)

  object PublishEventRequest {
    def toEvent(request: PublishEventRequest): Event = {
      Event(
        name  = request.eventName,
        value = request.eventValue
      )
    }
  }
}