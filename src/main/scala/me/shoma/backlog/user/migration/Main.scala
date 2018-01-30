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

import scala.util.{Failure, Success}

object Main extends App {

  import ApiDsl.HttpOp._

  CommandLineArgsParser.parse(OptionParser, args).attempt.map {
    case Right(config) => start(config)
    case Left(_)       => sys.exit(1)
  }.unsafeRunSync()

  def fetchUsers(api: UserApi, offset: Int, limit: Int): ApiPrg[Seq[User]] = {
    def go(acc: Seq[User], current: Int): ApiPrg[Seq[User]] = {
      api.all(current).orFail.flatMap { users =>
        if (users.nonEmpty)
          go(acc ++ users, current + 100)
        else
          pure(acc)
      }
    }
    go(Seq(), 0)
  }

  type UserStreamF[A] = (Seq[User], Int, Int) => ApiPrg[A]

  def streamUser[A](api: UserApi, offset: Int, limit: Int)(f: UserStreamF[A]): ApiPrg[A] = {
    def go(current: Int): ApiPrg[A] = {
      api.all(current, limit).orFail.flatMap { users =>
        if (users.nonEmpty)
          f(users, current, limit).flatMap(_ => go(current + limit))
        else
          f(users, current, limit)
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