package controllers

import javax.inject._
import actors.SpectraDownloadingActor
import akka.actor.{PoisonPill, ActorRef, ActorSystem}
import akka.stream.Materializer
import play.api.mvc._
import utils.model.{SpectraDownloadConfiguration, Directory, DatalinkConfig, Authorization}

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


  def test = Action.async { request =>
    import actors.VotableResolverActor._
    val resolverActor: ActorRef = actorSystem.actorOf(VotableResolverActor.props)

    val body = request.body
    val text = body.asText.getOrElse("invalid")
    (resolverActor ? VotableByDownload(text)).mapTo[ResolverResponse].map {
      case ParsingSuccess(votable) =>
        val downloadingActor = actorSystem.actorOf(SpectraDownloadingActor.props)
        downloadingActor ! SpectraDownloadingActor.InitiateDownloading(votable,
          SpectraDownloadConfiguration(Directory("outputDir"), None, /*Option(DatalinkConfig(List("test" -> "value")))*/ None))
        Ok(votable.getQueryStatus)
      case s:ResolverFailed =>
        InternalServerError("Failed")
    }
  }
}
