package com.xantoria.snotify.rest

import akka.http.scaladsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.serialisation.JsonProtocol._

import Directives._

/**
 * Routes related to registering new notifications to the cluster
 */
trait RegisterNotificationRouting extends SprayJsonSupport with StrictLogging {
  protected lazy val registrationRoutes: Route = pathPrefix("notification") {
    // FIXME: Stub for now
    path("ping") {
      post {
        entity(as[Notification]) { n =>
          logger.info(s"Notification posted to REST interface: $n")
          complete(200 -> "Pong!")
        }
      }
    }
  }
}
