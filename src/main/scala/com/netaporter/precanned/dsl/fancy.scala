package com.netaporter.precanned.dsl

import akka.actor.{ ActorRef, ActorRefFactory, ActorSystem, Props }
import akka.http.scaladsl.Http
import com.netaporter.precanned._
import com.netaporter.precanned.HttpServerMock._
import akka.util.Timeout

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import akka.pattern.ask

object fancy extends Expectations with CannedResponses {

  def httpServerMock(implicit af: ActorRefFactory) = {
    val actor = af.actorOf(Props[HttpServerMock]())
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

      def end: Option[PrecannedResponseAdded.type] = end(3.seconds)

      def end(blockUpTo: FiniteDuration): Option[PrecannedResponseAdded.type] = {
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

    def bind(
      port: Int = 0, interface: String = "127.0.0.1")(implicit as: ActorSystem, t: Timeout = 5.seconds): BindInProgress = {
      val bindFuture = HttpServerMock.startServer(mock, port, interface)
      BindInProgress(mock, bindFuture, t)
    }

  }

  case class BindInProgress(mock: ActorRef, bind: Future[Http.ServerBinding], t: Timeout) extends MockDsl {
    def block = {
      val bound = Await.result(bind, t.duration)
      BoundComplete(mock, bound)
    }
  }

  case class BoundComplete(mock: ActorRef, binding: Http.ServerBinding) extends MockDsl

  class RespondWord
  val respond = new RespondWord
}
