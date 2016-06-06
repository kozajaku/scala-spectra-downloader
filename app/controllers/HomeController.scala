package controllers

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.mvc._
import services.JobDatabase

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(database: JobDatabase, implicit val actorSystem: ActorSystem,
                               materializer: Materializer, implicit val ec: ExecutionContext) extends Controller {


  def index = Action {
    Ok(views.html.index(database.toArray))
  }

}
