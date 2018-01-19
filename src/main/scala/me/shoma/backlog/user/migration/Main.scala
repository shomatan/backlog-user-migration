package me.shoma.backlog.user.migration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.implicits._
import backlog4s.apis.UserApi
import backlog4s.datas.AccessKey
import backlog4s.interpreters.AkkaHttpInterpret
import me.shoma.backlog.user.migration.command.CommandLineArgsParser

import scala.util.{Failure, Success}

object Main extends App with CommandLineArgsParser {

  val config = parse(args) // if validation failed, sys.exit(1)

  implicit val system = ActorSystem("backlog-user-migration")
  implicit val mat = ActorMaterializer()
  implicit val exc = system.dispatcher

  val srcInterpreter = new AkkaHttpInterpret(
    s"${config.srcBacklogUrl}/api/v2/", AccessKey(config.srcBacklogKey)
  )
  val dstInterpreter = new AkkaHttpInterpret(
    s"${config.dstBacklogUrl}/api/v2/", AccessKey(config.dstBacklogKey)
  )

  val prg = for {
    users <- UserApi.all()
  } yield users

  prg.foldMap(srcInterpreter).onComplete { result =>
    result match {
      case Success(data) => data.foreach(println)
      case Failure(ex) => ex.printStackTrace()
    }
    system.terminate()
  }


}