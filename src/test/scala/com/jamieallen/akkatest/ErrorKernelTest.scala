package com.jamieallen.akkatest

import org.scalatest.junit.JUnitSuite
import org.junit.Test

import java.util.concurrent.TimeUnit
import org.multiverse.api.latches.StandardLatch
import akka.actor.{Actor, Death}
import Actor._

/**
 * @author Jamie Allen (jallen@chariotsolutions.com)
 */
object ErrorKernelTest {
  class FireWorkerException(msg: String) extends Exception(msg)
}

class ErrorKernelTest extends JUnitSuite {
  import ErrorKernelTest._
  import ErrorKernel._

  @Test
  def killWorkerShouldRestartMangerAndOtherWorkers = {
    println("******************************************** RESTART TEST")
    val timingLatch = new StandardLatch

    val producer = actorOf[Producer]
    val consumer = actorOf[Consumer]
    val errorKernel = actorOf(new ErrorKernel(null, producer, consumer)).start()
    errorKernel ! StartCacheRefresh

    timingLatch.tryAwait(1, TimeUnit.SECONDS)

    producer ! Death(producer, new FireWorkerException("Fire the Producer!"))

    timingLatch.tryAwait(3, TimeUnit.SECONDS)

    assert(producer.isRunning)
    assert(consumer.isRunning)
  }

  @Test
  def sendingStopMessageShouldStopAllChildActors = {
    println("******************************************** POISON PILL TEST")
    val timingLatch = new StandardLatch

    val producer = actorOf[Producer]
    val consumer = actorOf[Consumer]

    val errorKernel = actorOf(new ErrorKernel(null, producer, consumer)).start()
    errorKernel ! StartCacheRefresh
    timingLatch.tryAwait(1, TimeUnit.SECONDS)

    errorKernel ! StopCacheRefresh
    timingLatch.tryAwait(3, TimeUnit.SECONDS)

    assert(producer.isShutdown)
    assert(consumer.isShutdown)
  }
}