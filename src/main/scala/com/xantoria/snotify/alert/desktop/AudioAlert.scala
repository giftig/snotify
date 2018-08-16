package com.xantoria.snotify.alert.desktop

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import com.typesafe.config.{Config => TypesafeConfig, ConfigObject}
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert.AlertHandling
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.utils.PriorityTranslator

class AudioAlert extends AlertHandling with StrictLogging {
  import AudioAlert._

  private lazy val cfg = Config.alertingConfig.getConfig("audio")
  private lazy val driver = AudioDriver.forName(cfg.getString("driver"))

  // Provide a priority to the decider to figure out what the sound should be
  private val alertSound: PriorityTranslator[PlaybackConfig] = {
    PriorityTranslator.fromConfig(cfg.getObject("sounds")) {
      case section: ConfigObject => PlaybackConfig(section.toConfig)
      case _ => throw new IllegalArgumentException("Unexpected sound config!")
    }
  }

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    val sound = alertSound(n.priority)
    logger.debug(s"Playing sound ${sound.filename} using driver ${driver.name}...")
    driver.play(sound)
  }
}

object AudioAlert {
  /**
   * Represents the details of a sound to play, including sound filename and playback properties
   */
  case class PlaybackConfig(
    filename: String,
    repeats: Int = 1,
    cutoffTime: Option[Duration] = None
  )

  // TODO: Properly support durations rather than using a number of seconds
  object PlaybackConfig {
    def apply(cfg: TypesafeConfig): PlaybackConfig = PlaybackConfig(
      filename = cfg.getString("file"),
      repeats = if (cfg.hasPath("repeats")) cfg.getInt("repeats") else 1,
      cutoffTime = if (cfg.hasPath("cutoff")) Some(cfg.getInt("cutoff").seconds) else None
    )
  }

  trait AudioDriver {
    val name: String

    /**
     * Play the sound and return a boolean indicating whether playback succeeded when complete
     */
    def play(sound: PlaybackConfig)(implicit ec: ExecutionContext): Future[Boolean]
  }

  trait SubprocessAudioDriver extends AudioDriver with StrictLogging {
    protected def command(sound: PlaybackConfig): Seq[String]

    override def play(sound: PlaybackConfig)(implicit ec: ExecutionContext): Future[Boolean] = {
      import scala.sys.process._

      val cmd = command(sound)
      logger.debug(s"Running command: $cmd")

      Future(cmd ! ProcessLogger(_ => ())) map { _ == 0 }
    }
  }

  object Mplayer extends SubprocessAudioDriver {
    override val name: String = "mplayer"

    override protected def command(sound: PlaybackConfig): Seq[String] = {
      val baseCommand = Seq(
        "/usr/bin/env",
        "mplayer",
        "-really-quiet", // I shit you not, that's the actual flag
        "-msglevel", "all=0",
        "-loop", sound.repeats.toString
      )
      val endPos = sound.cutoffTime map { d => Seq("-endpos", d.toSeconds.toString) } getOrElse Nil

      baseCommand ++: endPos ++: Seq(sound.filename)
    }
  }

  object AudioDriver {
    def forName(s: String): AudioDriver = s match {
      case Mplayer.name => Mplayer
      case _ => throw new IllegalArgumentException(s"Unsupported audio driver $s")
    }
  }
}
