package com.xantoria.snotify.targeting

import com.xantoria.snotify.SpecKit

class TargetResolverSpec extends SpecKit {
  import TargetResolution._

  private val selfTarget = "resolver-self"

  private val peer1 = "resolver-peer1"
  private val peer2 = "resolver-peer2"
  private val peers = Set(peer1, peer2)

  private val peerGroup = TargetGroup("resolver-peergroup", peers)
  private val selfGroup = TargetGroup("resolver-selfgroup", Set(selfTarget))
  private val mixedGroup = TargetGroup("resolver-mixedgroup", Set(selfTarget, peer1))

  private val groups = Set(peerGroup, selfGroup, mixedGroup)

  "TargetResolver" should "populate self and peers list correctly based on config" in {
    val resolver = TargetResolver(selfTarget, peers, groups)

    resolver.identify(selfTarget) should be(OnlySelf)
    peers foreach { resolver.identify(_) should be(OnlyPeer) }
    resolver.identify(peerGroup.name) should be(OnlyPeer)
    resolver.identify(selfGroup.name) should be(OnlySelf)
    resolver.identify(mixedGroup.name) should be(MixedGroup)
  }
}
