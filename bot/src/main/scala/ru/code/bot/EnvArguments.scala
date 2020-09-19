package ru.code.bot

object EnvArguments {
  lazy val workerHosts: Set[String] = System.getenv("WORKER_HOSTS").split(",").toSet
}