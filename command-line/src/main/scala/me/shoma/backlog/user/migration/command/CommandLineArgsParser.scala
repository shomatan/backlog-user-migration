package me.shoma.backlog.user.migration.command

import cats.effect.IO

case class Config(
  srcBacklogUrl: String = "",
  srcBacklogKey: String = "",
  dstBacklogUrl: String = "",
  dstBacklogKey: String = ""
)

trait CmdLineParser {
  def parse(args: Seq[String]): IO[Config]
}

object OptionParser extends CmdLineParser {
  private val parser = new scopt.OptionParser[Config]("backlog-user-migration.jar") {

    head("Backlog user migration", "0.0.1")

    opt[String]("srcBacklogUrl").required().action( (x, c) =>
      c.copy(srcBacklogUrl = x) ).text("Source Backlog URL")

    opt[String]("srcBacklogKey").required().action( (x, c) =>
      c.copy(srcBacklogKey = x) ).text("Source Backlog API key")

    opt[String]("dstBacklogUrl").required().action( (x, c) =>
      c.copy(dstBacklogUrl = x) ).text("Destination Backlog URL")

    opt[String]("dstBacklogKey").required().action( (x, c) =>
      c.copy(dstBacklogKey = x) ).text("Destination Backlog API key")

    help("help") text "print this usage text."

  }

  override def parse(args: Seq[String]): IO[Config] =
    IO {
      parser.parse(args, Config()) match {
        case Some(config) => config
        case None => throw new IllegalArgumentException("Invalid config")
      }
    }

}

object CommandLineArgsParser {

  def parse(parser: CmdLineParser, args: Seq[String]): IO[Config] =
    parser.parse(args)

}
