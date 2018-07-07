package com.xantoria.snotify.targeting

import com.xantoria.snotify.model.Notification

/**
 * Figures out whether a notification belongs to this node, another node, or no identifiable node
 */
trait TargetResolution {
  import TargetResolution._

  protected val selfTargets: Set[String]
  protected val peers: Set[String]

  /**
   * Check whether the given target is this node, a peer node, or unknown
   */
  def identify(target: String): Result = target match {
    case t if selfTargets.contains(t) && peers.contains(t) => MixedGroup
    case t if selfTargets.contains(t) => OnlySelf
    case t if peers.contains(t) => OnlyPeer
    case _ => Unknown
  }

  def isSelf(n: Notification): Boolean = n.targets.map(identify) exists { _.isInstanceOf[Self] }
  def isPeer(n: Notification): Boolean = n.targets.map(identify) exists { _.isInstanceOf[Peer] }
  def isUnknown(n: Notification): Boolean = n.targets.map(identify).contains(Unknown)
}

object TargetResolution {
  sealed trait Result
  trait Self extends Result
  trait Peer extends Result

  case object OnlySelf extends Self
  case object OnlyPeer extends Peer
  case object MixedGroup extends Self with Peer
  case object Unknown extends Result
}
