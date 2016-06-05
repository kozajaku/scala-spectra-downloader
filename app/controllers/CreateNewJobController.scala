package controllers

import java.util.concurrent.atomic.AtomicInteger
import javax.inject._
import actors.SpectraDownloadingActor
import akka.actor.{PoisonPill, ActorRef, ActorSystem}
import akka.stream.Materializer
import play.api.libs.json._
import play.api.mvc._
import services.JobDatabase
import utils.model.{SpectraDownloadConfiguration, Directory, DatalinkConfig, Authorization}
import utils.parser.model.IndexedSSAPVotable

import scala.concurrent.{Future, ExecutionContext}

/**
  * Created by radiokoza on 3.6.16.
  */
@Singleton
class CreateNewJobController @Inject()(database: JobDatabase, implicit val actorSystem: ActorSystem, materializer: Materializer, implicit val ec: ExecutionContext) extends Controller {

  import actors.VotableResolverActor
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._
  import scala.collection.mutable

  implicit val timeout = Timeout(5.seconds)

  val votableResolverActor: ActorRef = actorSystem.actorOf(VotableResolverActor.props)
  val parsedMap: mutable.Map[Int, IndexedSSAPVotable] = mutable.Map()
  val counter = new AtomicInteger()

  def index = Action {
    Ok(views.html.createJob())
  }

  def downloadVotable = Action.async { request =>
    import VotableResolverActor._
    val url: String = request.body.asText.getOrElse("").trim
    if (url == "") Future {
      BadRequest(invalidUrl)
    } else {
      (votableResolverActor ? VotableByDownload(url)).mapTo[ResolverResponse].map {
        case ParsingSuccess(votable) =>
          if (votable.getQueryStatus == "OK") {
            val parsedId = counter.getAndIncrement()
            parsedMap.put(parsedId, votable)
            Ok(parsingSuccess(parsedId, votable, Option(url)))
          }
          else BadRequest(badQueryStatus(votable.getQueryStatus))
        case DownloadFailed(ex) =>
          BadRequest(downloadFailed(ex))
        case ParsingFailed =>
          BadRequest(parsingFailed)
      }
    }
  }

  def directVotableInput = Action.async {request =>
    import VotableResolverActor._
    val input: String = request.body.asText.getOrElse("").trim
    if (input == "") Future {
      BadRequest(invalidUrl)
    } else {
      (votableResolverActor ? VotableByUpload(input)).mapTo[ResolverResponse].map {
        case ParsingSuccess(votable) =>
          if (votable.getQueryStatus == "OK") {
            val parsedId = counter.getAndIncrement()
            parsedMap.put(parsedId, votable)
            Ok(parsingSuccess(parsedId, votable, None))
          }
          else BadRequest(badQueryStatus(votable.getQueryStatus))
        case DownloadFailed(ex) =>
          BadRequest(downloadFailed(ex))
        case ParsingFailed =>
          BadRequest(parsingFailed)
      }
    }
  }

  private lazy val invalidUrl: JsValue = {
    Json.obj(
      "status" -> JsString("error"),
      "message" -> JsString("Resource URL address is invalid")
    )
  }

  private lazy val parsingFailed: JsValue = {
    Json.obj(
      "status" -> JsString("error"),
      "message" -> JsString("Unable to parse VOTABLE")
    )
  }

  private def badQueryStatus(status: String): JsValue = {
    Json.obj(
      "status" -> JsString("error"),
      "message" -> JsString(s"Bad SSAP query status: $status")
    )
  }

  private def downloadFailed(ex: Exception): JsValue = {
    Json.obj(
      "status" -> JsString("error"),
      "message" -> JsString(s"Unable to download VOTABLE: ${ex.getMessage}")
    )
  }

  private def parsingSuccess(parsedId: Int, votable: IndexedSSAPVotable, url: Option[String]): JsValue = {
    import scala.collection.JavaConversions._
    Json.obj(
      "status" -> JsString("ok"),
      "id" -> JsNumber(parsedId),
      "parsedData" -> Json.obj(
        "url" -> url.map(JsString).getOrElse[JsValue](JsNull),
        "recordCount" -> JsNumber(votable.getRows.size),
        "datalinkAvailable" -> JsBoolean(votable.isDatalinkAvailable),
        "datalinkOptions" -> (
          if (!votable.isDatalinkAvailable) JsNull
          else {
            //datalink available
            votable.getDatalinkInputParams.toList.filterNot(_.isIdParam).map{param =>
              if (param.getOptions.isEmpty){
                //no options
                Json.obj(
                  "optionsSet" -> JsBoolean(false),
                  "name" -> JsString(param.getName)
                )
              } else {
                //selection
                Json.obj(
                  "optionsSet" -> JsBoolean(true),
                  "name" -> JsString(param.getName),
                  "options" -> (("Nothing selected" -> "") :: param.getOptions.toList.map(o => o.getName -> o.getValue)).map{
                    case (k, v) => Json.arr(k, v)
                  }
                )
              }
            }
          })
      )
    )
  }

  def test = Action.async { request =>
    import actors.VotableResolverActor._
    val resolverActor: ActorRef = actorSystem.actorOf(VotableResolverActor.props)
    val body = request.body
    val text = body.asText.getOrElse("invalid")
    (resolverActor ? VotableByDownload(text)).mapTo[ResolverResponse].map {
      case ParsingSuccess(votable) =>
        val downloadingActor: ActorRef = actorSystem.actorOf(SpectraDownloadingActor.props)
        val testClient: ActorRef = actorSystem.actorOf(actors.test.PrintingClientActor.props)
        val observer: ActorRef = actorSystem.actorOf(actors.DownloadObserverActor.props(testClient, downloadingActor))
        downloadingActor ! SpectraDownloadingActor.InitiateDownloading(votable,
          SpectraDownloadConfiguration(Directory("outputDir"), None, /*Option(DatalinkConfig(List("test" -> "value")))*/ None))
        Ok(votable.getQueryStatus)
      case s: ResolverFailed =>
        InternalServerError("Failed")
    }
  }
}
