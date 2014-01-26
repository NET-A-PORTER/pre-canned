package com.netaporter

import spray.http.{ HttpResponse, HttpRequest }

/**
 * Date: 24/01/2014
 * Time: 23:00
 */
package object precanned {
  type Expect = HttpRequest => Boolean
  type Precan = HttpResponse => HttpResponse
}
