package ru.code.bot.parser

import ru.code.bot.domain.{EventValueTypeError, PublishEventRequest, RequestParsingError, WrongRequestFormatError}
import scala.util.{Failure, Success, Try}

trait Parser[T] {
  def parse(s: String): Either[RequestParsingError, T]
}

object Parser {
  implicit val eventParser: Parser[PublishEventRequest] = (s: String) => {
    val parts = s
      .replaceFirst("/post ", "")
      .split(":")
    val eventName = parts.dropRight(1).mkString

    if (parts.length < 2) {
      Left(WrongRequestFormatError)
    } else {
      parts.lastOption match {
        case Some(part) => Try(part.toInt) match {
          case Success(eventValue) => Right(
            PublishEventRequest(
              eventName  = eventName,
              eventValue = eventValue
            )
          )
          case Failure(_)          => Left(EventValueTypeError(part))
        }
        case None       => Left(WrongRequestFormatError)
      }
    }
  }
}