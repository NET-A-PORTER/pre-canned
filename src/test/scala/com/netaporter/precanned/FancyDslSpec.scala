package com.netaporter.precanned

import com.netaporter.precanned.dsl.fancy._
import org.scalatest.{ BeforeAndAfterAll, Matchers, BeforeAndAfter, FlatSpecLike }
import spray.client.pipelining._
import spray.http.HttpHeaders._
import scala.concurrent.Await
import spray.http.StatusCodes._
import spray.http.ContentTypes._

class FancyDslSpec
    extends FlatSpecLike
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll
    with BaseSpec {

  // format: OFF

  val animalApi = httpServerMock(system).bind(8766).block

  after { animalApi.clearExpectations }
  override def afterAll() { system.shutdown() }

  "query expectation" should "match in any order" in {
    animalApi expect query ("key1" -> "val1", "key2" -> "val2") and respond using resource("/responses/animals.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8765?key2=val2&key1=val1"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "path expectation" should "match path" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8766/animals"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "contentType CannedResponse" should "set Content-Type header" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") and contentType(`application/json`) end()

    val resF = pipeline(Get("http://127.0.0.1:8766/animals"))
    val res = Await.result(resF, dur)

    res.header[`Content-Type`].get.value should equal("application/json; charset=UTF-8")
  }

  "several expectation" should "work together" in {
    animalApi expect
      get and path("/animals") and query("name" -> "giraffe") and
    respond using
      resource("/responses/giraffe.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8766/animals?name=giraffe"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""{"name": "giraffe"}""")
  }

  "earlier expectations" should "take precedence" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    animalApi expect
      get and query("name" -> "giraffe") and
    respond using
      resource("/responses/giraffe.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8766/animals?name=giraffe"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "unmatched requests" should "return 404" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    val resF = pipeline(Get("http://127.0.0.1:8766/hotdogs"))
    val res = Await.result(resF, dur)

    res.status should equal(NotFound)
  }
}
