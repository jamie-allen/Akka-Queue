package com.jamieallen.akkatest

import akka.actor._
import akka.config.Supervision.{AllForOneStrategy, Permanent, SupervisorConfig, Supervise}
import com.weiglewilczek.slf4s.Logging
import impl._
import org.multiverse.api.latches.StandardLatch
import java.util.concurrent.TimeUnit

object ErrorKernel {
  sealed trait ApplicationManagementMessage
  case object StartCacheRefresh extends ApplicationManagementMessage
  case object StopCacheRefresh extends ApplicationManagementMessage

  trait JamieActor extends Actor with Logging {
    override def preStart() { logger.debug("Starting up") }
    override def postStop() { logger.debug("Shutting down") }
    override def postRestart(reason: Throwable) { logger.debug("Restarted, reason: " + reason.getMessage) }
  }

  class Consumer extends JamieActor with Logging {
    val timingLatch = new StandardLatch

    def receive = {
      case (TakeNextFromQueue) =>
        logger.debug("Received TakeNextFromQueue")
        timingLatch.tryAwait(250, TimeUnit.MILLISECONDS)
        self ! TakeNextFromQueue
    }

    override def postRestart(reason: Throwable) {
      super.postRestart(reason)
      self ! TakeNextFromQueue
    }
  }

  class Producer extends JamieActor with Logging {
    self.receiveTimeout = Some(15000)
    val timingLatch = new StandardLatch

    def receive = {
      case (GetUpdates) =>
        logger.debug("Recieved GetUpdates")
        timingLatch.tryAwait(250, TimeUnit.MILLISECONDS)
        self ! GetUpdates
    }

    override def postRestart(reason: Throwable) {
      super.postRestart(reason)
      self ! GetUpdates
    }
  }
}

import ErrorKernel._
class ErrorKernel(val jdbcTemplate: Any, val producer: ActorRef, val consumer: ActorRef) extends JamieActor with Logging {
  self.lifeCycle = Permanent

  private val supervisor: Supervisor = Supervisor(
    SupervisorConfig(
      AllForOneStrategy(List(classOf[Throwable]), 10, 5000),
      Supervise(producer, Permanent) ::
      Supervise(consumer, Permanent) ::
      Nil))

  def receive = {
    case StartCacheRefresh =>
      logger.debug("Received StartCacheRefresh message")
      producer ! GetUpdates
      consumer ! TakeNextFromQueue
    case StopCacheRefresh =>
      logger.debug("Received StopCacheRefresh message, stopping actors")
      producer ! PoisonPill
      consumer ! PoisonPill
      logger.debug("Finished stopping actors")
    case x => logger.debug("ErrorKernel received undefined message: " + x)
  }

  override def postRestart(reason: Throwable) {
    super.postRestart(reason)
    if (producer.isShutdown) producer.start()
    if (consumer.isShutdown) consumer.start()
    self ! StartCacheRefresh
  }
}