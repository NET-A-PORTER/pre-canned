package com.netaporter.precanned.dsl

import akka.actor.{ ActorSystem, Props, ActorRefFactory, ActorRef }
import com.netaporter.precanned.HttpServerMock.ClearExpecations
import Function.chain
import akka.io.IO
import spray.can.Http
import com.netaporter.precanned._
import spray.http.HttpResponse
import com.netaporter.precanned.HttpServerMock.PrecannedResponse

object basic extends Expectations with CannedResponses {

  def httpServerMock(af: ActorRefFactory) = {
    val actor = af.actorOf(Props[HttpServerMock])
    Mock(actor)
  }

  case class Mock(mock: ActorRef) {
    def expect(es: Expect*) = MockExpects(mock, es)

    def bind(port: Int)(implicit as: ActorSystem) = {
      IO(Http) ! Http.Bind(mock, "127.0.0.1", port = port)
      this
    }

    def clearExpecations = {
      mock ! ClearExpecations
      this
    }
  }

  case class MockExpects(mock: ActorRef, expects: Seq[Expect]) {
    def andRespondWith(pcs: Precanned*): Unit =
      mock ! PrecannedResponse(r => expects.forall(_.apply(r)), chain(pcs)(HttpResponse()))
  }
}