package com.netaporter.precanned.dsl

import akka.actor.{ ActorSystem, ActorRef, Props, ActorRefFactory }
import com.netaporter.precanned._
import akka.io.IO
import spray.can.Http
import com.netaporter.precanned.HttpServerMock.ClearExpecations
import spray.http._
import com.netaporter.precanned.HttpServerMock.PrecannedResponse

object fancy extends Expectations with CannedResponses {

  def httpServerMock(implicit af: ActorRefFactory) = {
    val actor = af.actorOf(Props[HttpServerMock])
    Mock(actor)
  }

  case class Mock(mock: ActorRef) {
    def expect(e: Expect) = MockExpects(e)

    def bind(port: Int)(implicit as: ActorSystem) = {
      IO(Http) ! Http.Bind(mock, "127.0.0.1", port = port)
      this
    }

    def clearExpecations = {
      mock ! ClearExpecations
      this
    }

    case class MockExpects(expect: Expect) {
      def and(also: Expect) = MockExpects(x => expect(x) && also(x))
      def and(also: RespondWord) = MockExpectsAndResponds(expect, identity)
    }

    case class MockExpectsAndResponds(expect: Expect, response: Precanned) extends CannedResponses {
      def using(also: Precanned) = and(also)
      def and(also: Precanned) = copy(response = response andThen also)
      def end() = mock ! PrecannedResponse(expect, response(HttpResponse()))
    }
  }

  class RespondWord
  val respond = new RespondWord
}
