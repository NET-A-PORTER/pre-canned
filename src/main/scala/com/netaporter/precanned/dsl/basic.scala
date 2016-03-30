package com.netaporter.precanned.dsl

import akka.actor.{ ActorRef, ActorRefFactory, ActorSystem, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.netaporter.precanned.HttpServerMock._
import com.netaporter.precanned._
import spray.can.Http

import scala.Function.chain
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object basic extends Expectations with CannedResponses {

  def httpServerMock(af: ActorRefFactory) = {
    val actor = af.actorOf(Props[HttpServerMock])
    Start(actor)
  }

  trait MockDsl {
    def mock: ActorRef

    def expect(es: Expect*) = MockExpects(mock, es)

    def clearExpectations(blockUpTo: FiniteDuration = 3.seconds) = {
      val clearing = mock.ask(ClearExpectations)(Timeout(blockUpTo))
      if (blockUpTo > Duration.Zero) {
        Await.result(clearing, blockUpTo)
      }
      this
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

  case class MockExpects(mock: ActorRef, expects: Seq[Expect]) {
    def andRespondWith(pcs: Precanned*): ExpectationAddInProgress = {
      val expectAndRespond = ExpectAndRespondWith(r => expects.forall(_.apply(r)), chain(pcs)(PrecannedResponse.empty))
      // 60 seconds is a hack for users who want to use `ExpectationAddInProgress.blockFor`. Would be nice to use their
      // timeout specified in the method, but we do not have that yet here
      // Adding an expectation is a fast operation, so it is reasonably safe to assume we will never need to wait longer
      // 60 seconds for this.
      val expectInProgress = mock.ask(expectAndRespond)(Timeout(60.seconds)).mapTo[PrecannedResponseAdded.type]
      ExpectationAddInProgress(expectInProgress)
    }
  }

  case class ExpectationAddInProgress(expectInProgress: Future[PrecannedResponseAdded.type]) {
    def blockFor(blockUpTo: FiniteDuration) = {
      Await.result(expectInProgress, blockUpTo)
    }
  }
}