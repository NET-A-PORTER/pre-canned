package com.netaporter.precanned

import akka.actor.{ ActorRef, Actor }
import spray.http.{ StatusCodes, HttpRequest, HttpResponse }
import com.netaporter.precanned.HttpServerMock._
import StatusCodes._
import spray.can.Http.{ Connected, Register }

import scala.concurrent.duration._

object HttpServerMock {
  case class PrecannedResponse(expects: Expect, response: HttpResponse)
  case object ClearExpectations
  case class Delay(delay: FiniteDuration)
  case object PrecannedResponseAdded
  case object DelayAdded
  case class DelayedHttpRequest(h: HttpRequest, replyTo: ActorRef)
}

class HttpServerMock extends Actor {

  var responses = Vector.empty[PrecannedResponse]
  var delay: Option[FiniteDuration] = None

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

    case dh: DelayedHttpRequest =>
      responses.find(_.expects(dh.h)) match {
        case Some(pcr) =>
          dh.replyTo ! pcr.response
        case None =>
          dh.replyTo ! HttpResponse(status = NotFound)
      }

    case h: HttpRequest =>
      val replyTo = sender()
      delay match {
        case Some(delay) =>
          import scala.concurrent.ExecutionContext.Implicits.global
          context.system.scheduler.scheduleOnce(delay) {
            self ! DelayedHttpRequest(h, replyTo)
          }
        case None =>
          self ! DelayedHttpRequest(h, replyTo)
      }
  }
}
