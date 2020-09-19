package ru.code.worker.utils

import cats.effect.Sync
import fs2.kafka.KafkaAdminClient
import ru.code.KafkaTopic
import cats.syntax.applicativeError._
import org.apache.kafka.common.errors.TopicExistsException
import org.slf4j.{Logger, LoggerFactory}
import scala.language.higherKinds

trait TopicManager[F[_]] {
  def create(topic: KafkaTopic[_]): F[Unit]
}

object TopicManager {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def apply[F[_]](client: KafkaAdminClient[F])(implicit F: Sync[F]): TopicManager[F] = new TopicManager[F] {
    override def create(topic: KafkaTopic[_]): F[Unit] = {
      client.createTopic(topic.repr)
        .recoverWith {
          case _:TopicExistsException => F.delay(logger.debug(s"Topic ${topic.name} already exists"))
        }
    }
  }
}