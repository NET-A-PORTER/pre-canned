package com.netaporter.precanned

import spray.http.{ HttpEntity, ContentType, HttpHeader, StatusCode }
import spray.http.HttpHeaders.`Content-Type`
import scala.io.Source

trait CannedResponses {
  def status(s: StatusCode): Precanned = r =>
    r.copy(status = s)

  def header(h: HttpHeader): Precanned = r =>
    r.copy(headers = h :: r.headers)

  def contentType(c: ContentType) =
    header(`Content-Type`(c))

  def entity(e: HttpEntity): Precanned = r =>
    r.withEntity(e)

  def resource(filename: String): Precanned = r => {
    val resource = getClass.getResourceAsStream(filename)
    val source = Source.fromInputStream(resource)
    val content = source.mkString
    source.close
    entity(content)(r)
  }
}
