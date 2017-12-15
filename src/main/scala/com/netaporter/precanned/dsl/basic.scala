package com.netaporter.precanned.dsl

import akka.actor.{ ActorRef, ActorRefFactory, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.util.Timeout
import com.netaporter.precanned.HttpServerMock._
import com.netaporter.precanned._

import scala.Function.chain
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object basic extends Expectations with CannedResponses {

  def httpServerMock(af: ActorRefFactory): Start = {
    val actor = af.actorOf(Props[HttpServerMock])
    Start(actor)
  }

  trait MockDsl {
    def mock: ActorRef

    def expect(es: Expect*) = MockExpects(mock, es)

    def clearExpectations(blockUpTo: FiniteDuration = 3.seconds): MockDsl = {
      val clearing = mock.ask(ClearExpectations)(Timeout(blockUpTo))
      if (blockUpTo > Duration.Zero) {
        Await.result(clearing, blockUpTo)
      }
      this
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
    def block: BoundComplete = {
      val bound = Await.result(bind, t.duration)
      BoundComplete(mock, bound)
    }
  }

  case class BoundComplete(mock: ActorRef, binding: Http.ServerBinding) extends MockDsl

  case class MockExpects(mock: ActorRef, expects: Seq[Expect], numberOfTimes: Option[Int] = None) {

    numberOfTimes.foreach { num => require(num > 0, s"numberOfTimes Some($num) must be a positive value") }

    /** Sets the number of times to respond with this response. Must be positive! */
    def numberOfTimes(num: Int): MockExpects = this.copy(numberOfTimes = Some(num))

    /** Sets the number of times to respond with this response. `None` for unlimited. Must be positive! */
    def numberOfTimes(num: Option[Int]): MockExpects = this.copy(numberOfTimes = num)

    def andRespondWith(pcs: Precanned*): ExpectationAddInProgress = {
      val expectAndRespond = ExpectAndRespondWith(
        expects = r => expects.forall(_.apply(r)),
        respondWith = chain(pcs)(PrecannedResponse.empty),
        numberOfTimes = numberOfTimes)
      // 60 seconds is a hack for users who want to use `ExpectationAddInProgress.blockFor`. Would be nice to use their
      // timeout specified in the method, but we do not have that yet here
      // Adding an expectation is a fast operation, so it is reasonably safe to assume we will never need to wait longer
      // 60 seconds for this.
      val expectInProgress = mock.ask(expectAndRespond)(Timeout(60.seconds)).mapTo[PrecannedResponseAdded.type]
      ExpectationAddInProgress(expectInProgress)
    }
  }

  case class ExpectationAddInProgress(expectInProgress: Future[PrecannedResponseAdded.type]) {
    def blockFor(blockUpTo: FiniteDuration): PrecannedResponseAdded.type = {
      Await.result(expectInProgress, blockUpTo)
    }
  }
}