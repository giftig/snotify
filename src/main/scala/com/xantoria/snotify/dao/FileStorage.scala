package com.xantoria.snotify.dao

import com.typesafe.config.{Config => TConfig}

import com.xantoria.snotify.config.Config

class FileStorage(override protected val cfg: TConfig) extends FileHandling
