package com.netaporter.precanned

import akka.actor.{ ActorRef, Actor }
import spray.http.{ StatusCodes, HttpRequest, HttpResponse }
import com.netaporter.precanned.HttpServerMock._
import StatusCodes._
import spray.can.Http.{ Connected, Register }

import scala.concurrent.duration._

object HttpServerMock {
  case class PrecannedResponse(response: HttpResponse, delay: FiniteDuration)
  object PrecannedResponse {
    val empty = PrecannedResponse(HttpResponse(), Duration.Zero)
  }

  case class ExpectAndRespondWith(expects: Expect, respondWith: PrecannedResponse)
  case object PrecannedResponseAdded

  case object ClearExpectations
  case object ExpectationsCleared
}

class HttpServerMock extends Actor {

  import context.dispatcher

  var responses = Vector.empty[ExpectAndRespondWith]

  def responseFor(request: HttpRequest) =
    responses.find(_.expects(request)).map(_.respondWith)

  def receive = {
    case expectAndRespond: ExpectAndRespondWith =>
      responses :+= expectAndRespond
      sender ! PrecannedResponseAdded

    case ClearExpectations =>
      responses = Vector.empty
      sender ! ExpectationsCleared

    case Connected(_, _) =>
      sender ! Register(self)

    case req: HttpRequest =>
      responseFor(req) match {
        case Some(PrecannedResponse(response, delay)) =>
          if (delay > Duration.Zero) {
            context.system.scheduler.scheduleOnce(delay, sender, response)
          } else {
            sender ! response
          }
        case None =>
          sender ! HttpResponse(status = NotFound)
      }
  }
}
