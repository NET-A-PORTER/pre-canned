package com.netaporter.precanned.dsl

import spray.http._
import spray.http.Uri.Query
import scala.io.Source
import akka.actor.{ ActorSystem, Props, ActorRefFactory, ActorRef }
import com.netaporter.precanned.HttpServerMock.ClearExpecations
import Function.chain
import akka.io.IO
import spray.can.Http
import com.netaporter.precanned._
import com.netaporter.precanned.HttpServerMock.PrecannedResponse
import spray.http.HttpRequest
import spray.http.HttpResponse
import com.netaporter.precanned.HttpServerMock.PrecannedResponse
import spray.http.HttpHeaders.`Content-Type`

object basic {

  // Service Mock
  def httpServerMock(implicit af: ActorRefFactory) = {
    val actor = af.actorOf(Props[HttpServerMock])
    Mock(actor)
  }

  case class Mock(mock: ActorRef) {
    def expect(es: Expect*) = MockExpects(mock, es)

    def bind(port: Int)(implicit as: ActorSystem) = {
      IO(Http) ! Http.Bind(mock, "127.0.0.1", port = port)
      Thread.sleep(1000l)
      this
    }

    def clearExpecations = {
      mock ! ClearExpecations
      this
    }
  }

  case class MockExpects(mock: ActorRef, expects: Seq[Expect]) {
    def precan(pcs: Precan*): Unit =
      mock ! PrecannedResponse(mock, r => expects.exists(_.apply(r)), chain(pcs)(HttpResponse()))
  }

  // Expectations

  def method(m: HttpMethod): Expect = r =>
    r.method == m

  def path(s: String): Expect = r =>
    r.uri.path.toString == s

  def pathStartsWith(s: String): Expect = r =>
    r.uri.path.toString.startsWith(s)

  def query(kvs: (String, String)*): Expect = r =>
    r.uri.query.filter(kvs.contains) == Query(kvs: _*)

  def header(hs: HttpHeader*): Expect = r =>
    r.headers.filter(hs.contains) == hs.toList

  // Responses

  def status(s: StatusCode): Precan = r =>
    r.copy(status = s)

  def header(h: HttpHeader): Precan = r =>
    r.copy(headers = h :: r.headers)

  def contentType(c: ContentType) =
    header(`Content-Type`(c))

  def entity(e: HttpEntity): Precan = r =>
    r.withEntity(e)

  def resource(filename: String): Precan = r => {
    val resource = getClass.getResourceAsStream(filename)
    val source = Source.fromInputStream(resource)
    val content = source.mkString
    source.close
    entity(content)(r)
  }
}