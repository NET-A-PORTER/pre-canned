package com.netaporter

import akka.http.scaladsl.model.HttpRequest
import com.netaporter.precanned.HttpServerMock.PrecannedResponse

package object precanned {
  type Expect = HttpRequest => Boolean
  type Precanned = PrecannedResponse => PrecannedResponse
}
