package com.netaporter.precanned

import akka.http.scaladsl.model.{ ContentType, HttpEntity }

/**
 * Adds convenient methods to Akka HTTP HttpEntity using the assumption that all the instances of it are
 * already strict.
 */
trait StrictHttpEntityOps {
  implicit class RichHttpEntity(private val inner: HttpEntity) {
    private def asStrict: HttpEntity.Strict = inner.asInstanceOf[HttpEntity.Strict]

    def asString: String = {
      val strict = asStrict
      strict.contentType match {
        case nonBinary: ContentType.NonBinary =>
          strict.data.decodeString(nonBinary.charset.nioCharset())
        case binary: ContentType.Binary =>
          sys.error(s"unable to read as a string entity with a binary content type $binary")
      }
    }
  }
}
