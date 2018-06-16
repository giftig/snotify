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

  /**
   * Check whether the given target is this node, a peer node, or unknown
   */
  protected def identify(target: String): Result = target match {
    case t if t == selfTarget => Self
    case t if peers.contains(t) => Peer
    case _ => Unknown
  }

  protected def isSelf(n: Notification): Boolean = n.targets.map(identify).contains(Self)
  protected def isPeer(n: Notification): Boolean = n.targets.map(identify).contains(Peer)
  protected def isUnknown(n: Notification): Boolean = n.targets.map(identify).contains(Unknown)

  /**
   * Filter the notification's list of targets to those which correspond to known peers
   */
  protected def peerTargets(n: Notification): Seq[String] = {
    n.targets collect { case t if identify(t) == Peer => t }
  }
}

object TargetResolution {
  sealed trait Result

  case object Self extends Result
  case object Peer extends Result
  case object Unknown extends Result
}
