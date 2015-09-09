package com.netaporter.precanned.dsl

import akka.actor.{ ActorSystem, ActorRef, Props, ActorRefFactory }
import com.netaporter.precanned._
import akka.io.IO
import spray.can.Http
import com.netaporter.precanned.HttpServerMock.ClearExpectations
import spray.http._
import com.netaporter.precanned.HttpServerMock.PrecannedResponse
import akka.util.Timeout
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import akka.pattern.ask

object fancy extends Expectations with CannedResponses {

  def httpServerMock(implicit af: ActorRefFactory) = {
    val actor = af.actorOf(Props[HttpServerMock])
    Start(actor)
  }

  trait MockDsl {
    def mock: ActorRef

    def expect(e: Expect) = MockExpects(e)

    def clearExpectations = {
      mock ! ClearExpectations
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

  case class Start(mock: ActorRef) extends MockDsl {
    def bind(interface: String, port: Int)(implicit as: ActorSystem, t: Timeout = 5.seconds) = {
      val bindFuture = IO(Http) ? Http.Bind(mock, interface, port = port)
      BindInProgress(mock, bindFuture.mapTo[Http.Bound], t)
    }

    def bind(port: Int)(implicit as: ActorSystem, t: Timeout = 5.seconds) =
      bind("127.0.0.1", port)
  }

  case class BindInProgress(mock: ActorRef, bind: Future[Http.Bound], t: Timeout) extends MockDsl {
    def block = {
      Await.result(bind, t.duration)
      BoundComplete(mock)
    }
  }

  case class BoundComplete(mock: ActorRef) extends MockDsl

  class RespondWord
  val respond = new RespondWord
}
