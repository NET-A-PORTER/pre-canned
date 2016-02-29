package com.netaporter.precanned.dsl

import akka.actor.{ ActorRef, ActorRefFactory, ActorSystem, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.netaporter.precanned.HttpServerMock.{ ClearExpectations, Delay, PrecannedResponse }
import com.netaporter.precanned._
import spray.can.Http
import spray.http.HttpResponse

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

    def clearExpectations = {
      mock ! ClearExpectations
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
    def andRespondWith(pcs: Precanned*): MockExpects = {
      mock ! PrecannedResponse(r => expects.forall(_.apply(r)), chain(pcs)(HttpResponse()))
      this
    }

    def andRespondAfter(delay: FiniteDuration): MockExpects = {
      mock ! Delay(delay)
      this
    }
  }
}