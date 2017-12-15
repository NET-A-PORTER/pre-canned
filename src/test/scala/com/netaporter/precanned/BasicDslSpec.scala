package com.netaporter.precanned

import akka.http.scaladsl.model.{ HttpEntity, StatusCodes }
import StatusCodes._
import com.netaporter.precanned.HttpServerMock.PrecannedResponseAdded
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, Matchers, OptionValues }
import dsl.basic._

import scala.concurrent.Await
import scala.concurrent.duration._

class BasicDslSpec
  extends FlatSpecLike
  with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll
  with OptionValues
  with BaseSpec {

  val port = 8765
  val animalApi: BoundComplete = httpServerMock(system).bind(8765).block

  after { animalApi.clearExpectations() }
  override def afterAll() {
    Await.result(system.terminate(), Duration.Inf)
  }

  "query expectation" should "match in any order" in {
    animalApi.expect(query("key1" -> "val1", "key2" -> "val2"))
      .andRespondWith(resource("/responses/animals.json")).blockFor(3.seconds)

    val resF = pipeline(Get(s"http://127.0.0.1:$port?key2=val2&key1=val1"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "path expectation" should "match path" in {
    animalApi.expect(path("/animals"))
      .andRespondWith(resource("/responses/animals.json"))

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "several expectation" should "work together" in {
    animalApi.expect(get, path("/animals"), query("name" -> "giraffe"))
      .andRespondWith(resource("/responses/giraffe.json")).blockFor(3.seconds)

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals?name=giraffe"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""{"name": "giraffe"}""")
  }

  "earlier expectations" should "take precedence" in {
    animalApi.expect(get, path("/animals"))
      .andRespondWith(resource("/responses/animals.json")).blockFor(3.seconds)

    animalApi.expect(get, path("/animals"), query("name" -> "giraffe"))
      .andRespondWith(resource("/responses/giraffe.json"))

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals?name=giraffe"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""[{"name": "rhino"}, {"name": "giraffe"}, {"name": "tiger"}]""")
  }

  "unmatched requests" should "return 404" in {
    animalApi.expect(get, path("/animals")).andRespondWith(resource("/responses/animals.json")).blockFor(3.seconds)

    val resF = pipeline(Get(s"http://127.0.0.1:$port/hotdogs"))
    val res = Await.result(resF, dur)

    res.status should equal(StatusCodes.NotFound)
  }

  "custom status code with entity" should "return as expected" in {

    animalApi.expect(get, path("/animals")).andRespondWith(status(404), entity(HttpEntity("""{"error": "animals not found"}"""))).blockFor(3.seconds)

    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals"))
    val res = Await.result(resF, dur)

    res.status should equal(NotFound)
    res.entity.asString should equal("""{"error": "animals not found"}""")
  }

  "post request non empty content " should "match exactly" in {
    val postContent: String = """ {"name":"gorilla gustav"} """
    animalApi.expect(post, path("/animals"), exactContent(postContent)).andRespondWith(entity(HttpEntity("""{"record":"created" """))).blockFor(3.seconds)

    val resF = pipeline(Post(s"http://127.0.0.1:$port/animals", postContent))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""{"record":"created" """)
  }

  "post request empty content " should "match" in {
    animalApi.expect(post, path("/animals"), exactContent()).andRespondWith(entity(HttpEntity("""{"error":"name not provided" """))).blockFor(3.seconds)

    val resF = pipeline(Post(s"http://127.0.0.1:$port/animals"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""{"error":"name not provided" """)
  }

  "post request non empty content " should "match partially" in {
    val postContent: String = """ {"name":"gorilla gustav"} """
    animalApi.expect(post, path("/animals"), containsContent("gorilla gustav")).andRespondWith(entity(HttpEntity("""{"record":"created" """))).blockFor(3.seconds)

    val resF = pipeline(Post(s"http://127.0.0.1:$port/animals", postContent))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("""{"record":"created" """)
  }

  "a delay" should "cause the response to be delayed" in {
    animalApi.expect(get, path("/animals")).andRespondWith(status(200), delay(5.seconds)).blockFor(3.seconds)
    val resF = pipeline(Get(s"http://127.0.0.1:$port/animals"))

    Thread.sleep(4000l)
    resF.isCompleted should equal(false)

    val res = Await.result(resF, dur)
    res.status.intValue should equal(200)
  }

  "blockFor" should "block until the expectation is added and return confirmation" in {
    val blocked = animalApi.expect(get, path("/animals")).andRespondWith(status(200), delay(5.seconds)).blockFor(3.seconds)
    blocked should equal(PrecannedResponseAdded)
  }

  "server mock" should "be bound to some available port" in {
    val availablePortApi = httpServerMock(system).bind().block
    val availablePort = availablePortApi.binding.localAddress.getPort
    availablePortApi.expect(get, path("/status")).andRespondWith(entity("OK")).blockFor(3.seconds)

    val resF = pipeline(Get(s"http://127.0.0.1:$availablePort/status"))
    val res = Await.result(resF, dur)

    res.entity.asString should equal("OK")
  }

  "numberOfTimes" should "cause the response to be served only limited number of times" in {
    animalApi.expect(get, path("/animals")).numberOfTimes(2).andRespondWith(status(200)).blockFor(3.seconds)

    // first
    val resF1 = pipeline(Get(s"http://127.0.0.1:$port/animals"))
    val res1 = Await.result(resF1, dur)
    res1.status.intValue should equal(200)

    // second
    val resF2 = pipeline(Get(s"http://127.0.0.1:$port/animals"))
    val res2 = Await.result(resF2, dur)
    res2.status.intValue should equal(200)

    // unexpected
    val resF3 = pipeline(Get(s"http://127.0.0.1:$port/animals"))
    val res3 = Await.result(resF3, dur)
    res3.status.intValue should equal(404)
  }
}
