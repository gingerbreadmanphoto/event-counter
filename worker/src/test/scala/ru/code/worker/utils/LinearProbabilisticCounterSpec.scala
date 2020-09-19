package ru.code.worker.utils

import sun.security.util.BitArray
import utest._
import scala.util.Random

object LinearProbabilisticCounterSpec extends TestSuite {
  override def tests: Tests = Tests {
    test("counter should count unique items with expected accuracy") {

      val rand        = Random
      val cardinality = 100000
      val length      = 95000
      (1 to 200).foreach { p =>
        var i           = 0
        val set         = collection.mutable.Set.empty[Long]
        val counter     = new LinearProbabilisticCounter(length, HashFunctions.murmur2)
        val mask        = new BitArray(length)

        while (i < cardinality) {
          val item = rand.nextLong()
          if (!set(item)) {
            set.add(item)
            counter.updateMask(mask, item)
            i += 1
          }
        }

        val accuracy = 100 - Math.abs(length - counter.count(mask)) * 1.0 / length
        println(accuracy)
        (accuracy >= 99.9) ==> true
      }
    }
  }
}