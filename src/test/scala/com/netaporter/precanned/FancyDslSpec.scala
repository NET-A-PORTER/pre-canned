package com.netaporter.precanned

import akka.actor.ActorSystem
import org.scalatest.{ BeforeAndAfterAll, Matchers, BeforeAndAfter, FlatSpecLike }
import com.netaporter.precanned.dsl.fancy._
import spray.client.pipelining._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import spray.http.StatusCodes._
import spray.http.HttpRequest
import spray.http.HttpResponse

class FancyDslSpec
    extends FlatSpecLike
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll {

  // format: OFF

  implicit val system = ActorSystem()
  implicit def ec = system.dispatcher

  val animalApi = httpServerMock(system).bind(8766)

  after { animalApi.clearExpecations }
  override def afterAll() { system.shutdown() }

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  "path expectation" should "match path" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8766/animals"))
    val res = Await.result(resF, 10.second)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "several expectation" should "work together" in {
    animalApi expect
      get and path("/animals") and query("name" -> "giraffe") and
    respond using
      resource("/responses/giraffe.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8766/animals?name=giraffe"))
    val res = Await.result(resF, 10.second)

    res.entity.asString should equal("""{"name": "giraffe"}""")
  }

  "earlier expectations" should "take precedence" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    animalApi expect
      get and query("name" -> "giraffe") and
    respond using
      resource("/responses/giraffe.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8766/animals?name=giraffe"))
    val res = Await.result(resF, 1.second)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "unmatched requests" should "return 404" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8766/hotdogs"))
    val res = Await.result(resF, 1.second)

    res.status should equal(NotFound)
  }
}
