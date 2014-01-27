package com.netaporter.precanned

import akka.actor.ActorSystem
import org.scalatest.{ BeforeAndAfterAll, Matchers, BeforeAndAfter, FlatSpecLike }
import dsl.basic._
import spray.client.pipelining._
import spray.http.{ HttpRequest, HttpResponse }
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import spray.http.StatusCodes._

class BasicDslSpec
    extends FlatSpecLike
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll
    with BaseSpec {

  val animalApi = httpServerMock(system).bind(8765)

  after { animalApi.clearExpecations }
  override def afterAll() { system.shutdown() }

  "path expectation" should "match path" in {
    animalApi.expect(path("/animals"))
      .andRespondWith(resource("/responses/animals.json"))

    val resF = pipeline(Get("http://127.0.0.1:8765/animals"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "several expectation" should "work together" in {
    animalApi.expect(get, path("/animals"), query("name" -> "giraffe"))
      .andRespondWith(resource("/responses/giraffe.json"))

    val resF = pipeline(Get("http://127.0.0.1:8765/animals?name=giraffe"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""{"name": "giraffe"}""")
  }

  "earlier expectations" should "take precedence" in {
    animalApi.expect(get, path("/animals"))
      .andRespondWith(resource("/responses/animals.json"))

    animalApi.expect(get, path("/animals"), query("name" -> "giraffe"))
      .andRespondWith(resource("/responses/giraffe.json"))

    val resF = pipeline(Get("http://127.0.0.1:8765/animals?name=giraffe"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "unmatched requests" should "return 404" in {
    animalApi.expect(get, path("/animals")).andRespondWith(resource("/responses/animals.json"))

    val resF = pipeline(Get("http://127.0.0.1:8765/hotdogs"))
    val res = Await.result(resF, dur)

    res.status should equal(NotFound)
  }
}
