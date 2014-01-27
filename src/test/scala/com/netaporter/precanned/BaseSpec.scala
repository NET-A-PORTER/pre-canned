package com.netaporter.precanned

import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.concurrent.Future
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.http.HttpResponse

trait BaseSpec {
  implicit val system = ActorSystem()
  implicit def ec = system.dispatcher

  val dur = 5.seconds

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
}
