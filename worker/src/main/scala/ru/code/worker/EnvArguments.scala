package ru.code.worker

object EnvArguments {
  lazy val nodeIndex: Int  = System.getenv("NODE_INDEX").toInt
  lazy val nodesCount: Int = System.getenv("NODES_COUNT").toInt
}