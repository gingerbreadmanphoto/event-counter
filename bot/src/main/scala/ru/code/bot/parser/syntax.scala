package ru.code.bot.parser

import ru.code.bot.domain.RequestParsingError

object syntax {
  implicit class ParserOps(val value: String) extends AnyVal {
    def parse[T](implicit parser: Parser[T]): Either[RequestParsingError, T] = {
      parser.parse(value)
    }
  }
}