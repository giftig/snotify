package com.xantoria.snotify.targeting

import com.xantoria.snotify.SpecKit

class TargetResolutionSpec extends SpecKit with TargetResolution {
  import TargetResolution._

  override protected val selfTarget: String = "test-targetres-self"
  override protected val peers: Set[String] = Set("test-targetres-peer1", "test-targetres-peer2")

  "TargetResolution" should "identify self" in {
    identify(selfTarget) should be(Self)
  }

  it should "identify peers" in {
    peers foreach { p => identify(p) should be(Peer) }
  }

  it should "identify unknown" in {
    identify("hodorhodorhodor") should be(Unknown)
  }
}
