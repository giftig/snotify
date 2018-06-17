package com.xantoria.snotify.rest

import scala.util.{Failure, Success}
import scala.util.control.NonFatal

import akka.actor.ActorRef
import akka.http.scaladsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server._
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, Sink}
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.{Notification, ReceivedNotification}
import com.xantoria.snotify.serialisation.JsonProtocol._

import Directives._

/**
 * Routes related to registering new notifications to the cluster
 */
trait RegisterNotificationRouting extends SprayJsonSupport with StrictLogging {
  protected implicit val materializer: Materializer

  protected val notificationSink: ActorRef

  protected lazy val registrationRoutes: Route = pathPrefix("notification") {
    pathEndOrSingleSlash {
      post {
        entity(as[Notification]) { n =>
          val wrapped = RestNotification(n)
          notificationSink ! wrapped

          onComplete(wrapped.result) {
            case Success(_) => complete(200 -> "OK")
            case Failure(NonFatal(e)) => complete(500 -> e.getMessage)
          }
        }
      }
    }
  }
}
