package me.shoma.backlog.user.migration.command

case class Config(
  srcBacklogUrl: String = "",
  srcBacklogKey: String = "",
  dstBacklogUrl: String = "",
  dstBacklogKey: String = ""
)

trait CommandLineArgsParser {

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

  def parse(args: Array[String]): Config =
    parser.parse(args, Config()) match {
      case Some(config) => config
      case None => sys.exit(1)
    }

}
