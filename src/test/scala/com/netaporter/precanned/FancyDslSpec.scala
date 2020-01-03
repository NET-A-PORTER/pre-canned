package com.netaporter.precanned

import com.netaporter.precanned.HttpServerMock.PrecannedResponseAdded
import com.netaporter.precanned.dsl.fancy._
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.model.headers._

import scala.concurrent.Await
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ContentTypes._

import scala.concurrent.duration._

class FancyDslSpec
  extends AnyFlatSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with BaseSpec {

  // format: OFF

  val port = 8766
  val animalApi: BoundComplete = httpServerMock(system).bind(port).block

  after { animalApi.clearExpectations() }
  override def afterAll() {
    Await.result(system.terminate(), Duration.Inf)
  }

  "query expectation" should "match in any order" in {
    animalApi expect query ("key1" -> "val1", "key2" -> "val2") and respond using resource("/responses/animals.json") end()

    val resF = pipeline(Get(s"http://127.0.0.1:$port?key2=val2&key1=val1"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "path expectation" should "match path" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "contentType CannedResponse" should "set Content-Type header" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") and contentType(`application/json`) end()

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals"))
    val res = Await.result(resF, dur)

    res.header[`Content-Type`].get.value should equal("application/json")
  }

  "several expectation" should "work together" in {
    animalApi expect
      get and path("/animals") and query("name" -> "giraffe") and
    respond using
      resource("/responses/giraffe.json") end()

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals?name=giraffe"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""{"name": "giraffe"}""")
  }

  "earlier expectations" should "take precedence" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    animalApi expect
      get and query("name" -> "giraffe") and
    respond using
      resource("/responses/giraffe.json") end(blockUpTo = 3.seconds)

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals?name=giraffe"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "unmatched requests" should "return 404" in {
    animalApi expect path("/animals") and respond using resource("/responses/animals.json") end()

    val resF = pipeline(Get(s"http://127.0.0.1:8766/hotdogs"))
    val res = Await.result(resF, dur)

    res.status should equal(NotFound)
  }

  "a delay" should "cause the response to be delayed" in {
    animalApi expect
      get and path("/animals") and
    respond using
      status(200) and delay(5.seconds) end(blockUpTo = 3.seconds)

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals"))

    Thread.sleep(4000l)
    resF.isCompleted should equal(false)

    val res = Await.result(resF, dur)
    res.status.intValue should equal(200)
  }

  "blockFor" should "block by default until the expectation is added and return confirmation" in {
    val blocked =
      animalApi expect
        get and path("/animals") and
      respond using
        status(200) and delay(5.seconds) end()

    blocked should equal(Some(PrecannedResponseAdded))
  }

  "No blockFor" should "not block and return immediately with a None" in {
    val blocked =
      animalApi expect
        get and path("/animals") and
        respond using
        status(200) and delay(5.seconds) end(blockUpTo = Duration.Zero)

    blocked should equal(None)
  }

  "server mock" should "be bound to some available port" in {
    val availablePortApi = httpServerMock(system).bind().block
    val availablePort = availablePortApi.binding.localAddress.getPort
    availablePortApi expect get and path("/status") and respond using entity("OK") end()

    val resF = pipeline(Get(s"http://127.0.0.1:$availablePort/status"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("OK")
  }
}
