package com.xantoria.snotify.targeting

/**
 * Resolve whether messages are going to self, peer, neither, or both
 */
class TargetResolver(
  override protected val selfTargets: Set[String],
  override protected val peers: Set[String]
) extends TargetResolution

object TargetResolver {
  /**
   * Construct a TargetResolver from information about self, peers, and group definitions
   *
   * Our own clientId, and any groups to which we belong, will identify as self
   * Our list of peer IDs, and any groups which contain any of those peer IDs, will be peers
   */
  def apply(clientId: String, peerIds: Set[String], groups: Set[TargetGroup]): TargetResolver = {
    new TargetResolver(
      groups.collect {
        case TargetGroup(name, members) if members.contains(clientId) => name
      } + clientId,
      groups.collect {
        case TargetGroup(name, members) if members.intersect(peerIds).nonEmpty => name
      } ++ peerIds
    )
  }
}
