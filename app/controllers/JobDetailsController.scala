package controllers

import javax.inject._

import actors.DownloadObserverActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services.JobDatabase

import scala.concurrent.ExecutionContext

/**
  * Created by radiokoza 5.6.16
  */
@Singleton
class JobDetailsController @Inject()(database: JobDatabase, implicit val actorSystem: ActorSystem,
                                     implicit val mat: Materializer, implicit val ec: ExecutionContext) extends Controller {

  def jobDetails(jobId: Int) = Action{implicit request =>
    try {
      Ok(views.html.jobDetails(database(jobId))(routes.JobDetailsController.socket(jobId).webSocketURL()))
    } catch {
      case e: IndexOutOfBoundsException => Redirect(routes.HomeController.index())
    }
  }

  def socket(jobId: Int) = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => DownloadObserverActor.props(out, database(jobId).downActor))
  }

}
