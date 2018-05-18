package com.xantoria.snotify.alert.desktop

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import com.typesafe.config.{Config => TypesafeConfig}
import com.typesafe.scalalogging.StrictLogging

import com.xantoria.snotify.alert.AlertHandling
import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification

class AudioAlert extends AlertHandling with StrictLogging {
  import AudioAlert._

  private lazy val cfg = Config.alertingConfig.getConfig("audio")
  // FIXME: Differ based on priority, and maybe source
  private lazy val alertSound = PlaybackConfig(cfg.getConfig("sound"))
  private lazy val driver = AudioDriver.forName(cfg.getString("driver"))

  override def triggerAlert(n: Notification)(implicit ec: ExecutionContext): Future[Boolean] = {
    logger.debug(s"Playing sound $alertSound using driver ${driver.name}...")
    driver.play(alertSound)
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

  trait SubprocessAudioDriver extends AudioDriver {
    protected def command(sound: PlaybackConfig): Seq[String]

    override def play(sound: PlaybackConfig)(implicit ec: ExecutionContext): Future[Boolean] = {
      import scala.sys.process._

      Future(command(sound) ! ProcessLogger(_ => ())) map { _ == 0 }
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
