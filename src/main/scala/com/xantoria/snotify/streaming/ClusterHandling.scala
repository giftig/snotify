package com.xantoria.snotify.streaming

import akka.actor.ActorRef
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.model.ReceivedNotification

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
 * The ClusterHandling trait will deal with reading any notifications which should be considered
 * for possible redirection throughout the cluster - in practice that should mean anything that
 * hasn't been delivered to the personal queue, or otherwise marked as being for this node only.
 */
trait ClusterHandling[T <: ReceivedNotification] extends StrictLogging {
  protected val notificationSource: NotificationSource[T]
  protected val notificationWriter: NotificationWriting
  protected val targetResolver: IncomingTargetResolution[T]

  private val errorSink: Sink[T, _] = Sink.foreach {
    n: T => logger.error(s"$n contained an unidentified target")
  }

  val source: Source[T, ActorRef] = {
    // TODO: configure
    val actorRefHook = Source.actorRef[T](100, OverflowStrategy.dropNew)

    val g = GraphDSL.create(actorRefHook) { implicit b: GraphDSL.Builder[ActorRef] => actorRef =>
      import GraphDSL.Implicits._

      val merger = b.add(Merge[T](2))
      val resolver = b.add(targetResolver.resolverShape)
      val src = notificationSource.source()
      val unwrapNotification = Flow[T].map { _.notification }

      actorRef ~> merger
      src ~> merger

      merger ~> resolver
      resolver.out(1) ~> unwrapNotification ~> notificationWriter.sink
      resolver.out(2) ~> errorSink

      SourceShape(resolver.out(0))
    }

    Source.fromGraph(g)
  }
}
