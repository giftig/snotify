package com.xantoria.snotify.rest

import akka.http.scaladsl.server.Route

/**
 * All routes belonging to the rest API
 */
trait Routing extends RegisterNotificationRouting {
  lazy val routes: Route = registrationRoutes
}
