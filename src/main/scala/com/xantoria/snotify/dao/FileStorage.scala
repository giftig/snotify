package com.xantoria.snotify.dao

import akka.actor.ActorSystem
import com.typesafe.config.{Config => TConfig}

class FileStorage(
  override protected val cfg: TConfig,
  system: ActorSystem
) extends FileHandling
