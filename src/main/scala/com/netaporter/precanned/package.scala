package com.netaporter

import spray.http.{ HttpResponse, HttpRequest }

package object precanned {
  type Expect = HttpRequest => Boolean
  type Precanned = HttpResponse => HttpResponse
}
