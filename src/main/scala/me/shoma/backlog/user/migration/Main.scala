package me.shoma.backlog.user.migration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.implicits._
import backlog4s.apis.{AllApi, UserApi}
import backlog4s.datas.{AddUserForm, User}
import backlog4s.dsl.ApiDsl
import backlog4s.dsl.ApiDsl.ApiPrg
import backlog4s.dsl.syntax._
import backlog4s.interpreters.AkkaHttpInterpret
import me.shoma.backlog.user.migration.command.{CommandLineArgsParser, Config, OptionParser}

import scala.annotation.tailrec
import scala.util.{Failure, Success}

object Main extends App {

  import ApiDsl.HttpOp._

  CommandLineArgsParser.parse(OptionParser, args).attempt.map {
    case Right(config) => start(config)
    case Left(_)       => sys.exit(1)
  }.unsafeRunSync()

  type UserStreamF[A] = (Seq[User], Int, Int) => ApiPrg[A]

  def streamUser[A](api: UserApi, offset: Int, limit: Int)(f: UserStreamF[A]): ApiPrg[A] = {
    // @tailrec Q5. tailrec is fail
    def go(current: Int): ApiPrg[A] = {
      api.all(current, limit).orFail.flatMap { users =>
        if (users.isEmpty)
          f(users, current, limit)
        else
          f(users, current, limit).flatMap(_ => go(current + limit))
      }
    }
    go(0)
  }
  
  def start(config: Config): Unit = {

    implicit val system = ActorSystem("backlog-user-migration")
    implicit val mat = ActorMaterializer()
    implicit val exc = system.dispatcher

    val httpInterpret = new AkkaHttpInterpret
    val srcApi = AllApi.accessKey(s"${config.srcBacklogUrl}/api/v2/", config.srcBacklogKey)
    val dstApi = AllApi.accessKey(s"${config.dstBacklogUrl}/api/v2/", config.dstBacklogKey)

    val result = streamUser(srcApi.userApi, 0, 2) { (users, current, limit) =>
      users.map { user =>
        val form = AddUserForm(
          userId = user.userId.getOrElse(""),
          // Q4. I'm considering to encrypt the password.
          // Backlog API can be accepted only the password length between 8 and 20.
          // bcrypt generates a password of 60 characters
          password = "aaaaaaaa",
          name = user.name,
          mailAddress = user.mailAddress,
          roleType = user.roleType
        )
        dstApi.userApi.create(form).orFail
        // Q3. Good solution to output progress. IO modad
        // println(current + "/" + limit)
      }.sequence
    }

    result.foldMap(httpInterpret).onComplete { result =>
      result match {
        case Success(data) => println(data)
        case Failure(ex) => ex.printStackTrace()
      }
      system.terminate()
    }
  }
  
}