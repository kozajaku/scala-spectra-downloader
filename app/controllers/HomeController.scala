package controllers

import javax.inject._

import actors.SpectraDownloadingActor
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import play.api.mvc._
import services.JobDatabase

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(database: JobDatabase, implicit val actorSystem: ActorSystem,
                               materializer: Materializer, implicit val ec: ExecutionContext) extends Controller {


  def index = Action {
    import utils.model.{Directory, JobInfo}
    val someActor: ActorRef = actorSystem.actorOf(SpectraDownloadingActor.props)
    val jobInfo: JobInfo = JobInfo(someActor, Option("http://blablasource.xml"), 25, Directory("/some/dir"))
    database += jobInfo
    database += jobInfo
    database += jobInfo
    Ok(views.html.index(database.toArray))
  }

}
