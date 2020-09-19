package ru.code.worker.config

case class Config(kafkaBootstrapServers: String,
                  httpPort: Int,
                  httpHost: String)
