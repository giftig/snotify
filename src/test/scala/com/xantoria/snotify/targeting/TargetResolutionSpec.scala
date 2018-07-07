package com.xantoria.snotify.targeting

import com.xantoria.snotify.SpecKit
import com.xantoria.snotify.streaming.TestNotification

class TargetResolutionSpec extends SpecKit with TargetResolution {
  import TargetResolution._

  private val clientId = "test-targetres-self"
  private val peerIds = Set("test-targetres-peer1", "test-targetres-peer2")
  private val commonGroup = "test-targetres-commongroup"

  override protected val selfTargets: Set[String] = Set(clientId) + commonGroup
  override protected val peers: Set[String] = peerIds + commonGroup

  "TargetResolution" should "identify self" in {
    identify(clientId) should be(OnlySelf)
  }

  it should "identify peers" in {
    peerIds foreach { p => identify(p) should be(OnlyPeer) }
  }

  it should "identify groups" in {
    identify(commonGroup) should be(MixedGroup)
  }

  it should "identify unknown" in {
    identify("hodorhodorhodor") should be(Unknown)
  }

  it should "detect self" in {
    val pos = Seq(
      TestNotification(targets = Seq(clientId, "hodor")),
      TestNotification(targets = Seq(commonGroup, peerIds.head))
    ) map { _.notification }
    val neg = Seq(
      TestNotification(targets = Seq(peerIds.head, "hodor")),
      TestNotification(targets = Seq("hodor"))
    ) map { _.notification }

    pos foreach { isSelf(_) should be(true) }
    neg foreach { isSelf(_) should be(false) }
  }

  it should "detect peer" in {
    val pos = Seq(
      TestNotification(targets = Seq(peerIds.head, "hodor")),
      TestNotification(targets = Seq(commonGroup))
    ) map { _.notification }
    val neg = Seq(
      TestNotification(targets = Seq(clientId, "hodor")),
      TestNotification(targets = Seq("hodor"))
    ) map { _.notification }

    pos foreach { isPeer(_) should be(true) }
    neg foreach { isPeer(_) should be(false) }
  }

  it should "detect unknown" in {
    val pos = Seq(
      TestNotification(targets = Seq(clientId, "hodor")),
      TestNotification(targets = Seq(peerIds.head, "hodor")),
      TestNotification(targets = Seq("hodor"))
    ) map { _.notification }
    val neg = Seq(
      TestNotification(targets = Seq(commonGroup, peerIds.head))
    ) map { _.notification }

    pos foreach { isUnknown(_) should be(true) }
    neg foreach { isUnknown(_) should be(false) }
  }
}
