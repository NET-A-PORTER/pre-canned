package com.netaporter.precanned

import akka.actor.{ Actor, ActorRef, ActorSystem }
import akka.pattern.ask
import akka.http.scaladsl.model._
import com.netaporter.precanned.HttpServerMock._
import StatusCodes._
import akka.http.scaladsl.Http
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.Flow
import akka.util.Timeout

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

object HttpServerMock {
  case class PrecannedResponse(response: HttpResponse, delay: FiniteDuration)
  object PrecannedResponse {
    val empty = PrecannedResponse(HttpResponse(), Duration.Zero)
  }

  case class ExpectAndRespondWith(expects: Expect, respondWith: PrecannedResponse)
  case object PrecannedResponseAdded

  case object ClearExpectations
  case object ExpectationsCleared

  def handleRequest(
    mockRef: ActorRef,
    requestToStrictDuration: FiniteDuration = 5.seconds,
    //you can't pre-can a delayed response with a delay larger than this timeout
    mockAskTimeout: Timeout = 1.minute)(req: HttpRequest)(
      implicit ec: ExecutionContext, materializer: Materializer): Future[HttpResponse] = req
    .toStrict(requestToStrictDuration)
    .flatMap(mockRef.ask(_)(timeout = mockAskTimeout)).mapTo[HttpResponse]

  def startServer(
    mockRef: ActorRef, port: Int, interface: String)(implicit system: ActorSystem): Future[Http.ServerBinding] = {
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val materializer: Materializer = ActorMaterializer()
    val handler = Flow[HttpRequest].mapAsyncUnordered(Int.MaxValue)(handleRequest(mockRef))
    Http().bindAndHandle(port = port, interface = interface, handler = handler)
  }
}

class HttpServerMock extends Actor {

  import context.dispatcher

  var responses = Vector.empty[ExpectAndRespondWith]

  def responseFor(request: HttpRequest) =
    responses.find(_.expects(request)).map(_.respondWith)

  def receive = {
    case expectAndRespond: ExpectAndRespondWith =>
      responses :+= expectAndRespond
      sender ! PrecannedResponseAdded

    case ClearExpectations =>
      responses = Vector.empty
      sender ! ExpectationsCleared

    case req: HttpRequest =>
      responseFor(req) match {
        case Some(PrecannedResponse(response, delay)) =>
          if (delay > Duration.Zero) {
            context.system.scheduler.scheduleOnce(delay, sender, response)
          } else {
            sender ! response
          }
        case None =>
          sender ! HttpResponse(status = NotFound)
      }
  }
}
