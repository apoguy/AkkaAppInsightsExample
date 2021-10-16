package com.example

import akka.actor.Actor

class AkkaActor extends Actor {

  def performRequest(): Unit = AkkaAppInsightsExample.performRequest(context.system)

  override def receive: Receive = {
    case "PerformRequest" => performRequest();
  }
}
