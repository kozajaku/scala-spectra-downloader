package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import play.api.libs.json.{JsBoolean, JsNumber, Json}
import play.api.mvc._
import services.JobDatabase

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by radiokoza on 5.6.16.
  */
@Singleton
class ApiController @Inject()(database: JobDatabase, implicit val actorSystem: ActorSystem, materializer: Materializer, implicit val ec: ExecutionContext) extends Controller {

  import scala.concurrent.duration._

  implicit val timeout = Timeout(5.seconds)

  def jobStates = Action.async {
    import actors.SpectraDownloadingActor._

    val responses = database.toArray.map(_.downActor).map { actor =>
      (actor ? AskDownloadState).mapTo[DownloadStateResponse].map {
        case DownloadStateResponse(_, _, remaining) => remaining == 0
      }
    }
    val futValues = Future.sequence(responses.toSeq)
    futValues.map(_.zipWithIndex).map { arr =>
      Ok(Json.obj(
        "jobs" -> arr.map {
          case (state, index) =>
            Json.obj(
              "id" -> JsNumber(index),
              "finished" -> JsBoolean(state)
            )
        }
      ))
    }
  }

}
