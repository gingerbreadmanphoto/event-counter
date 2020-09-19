package ru.code

import fs2.kafka.{ConsumerRecord, ProducerRecord}
import io.circe.Printer
import ru.code.domain.Event
import io.circe.jawn.parseByteArray
import io.circe.syntax._
import cats.syntax.either._
import org.apache.kafka.clients.admin.NewTopic

trait KafkaTopic[T] {
  def repr: NewTopic = new NewTopic(
    name,
    partitions,
    replicationFactor
  )
  def replicationFactor: Short
  def name: String
  def partitions: Int
  def encode(value: T): ProducerRecord[Array[Byte], Array[Byte]]
  def decode(record: ConsumerRecord[Array[Byte], Array[Byte]]): Either[io.circe.Error, T]
}

object KafkaTopic {
  val events: KafkaTopic[Event] = new KafkaTopic[Event] {
    override val name: String             = "events"
    override val partitions: Int          = 5
    override val replicationFactor: Short = 1

    override def encode(value: Event): ProducerRecord[Array[Byte], Array[Byte]] = {
      ProducerRecord(
        topic = name,
        key   = value.name.getBytes(),
        value = Printer.noSpaces.print(value.asJson).getBytes()
      )
    }

    override def decode(record: ConsumerRecord[Array[Byte], Array[Byte]]): Either[io.circe.Error, Event] = {
      parseByteArray(record.value) match {
        case Right(json) => json.as[Event]
        case Left(err)   => err.asLeft[Event]
      }
    }
  }
}