package com.netaporter.precanned

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.{ BeforeAndAfterAll, Matchers, BeforeAndAfter, FlatSpecLike }
import dsl.basic._
import spray.client.pipelining._
import spray.http.{ HttpMethods, HttpRequest, HttpResponse }
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import HttpMethods._

class BasicDslSpec
    extends FlatSpecLike
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll {

  implicit val system = ActorSystem()
  implicit def ec = system.dispatcher

  val animalApi = httpServerMock.bind(8765)

  after { animalApi.clearExpecations }
  override def afterAll() { system.shutdown() }

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  "path expectation" should "match path" in {
    animalApi.expect(path("/animals"))
      .precan(resource("/responses/animals.json"))

    val resF = pipeline(Get("http://127.0.0.1:8765/animals"))
    val res = Await.result(resF, 10.second)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "several expectation" should "work together" in {
    animalApi.expect(method(HttpMethods.GET), path("/animals"), query("name" -> "lion"))
      .precan(resource("/responses/animals.json"))

    val resF = pipeline(Get("http://127.0.0.1:8765/animals?name=lion"))
    val res = Await.result(resF, 10.second)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }
}
