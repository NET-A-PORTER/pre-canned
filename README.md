Pre-canned
==========

Mocking HTTP services on [spray can](http://spray.io) for integration testing

Introduction
------------

Pre-canned helps you to mock out the HTTP services your application depends on. This can be especially useful
for your integration testing.

For SBT add the dependency `"com.netaporter" %% "scala-uri" % "0.0.1"`

DSLs
----

Pre-canned currently comes in two flavours:

 * *[`basic`](#basic)* - Simple, but many parentheses
 * *[`fancy`](#fancy)* - Few parentheses, but quite wordy

Help make the perfect DSL and submit a new flavour in a PR, or ideas for one in an issue.

### basic

```scala
import com.netaporter.precanned.dsl.basic._

val animalApi = httpServerMock(system).bind(8765)

animalApi.expect(get, path("/animals"), query("name" -> "giraffe"))
         .andRespondWith(resource("/responses/giraffe.json"))
```

### fancy

```scala
import com.netaporter.precanned.dsl.fancy._

val animalApi = httpServerMock(system).bind(8766)

animalApi expect
  get and path("/animals") and query("name" -> "giraffe") and
respond using
  resource("/responses/giraffe.json") end()
```