package ru.code.worker.utils

import java.nio.ByteBuffer
import ru.code.worker.utils.HashFunctions.HashFunction
import sun.security.util.BitArray

class LinearProbabilisticCounter(val length: Int,
                                 hash: HashFunction) {
  def updateMask(mask: BitArray, value: Long): Unit = {
    val bytes = ByteBuffer.allocate(java.lang.Long.SIZE)
    bytes.putLong(value)
    val bitIndex = hash(bytes.array()) % length
    mask.set(bitIndex, true)
  }

  def count(mask: BitArray): Long = {
    // Sorry about that ...
    var counter = 0
    var i       = 0
    while (i < length) {
      if (mask.get(i)) {
        counter += 1
      }
      i += 1
    }
    Math.round(length * Math.log(length * 1.0 / (length - counter)))
  }
}