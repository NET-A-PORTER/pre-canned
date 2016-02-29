package com.netaporter.precanned

import akka.actor.Actor
import spray.http.{ StatusCodes, HttpRequest, HttpResponse }
import com.netaporter.precanned.HttpServerMock._
import StatusCodes._
import spray.can.Http.{ Connected, Register }

import scala.concurrent.duration.Duration

object HttpServerMock {
  case class PrecannedResponse(expects: Expect, response: HttpResponse)
  case object ClearExpectations
  case class Delay(delay: Duration)
  case object PrecannedResponseAdded
  case object DelayAdded
}

class HttpServerMock extends Actor {

  var responses = Vector.empty[PrecannedResponse]
  var delay: Option[Duration] = None

  def receive = {
    case p: PrecannedResponse =>
      responses :+= p
      sender ! PrecannedResponseAdded

    case d: Delay =>
      delay = Some(d.delay)
      sender ! DelayAdded

    case ClearExpectations =>
      responses = Vector.empty[PrecannedResponse]

    case Connected(_, _) =>
      sender ! Register(self)

    case h: HttpRequest =>
      delay.foreach { d => Thread.sleep(d.toMillis) }
      responses.find(_.expects(h)) match {
        case Some(pcr) =>
          sender ! pcr.response
        case None =>
          sender ! HttpResponse(status = NotFound)
      }
  }
}
