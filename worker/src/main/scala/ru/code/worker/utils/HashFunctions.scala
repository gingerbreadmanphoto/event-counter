package ru.code.worker.utils

import org.apache.kafka.common.utils.Utils

object HashFunctions {
  type HashFunction = Array[Byte] => Int
  val murmur2: HashFunction = bytes => Utils.toPositive(Utils.murmur2(bytes))
}