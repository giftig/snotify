package com.xantoria.snotify.persist

import java.io.{BufferedWriter, File, FileWriter}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.matching.Regex

import com.typesafe.scalalogging.StrictLogging
import spray.json._

import com.xantoria.snotify.config.Config
import com.xantoria.snotify.model.Notification
import com.xantoria.snotify.serialisation.JsonProtocol._

/**
 * Basic filesystem storage for notifications
 *
 * This stores all active notifications as files in the allocated directory.
 *
 * Obviously, this has performance and scalability limitations. It shouldn't be used if there's
 * a large number of notifications. Writes must be kept single-threaded to avoid racing, so
 * `persist.threads` should always be set to 1 when using this storage. It's suitable for use on
 * desktop clients to avoid a dependency on a real database, and where traffic is limited.
 */
class FileStorage extends Persistence with StrictLogging {
  private val cfg = Config.persistConfig
  private val path: File = {
    val f = new File(cfg.getString("path"))
    ensureDirExists(f)
    f
  }
  private val completedPath: File = {
    val f = new File(path, "complete")
    ensureDirExists(f)
    f
  }

  /**
   * Check that the configured path is usable, creating it if absent and throwing if non-writable
   */
  private def ensureDirExists(dir: File): Unit = {
    if (!dir.exists) {
      logger.info(s"Creating non-existent storage dir: $dir")
      dir.mkdirs
    }

    if (!dir.canWrite) {
      throw new IllegalArgumentException(s"Non-writable storage dir: $dir")
    }
  }

  /**
   * Creates the appropriate name for a new file from the given notification
   *
   * This is simply the ID of the notification with a json extension, but another strategy may be
   * needed later so it's formalised here.
   */
  private def createFilename(n: Notification): String = s"${n.id}.json"

  /**
   * Save the given notification to a new file
   *
   * @throws NotificationConflict if a file already exists with this notification ID
   */
  override def save(n: Notification)(implicit ec: ExecutionContext): Future[Unit] = Future {
    val fn = createFilename(n)
    logger.info(s"Writing $fn")

    val f = new File(path, fn)
    if (f.exists) {
      throw new Persistence.NotificationConflict(n.id)
    }

    val data = n.toJson.compactPrint
    val out = new BufferedWriter(new FileWriter(f))
    out.write(data)
    out.close()
  }

  /**
   * Scans for all *.json files in the storage dir and attempts to read the notifications
   */
  override def findPending()(implicit ec: ExecutionContext): Future[Seq[Notification]] = {
    val files: Seq[File] = path.listFiles.toSeq filter {
      f: File => FileStorage.FilenamePattern.findFirstMatchIn(f.getName).isDefined
    }

    val results: Future[Seq[Option[Notification]]] = Future.traverse(files) { f: File =>
      Future {
        val src = Source.fromFile(f)
        val data = try {
          src.mkString
        } finally {
          src.close()
        }

        Some(data.parseJson.convertTo[Notification])
      } recover {
        case NonFatal(e) => {
          logger.error(s"Dropping notification file $f as an unexpected error occurred!", e)
          None
        }
      }
    }
    results map { _.flatten }
  }

  override def markComplete(n: Notification): Future[Unit] = {
    logger.info(s"Marking notification ${n.id} complete")
    val fn = createFilename(n)
    val src = new File(path, fn)
    val dest = new File(completedPath, fn)
    src.moveTo(dest)
  }
}

object FileStorage {
  val FilenamePattern: Regex= """.+\.json$""".r
}
