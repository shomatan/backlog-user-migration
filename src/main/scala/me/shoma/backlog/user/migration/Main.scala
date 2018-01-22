package me.shoma.backlog.user.migration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.implicits._
import backlog4s.apis.UserApi
import backlog4s.datas.{AccessKey, AddUserForm}
import backlog4s.dsl.syntax._
import backlog4s.interpreters.AkkaHttpInterpret
import me.shoma.backlog.user.migration.command.CommandLineArgsParser

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends App with CommandLineArgsParser {

  // Q1. This is not a pure function. How to improve?
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
    // Q2. How to get large number of users with FP?
    users <- UserApi.all(limit = 1000).orFail
  } yield users

  // Q3. Good solution to output progress.

  val result = for {
    users <- prg.foldMap(srcInterpreter)
    createdUserPrgs = users.map { user =>
      val form = AddUserForm(
        userId = user.userId.getOrElse(""),
        password = "aaaaaaaa",
        name = user.name,
        mailAddress = user.mailAddress,
        roleType = user.roleType
      )
      UserApi.create(form).orFail
    }
    createdUsers <- Future.sequence(
      createdUserPrgs.map(_.foldMap(dstInterpreter))
    )
  } yield createdUsers

  result.onComplete {
    case Success(createdUsers) =>
      println(createdUsers)
    case Failure(ex) =>
      println(ex.printStackTrace())
  }

  // Q4. How to shutdown akka safely?
}