package com.netaporter.precanned

import spray.http.HttpMethods._
import spray.http.{ HttpHeader, HttpMethod }
import spray.http.Uri.Query

trait Expectations {
  val get = method(GET)
  val post = method(POST)
  val head = method(HEAD)
  val trace = method(TRACE)
  val patch = method(PATCH)
  val delete = method(DELETE)
  val options = method(OPTIONS)
  val connect = method(CONNECT)

  def method(m: HttpMethod): Expect = r =>
    r.method == m

  def path(s: String): Expect = r =>
    r.uri.path.toString == s

  def pathStartsWith(s: String): Expect = r =>
    r.uri.path.toString.startsWith(s)

  def pathEndsWith(s: String): Expect = r =>
    r.uri.path.toString.endsWith(s)

  def uri(s: String): Expect = r =>
    r.uri.toString == s

  def uriStartsWith(s: String): Expect = r =>
    r.uri.toString.startsWith(s)

  def uriEndsWith(s: String): Expect = r =>
    r.uri.toString.endsWith(s)

  def query(kvs: (String, String)*): Expect = r =>
    r.uri.query.filter(kvs.contains) == Query(kvs: _*)

  def header(hs: HttpHeader*): Expect = r =>
    r.headers.filter(hs.contains) == hs.toList
}
