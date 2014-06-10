package com.netaporter.precanned

import akka.actor.Actor
import spray.http.{ StatusCodes, HttpRequest, HttpResponse }
import com.netaporter.precanned.HttpServerMock.{ ClearExpectations, PrecannedResponse }
import StatusCodes._
import spray.can.Http.{ Connected, Register }

object HttpServerMock {
  case class PrecannedResponse(expects: Expect, response: HttpResponse)
  case object ClearExpectations
}

class HttpServerMock extends Actor {

  var responses = Vector.empty[PrecannedResponse]

  def receive = {
    case p: PrecannedResponse =>
      responses :+= p

    case ClearExpectations =>
      responses = Vector.empty[PrecannedResponse]

    case Connected(_, _) =>
      sender ! Register(self)

    case h: HttpRequest =>
      responses.find(_.expects(h)) match {
        case Some(pcr) =>
          sender ! pcr.response
        case None =>
          sender ! HttpResponse(status = NotFound)
      }
  }
}
