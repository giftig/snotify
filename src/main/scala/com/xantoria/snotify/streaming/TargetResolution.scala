package com.xantoria.snotify.streaming

import com.xantoria.snotify.model.Notification

/**
 * Figures out whether a notification belongs to this node, another node, or no identifiable node
 */
trait TargetResolution {
  import TargetResolution._

  // TODO: Need to handle groups as well
  protected val selfTarget: String
  protected val peers: Set[String]

  protected def identify(target: String): Result = target match {
    case t if t == selfTarget => Self
    case t if peers.contains(t) => Peer
    case _ => Unknown
  }

  protected def isSelf(n: Notification) = n.targets.map(identify).contains(Self)
  protected def isPeer(n: Notification) = n.targets.map(identify).contains(Peer)
  protected def isUnknown(n: Notification) = n.targets.map(identify).contains(Unknown)
}

object TargetResolution {
  sealed trait Result

  case object Self extends Result
  case object Peer extends Result
  case object Unknown extends Result
}
