package com.jamieallen.akkatest

import akka.actor.Actor.actorOf
import com.weiglewilczek.slf4s.Logging
import akka.actor.{ActorRef, Actor, PoisonPill}

/**
 * Author: Jamie Allen (jallen@chariotsolutions.com)
 */
object Bootstrap extends Logging {
  import ErrorKernel._
  
  def main(args: Array[String]) {
    // Start up
    val producer = actorOf[Producer]
    val consumer = actorOf[Consumer]

	  // Execute application
    val errorKernel = actorOf(new ErrorKernel(null, producer, consumer)).start()
    errorKernel ! StartCacheRefresh
  	Thread.sleep(10000)
  	errorKernel ! StopCacheRefresh
    Actor.registry.shutdownAll()

	  // Shutdown cleanly
	  System.exit(0)
  }
}
