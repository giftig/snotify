package com.xantoria.snotify.rest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/**
 * All routes belonging to the rest API
 */
trait Routing extends AdminRouting with RegisterNotificationRouting {
  lazy val routes: Route = adminRoutes ~ registrationRoutes
}
