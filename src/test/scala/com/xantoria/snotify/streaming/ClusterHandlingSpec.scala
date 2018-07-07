package com.xantoria.snotify.streaming

import scala.concurrent.Future

import akka.Done
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.testkit.TestKit
import org.scalatest.concurrent._
import org.scalatest.time._

import com.xantoria.snotify.{SpecKit, StreamTesting}
import com.xantoria.snotify.model._
import com.xantoria.snotify.targeting.TargetResolver

import TestNotification._

import com.typesafe.config.ConfigFactory

class ClusterHandlingSpec
  extends TestKit(ActorSystem("ClusterHandlingSpec"))
  with SpecKit
  with StreamTesting {

  override implicit val patienceConfig = PatienceConfig(
    timeout = scaled(Span(2, Seconds)),
    interval = scaled(Span(50, Millis))
  )

  "The cluster handler" should "output notifications targeting self" in {
    val n = TestNotification()
    val probe = new TestNotificationWriter

    val h = ClusterHandlingSpec.createHandler(Seq(n), probe)
    val (actorRefHook: ActorRef, results: Future[TestNotification]) = {
      h.source.toMat(Sink.head)(Keep.both).run()
    }

    // Complete the actor hook so that both sources of this test stream are completed
    // otherwise the stream will never complete
    actorRefHook ! Status.Success("")

    results.futureValue should be(n)
  }

  it should "pass notifications targeting peers to its writer" in {
    val n = TestNotification(targets = TestNotification.Peers.toList)
    val probe = new TestNotificationWriter

    val h = ClusterHandlingSpec.createHandler(Seq(n), probe)
    val (actorRefHook, results) = h.source.toMat(Sink.seq)(Keep.both).run()

    // FIXME: Workaround for akka race bug :( See akka/akka #25285
    Thread.sleep(500)

    actorRefHook ! Status.Success("ok")

    results.futureValue should have length 0
    probe.notifications.head should be(n.notification)
  }

  it should "discard notifications which correspond to unknown targets" in {
    val n = TestNotification(targets = Seq("hodorhodorhodor"))
    val probe = new TestNotificationWriter

    val h = ClusterHandlingSpec.createHandler(Seq(n), probe)
    val (actorRefHook, results) = h.source.toMat(Sink.seq)(Keep.both).run()

    // FIXME: Workaround for akka race bug :( See akka/akka #25285
    Thread.sleep(500)

    actorRefHook ! Status.Success("ok")

    probe.notifications should have length 0
    results.futureValue should have length 0
  }

  it should "pass a notification to both self and peer if appropriate" in {
    val n = TestNotification(targets = Seq(SelfTarget, Peers.head))
    val probe = new TestNotificationWriter

    val h = ClusterHandlingSpec.createHandler(Seq(n), probe)
    val (actorRefHook, results) = h.source.toMat(Sink.seq)(Keep.both).run()

    // FIXME: Workaround for akka race bug :( See akka/akka #25285
    Thread.sleep(500)

    actorRefHook ! Status.Success("ok")

    results.futureValue should be(Seq(n))
    probe.notifications.head should be(n.notification)
  }
}

object ClusterHandlingSpec {

  /**
   * Convenience method to create a cluster handler connected to a notification source and writer
   *
   * The source is a TestNotificationSource containing the provided `notifications`, and the writer
   * is the probe passed to the method. The probe can be used to check which notifications are
   * sent to the peer queue writer, and the handler's source can be connected to a sink to check
   * notifications which are passed on to the outlet.
   */
  def createHandler(
    notifications: Seq[TestNotification],
    probe: TestNotificationWriter
  ): ClusterHandling[TestNotification] = {
    new ClusterHandler(
      new TestNotificationSource(notifications),
      probe,
      new IncomingTargetResolver[TestNotification](new TargetResolver(Set(SelfTarget), Peers))
    )
  }
}
