package com.example

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.azure.messaging.servicebus._

import java.util.concurrent.CountDownLatch
import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps
import scala.util.{Failure, Success}

object AkkaAppInsightsExample {
  var actor: Option[ActorRef] = None

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("SingleRequest")
    implicit val executionContextExecutor: ExecutionContextExecutor = system.dispatcher

    actor = Some(system.actorOf(Props(new AkkaActor), "AkkaActor"))
    val receiver = receiveMessages()
    receiver.start()
  }

  def performRequest(system: ActorSystem): Unit = {
    implicit val executionContextExecutor: ExecutionContextExecutor = system.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()(system)
    val responseFuture = Http(system).singleRequest(HttpRequest(uri = sys.env("SERVICE_URL")))
    responseFuture.onComplete {
      case Success(resp) => Unmarshal(resp.entity).to[String].map(s => sendMessage(s))
      case Failure(e) => println(e.getMessage)
    }
  }

  def sendMessage(message: String): Unit = {
    val queueName = sys.env("SERVICE_BUS_DEST")
    val senderClient = new ServiceBusClientBuilder().connectionString(sys.env("SERVICE_BUS_CONNECT_STRING")).sender.queueName(queueName).buildClient
    senderClient.sendMessage(new ServiceBusMessage(message))
    println("Sent a single message to the queue: " + queueName)
  }

  @throws[InterruptedException]
  def receiveMessages(): ServiceBusProcessorClient = {
    val countdownLatch = new CountDownLatch(1)
    val processorClient = new ServiceBusClientBuilder()
      .connectionString(sys.env("SERVICE_BUS_CONNECT_STRING"))
      .processor
      .queueName(sys.env("SERVICE_BUS_FROM"))
      .processMessage(processMessage)
      .processError((context: ServiceBusErrorContext) => processError(context, countdownLatch))
      .buildProcessorClient
    processorClient
  }

  private def processMessage(context: ServiceBusReceivedMessageContext): Unit = {
    val message = context.getMessage.getBody.toString
    println(s"Got message with body: $message")
    actor.foreach(_ ! "PerformRequest")
  }

  private def processError(context: ServiceBusErrorContext, latch: CountDownLatch): Unit = ???
}
