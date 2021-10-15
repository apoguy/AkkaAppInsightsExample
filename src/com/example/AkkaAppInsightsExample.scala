package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object AkkaAppInsightsExample {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("SingleRequest")
    implicit val executionContextExecutor: ExecutionContextExecutor = system.dispatcher

    system.scheduler.schedule(5 seconds, 1 seconds) {
      performRequest(system)
    }

  }

  def performRequest(system: ActorSystem): Unit = {
    implicit val executionContextExecutor: ExecutionContextExecutor = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()(system)
    val responseFuture = Http(system).singleRequest(HttpRequest(uri = "http://10.0.2.59:3002/cardid/5785539841043666"))
    responseFuture.onComplete {
      case Success(resp) => Unmarshal(resp.entity).to[String].map(s => println(s))
      case Failure(e) => println(e.getMessage)
    }
  }
}
