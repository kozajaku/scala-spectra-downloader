package controllers

import javax.inject._
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
  * Created by radiokoza on 3.6.16.
  */
@Singleton
class CreateNewJobController @Inject()(implicit actorSystem: ActorSystem, materializer: Materializer, implicit val ec: ExecutionContext) extends Controller {

  import actors.VotableResolverActor
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._

  implicit val timeout = Timeout(5.seconds)

  val resolverActor: ActorRef = actorSystem.actorOf(VotableResolverActor.props)

  def test = Action.async { request =>
    import actors.VotableResolverActor._

    val body = request.body
    val text = body.asText.getOrElse("invalid")
    (resolverActor ? VotableByDownload(text)).mapTo[ParsingSuccess].map {
      result => Ok(result.votable.getQueryStatus())
    }
  }
}
