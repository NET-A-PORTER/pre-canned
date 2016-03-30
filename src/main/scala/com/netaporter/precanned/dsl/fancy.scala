package com.netaporter.precanned.dsl

import akka.actor.{ ActorSystem, ActorRef, Props, ActorRefFactory }
import com.netaporter.precanned._
import akka.io.IO
import spray.can.Http
import com.netaporter.precanned.HttpServerMock._
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

    def clearExpectations(blockUpTo: FiniteDuration = 3.seconds) = {
      val clearing = mock.ask(ClearExpectations)(Timeout(blockUpTo))
      if (blockUpTo > Duration.Zero) {
        Await.result(clearing, blockUpTo)
      }
      this
    }

    case class MockExpects(expect: Expect) {
      def and(also: Expect) = MockExpects(x => expect(x) && also(x))

      def and(also: RespondWord) = MockExpectsAndResponds(expect, identity)
    }

    case class MockExpectsAndResponds(expect: Expect, response: Precanned) extends CannedResponses {
      def using(also: Precanned) = and(also)

      def and(also: Precanned) = copy(response = response andThen also)

      def end(blockUpTo: FiniteDuration = 3.seconds): Option[PrecannedResponseAdded.type] = {
        val expectInProgress = mock.ask(ExpectAndRespondWith(expect, response(PrecannedResponse.empty)))(blockUpTo)
        if (blockUpTo > Duration.Zero) {
          Some(Await.result(expectInProgress.mapTo[PrecannedResponseAdded.type], blockUpTo))
        } else {
          None
        }
      }
    }
  }

  case class Start(mock: ActorRef) extends MockDsl {

    def bind(port: Int, interface: String = "127.0.0.1")(implicit as: ActorSystem, t: Timeout = 5.seconds): BindInProgress = {
      val bindFuture = IO(Http) ? Http.Bind(mock, interface, port = port)
      BindInProgress(mock, bindFuture.mapTo[Http.Bound], t)
    }

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
