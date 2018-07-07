package com.xantoria.snotify.rest

import akka.http.scaladsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server._

import com.xantoria.snotify.admin.ServiceInfo
import com.xantoria.snotify.serialisation.JsonProtocol._

import Directives._

/**
 * Administrative endpoints such as health checks and service management
 */
trait AdminRouting extends SprayJsonSupport {
  private lazy val serviceInfo: Route = pathEndOrSingleSlash {
    get {
      complete {
        200 -> ServiceInfo()
      }
    }
  }

  protected lazy val adminRoutes: Route = serviceInfo
}
