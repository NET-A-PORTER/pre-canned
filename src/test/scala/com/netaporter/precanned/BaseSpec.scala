package com.netaporter.precanned

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import scala.concurrent.Future

trait BaseSpec {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val dur = 5.seconds

  val pipeline: HttpRequest => Future[HttpResponse] = req =>
    Http().singleRequest(req).flatMap(_.toStrict(dur))

  def Get(urlStr: String): HttpRequest = HttpRequest(method = HttpMethods.GET, uri = Uri(urlStr))

  def Post(urlStr: String, body: String = ""): HttpRequest =
    HttpRequest(method = HttpMethods.POST, uri = Uri(urlStr), entity = HttpEntity(body))
}
