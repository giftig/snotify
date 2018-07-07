package com.xantoria.snotify.targeting

/**
 * A group is simply a named set of group members
 */
case class TargetGroup(name: String, members: Set[String])
