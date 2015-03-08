Pre-canned
==========

[![Build Status](https://travis-ci.org/NET-A-PORTER/pre-canned.svg?branch=0.0.5)](https://travis-ci.org/NET-A-PORTER/pre-canned)

Mocking HTTP services on [spray can](http://spray.io) for integration testing

Introduction
------------

Pre-canned helps you to mock out the HTTP services your application depends on. This can be especially useful
for your integration testing.

For SBT add the dependency `"com.netaporter" %% "pre-canned" % "0.0.5" % "test"`

DSLs
----

Pre-canned currently comes in two flavours:

 * *[`basic`](#basic)* - Simple, but many parentheses
 * *[`fancy`](#fancy)* - Few parentheses, but quite wordy

Help make Pre-canned better and submit a new improved flavour via a PR, or ideas for one in an issue.

There are a basic set of [expectations](https://github.com/NET-A-PORTER/pre-canned/blob/master/src/main/scala/com/netaporter/precanned/Expectations.scala) and [canned responses](https://github.com/NET-A-PORTER/pre-canned/blob/master/src/main/scala/com/netaporter/precanned/CannedResponses.scala). Feel free to contribute more via a PR.

### basic

```scala
import com.netaporter.precanned.dsl.basic._

val animalApi = httpServerMock(system).bind(8765).block

animalApi.expect(get, path("/animals"), query("name" -> "giraffe"))
         .andRespondWith(resource("/responses/giraffe.json"))
```

### fancy

```scala
import com.netaporter.precanned.dsl.fancy._

val animalApi = httpServerMock(system).bind(8766).block

animalApi expect
  get and path("/animals") and query("name" -> "giraffe") and
respond using
  resource("/responses/giraffe.json") end()
```
