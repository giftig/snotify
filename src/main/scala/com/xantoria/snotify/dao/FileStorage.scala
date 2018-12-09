package com.xantoria.snotify.dao

import com.typesafe.config.{Config => TConfig}

class FileStorage(override protected val cfg: TConfig) extends FileHandling
