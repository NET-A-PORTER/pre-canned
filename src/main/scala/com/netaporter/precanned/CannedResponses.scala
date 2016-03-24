package com.netaporter.precanned

import spray.http._
import scala.concurrent.duration.FiniteDuration
import scala.io.Source

trait CannedResponses {

  def mapResponse(f: HttpResponse => HttpResponse): Precanned = precannedResponse =>
    precannedResponse.copy(response = f(precannedResponse.response))

  def status(s: StatusCode): Precanned = mapResponse { r =>
    r.copy(status = s)
  }

  def header(h: HttpHeader): Precanned = mapResponse { r =>
    r.copy(headers = h :: r.headers)
  }

  def contentType(c: ContentType): Precanned = mapResponse { r =>
    r.mapEntity { e => HttpEntity(c, e.data) }
  }

  def entity(e: HttpEntity): Precanned = mapResponse { r =>
    r.withEntity(e)
  }

  def resource(filename: String): Precanned = r => {
    val resource = getClass.getResourceAsStream(filename)
    val source = Source.fromInputStream(resource)
    val content = source.mkString
    source.close
    entity(content)(r)
  }

  def delay(duration: FiniteDuration): Precanned = response =>
    response.copy(delay = duration)
}
