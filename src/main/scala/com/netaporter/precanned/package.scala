package com.netaporter

import com.netaporter.precanned.HttpServerMock.PrecannedResponse
import spray.http.HttpRequest

package object precanned {
  type Expect = HttpRequest => Boolean
  type Precanned = PrecannedResponse => PrecannedResponse
}
