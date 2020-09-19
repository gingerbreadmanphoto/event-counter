package ru.code.worker.service

import cats.effect.IO
import cats.effect.concurrent.Ref
import fs2.Chunk
import ru.code.domain.{Event, EventName, Offset}
import ru.code.worker.utils.{HashFunctions, LinearProbabilisticCounter}
import sun.security.util.BitArray
import utest._
import cats.syntax.flatMap._

object EventServiceSpec extends TestSuite {
  override def tests: Tests = Tests {
    test("should process events and set correct bits") {
      val ref     = Ref.unsafe[IO, Map[EventName, (Offset, BitArray)]](Map.empty)
      val counter = new LinearProbabilisticCounter(10, HashFunctions.murmur2)
      val eventService = EventService[IO](ref, counter)

      (
        eventService.process(
          Chunk(
            (0, Event("first", 9)),
            (2, Event("first", 10)),
            (3, Event("second", 100))
          )
        ) >> ref.get.flatMap { store =>
          IO.delay(store.size ==> 2) >>
          IO.delay {
            val (firstOffset, firstMask) = store("first")
            firstOffset ==> 2
            counter.count(firstMask) ==> 2
          } >>
          IO.delay {
            val (secondOffset, secondMask) = store("second")
            secondOffset ==> 3
            counter.count(secondMask) ==> 1
          }
        }
      ).unsafeToFuture()
    }
  }
}