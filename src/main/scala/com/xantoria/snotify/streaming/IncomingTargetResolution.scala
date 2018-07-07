package com.xantoria.snotify.streaming

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._

import com.xantoria.snotify.model.ReceivedNotification
import com.xantoria.snotify.targeting.TargetResolution

/**
 * Defines graph shape(s) for dealing with targets in incoming messages
 */
trait IncomingTargetResolution[T <: ReceivedNotification] {
  protected val resolver: TargetResolution

  /**
   * A graph junction which directs incoming messages to one or more of three outputs
   *
   * See `TargetResolution` for more details. Messages will be directed to self, to peers, or
   * into the bin (if the target is unknown). Messages may have multiple targets, and therefore
   * may be directed to multiple output streams.
   *
   *                          ~~~ self ~~~> NotificationStreaming ~> persist ~> schedule
   * notification ~> resolver ~~~ peer ~~~> NotificationWriter ~> AMQP
   *                          ~~~ unk  ~~~> (ignored)
   */
  val resolverShape: Graph[UniformFanOutShape[T, T], NotUsed] = {
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val input = b.add(Flow[T])
      val bcast = b.add(Broadcast[T](outputPorts = 3))

      val forSelf = b.add(Flow[T].filter { n => resolver.isSelf(n.notification) })
      val forPeer = b.add(Flow[T].filter { n => resolver.isPeer(n.notification) })
      val unk = b.add(Flow[T].filter { n => resolver.isUnknown(n.notification) })

      input ~> bcast
      bcast ~> forSelf
      bcast ~> forPeer
      bcast ~> unk

      UniformFanOutShape(input.in, forSelf.out, forPeer.out, unk.out)
    }
  }
}
