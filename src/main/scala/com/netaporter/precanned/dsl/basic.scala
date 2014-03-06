package com.netaporter.precanned.dsl

import akka.actor.{ ActorSystem, Props, ActorRefFactory, ActorRef }
import com.netaporter.precanned.HttpServerMock.ClearExpecations
import Function.chain
import akka.io.IO
import spray.can.Http
import com.netaporter.precanned._
import spray.http.HttpResponse
import com.netaporter.precanned.HttpServerMock.PrecannedResponse
import scala.concurrent.{ Await, Future }
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask

object basic extends Expectations with CannedResponses {

  def httpServerMock(af: ActorRefFactory) = {
    val actor = af.actorOf(Props[HttpServerMock])
    Start(actor)
  }

  trait MockDsl {
    def mock: ActorRef

    def expect(es: Expect*) = MockExpects(mock, es)

    def clearExpecations = {
      mock ! ClearExpecations
      this
    }
  }

  case class Start(mock: ActorRef) extends MockDsl {

    def bind(port: Int)(implicit as: ActorSystem, t: Timeout = 5.seconds) = {
      val bindFuture = IO(Http) ? Http.Bind(mock, "127.0.0.1", port = port)
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
    def andRespondWith(pcs: Precanned*): Unit =
      mock ! PrecannedResponse(r => expects.forall(_.apply(r)), chain(pcs)(HttpResponse()))
  }
}