package ru.code

import io.circe.generic.semiauto._
import io.circe.Codec

object domain {
  type Offset    = Long
  type EventName = String

  case class Event(name: String,
                   value: Long)

  object Event {
    implicit val codec: Codec[Event] = deriveCodec
  }

  case class EventStat(name: String,
                       count: Long,
                       offset: Long)

  object EventStat {
    implicit val codec: Codec[EventStat] = deriveCodec
  }

  case class NodeEventStat(stats: List[EventStat],
                           nodeIndex: Int,
                           nodesCount: Int)

  object NodeEventStat {
    implicit val codec: Codec[NodeEventStat] = deriveCodec
  }
}