package me.shoma.backlog.user.migration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.implicits._
import backlog4s.apis.AllApi
import backlog4s.datas.AddUserForm
import backlog4s.dsl.syntax._
import backlog4s.interpreters.AkkaHttpInterpret
import me.shoma.backlog.user.migration.command.{CommandLineArgsParser, Config, OptionParser}

import scala.util.{Failure, Success}

object Main extends App {

  CommandLineArgsParser.parse(OptionParser, args).attempt.map {
    case Right(config) => start(config)
    case Left(_)       => sys.exit(1)
  }.unsafeRunSync()

  def start(config: Config): Unit = {

    implicit val system = ActorSystem("backlog-user-migration")
    implicit val mat = ActorMaterializer()
    implicit val exc = system.dispatcher

    val httpInterpret = new AkkaHttpInterpret
    val srcApi = AllApi.accessKey(s"${config.srcBacklogUrl}/api/v2/", config.srcBacklogKey)
    val dstApi = AllApi.accessKey(s"${config.dstBacklogUrl}/api/v2/", config.dstBacklogKey)

    // Q3. Good solution to output progress.

    val result = for {
      users <- srcApi.userApi.all(limit = 1000).orFail // Q2. How to get large number of users with FP?
      createdUsers <- users.map { user =>
        val form = AddUserForm(
          userId = user.userId.getOrElse(""),
          password = "aaaaaaaa",
          name = user.name,
          mailAddress = user.mailAddress,
          roleType = user.roleType
        )
        dstApi.userApi.create(form).orFail
      }.sequence
    } yield createdUsers

    result.foldMap(httpInterpret).onComplete { result =>
      result match {
        case Success(data) => println(data)
        case Failure(ex) => ex.printStackTrace()
      }
      system.terminate()
    }
  }
  
}