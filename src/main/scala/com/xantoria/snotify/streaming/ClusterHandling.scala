package com.xantoria.snotify.streaming

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.{Notification, ReceivedNotification}
import com.xantoria.snotify.queue.NotificationWriting

/**
 * Represents handling cluster mechanics by reading and writing to relevant queues
 *
 * Each node will read from two queues:
 *   - A personal queue to which notifications will be delivered for this node only
 *   - A cluster queue which may receive notifications which require retargeting to another node
 *
 * When reading from the cluster queue, each node will:
 *   - Figure out which node is the target for the message
 *     - If the target is the node which reads it, it will schedule it internally only
 *     - If the target is a single known peer, it will move the message to the personal queue of
 *       the target node.
 *     - If the target is a group of nodes, it will do one or both of the previous actions for
 *       each target in the group (i.e. schedule internally if the current node is in the group,
 *       and also place a copy of the message on the personal queue for each other member node)
 *     - If the target is unknown, the message will be rejected
 *
 * Though this is phrased in terms of queues, it's designed to be generic enough to accept generic
 * notification readers and writers, so it could just as well accept notifications via some other
 * mechanic if desired.
 */
trait ClusterHandling[T <: ReceivedNotification]
  extends NotificationSource[T]
  with TargetResolution
  with StrictLogging {

  protected val personalReader: NotificationSource[T]
  protected val clusterReader: NotificationSource[T]
  protected val notificationWriter: NotificationWriting

  /**
   * A graph junction which directs incoming messages to one or more of three outputs
   *
   * See `TargetResolution` for more details. Messages will be directed to self, to peers, or
   * into the bin (if the target is unknown). Messages may have multiple targets, and therefore
   * may be directed to multiple output streams.
   */
  private val targetResolver: Graph[UniformFanOutShape[T, T], NotUsed] = {
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val input = b.add(Flow[T])
      val bcast = b.add(Broadcast[T](outputPorts = 3))

      val forSelf = b.add(Flow[T].filter { n => isSelf(n.notification) })
      val forPeer = b.add(Flow[T].filter { n => isPeer(n.notification) })
      val unk = b.add(Flow[T].filter { n => isUnknown(n.notification) })

      input ~> bcast
      bcast ~> forSelf
      bcast ~> forPeer
      bcast ~> unk

      UniformFanOutShape(input.in, forSelf.out, forPeer.out, unk.out)
    }
  }

  private val errorSink: Sink[T, _] = Sink.foreach {
    n: T => logger.error(s"$n contained an unidentified target")
  }

  override def source(): Source[T, NotUsed] = {
    val g = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val merger = b.add(Merge[T](2))
      val resolver = b.add(targetResolver)
      val cluster = clusterReader.source()
      val personal = personalReader.source()
      val unwrapNotification = Flow[ReceivedNotification].map { n => n.notification}

      // TODO: Handle peer messages: need to attach a component which writes to RMQ
      cluster ~> resolver
      resolver ~> merger
      resolver ~> unwrapNotification ~> notificationWriter.sink
      resolver ~> errorSink

      personal ~> merger

      SourceShape(merger.out)
    }

    Source.fromGraph(g)
  }
}
