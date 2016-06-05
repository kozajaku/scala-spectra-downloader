package controllers

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc._
import services.JobDatabase

import scala.concurrent.ExecutionContext

/**
  * Created by radiokoza 5.6.16
  */
@Singleton
class JobDetailsController @Inject()(database: JobDatabase, implicit val actorSystem: ActorSystem,
                                     materializer: Materializer, implicit val ec: ExecutionContext) extends Controller {

  def jobDetails(jobId: Int) = Action{
    Ok("Viewing details of job " + jobId)
  }

}
