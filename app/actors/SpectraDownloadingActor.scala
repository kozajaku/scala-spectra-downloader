package actors

import akka.actor.{ActorRef, Props, Actor}
import utils.model.SpectraDownloadConfiguration
import utils.parser.model.IndexedSSAPVotable

import scala.collection.mutable.ListBuffer

/**
  * Created by radiokoza on 4.6.16.
  */
object SpectraDownloadingActor {
  def props = Props[SpectraDownloadingActor]

  case class InitiateDownloading(votable: IndexedSSAPVotable, configuration: SpectraDownloadConfiguration)

  case object RegisterObserver

  case class SpectrumDownloadSuccess(id: Int)

  case class SpectrumDownloadFailed(id: Int, ex: Exception)

  case object IsDownloadFinished

  trait DownloadFinishedResponse

  case object Yes extends DownloadFinishedResponse

  case object No extends DownloadFinishedResponse

  object DownloadState extends Enumeration {
    type DownloadState = Value
    val Pending, Finished, Failed = Value
  }

}

class SpectraDownloadingActor extends Actor {

  import SpectraDownloadingActor._
  import scala.collection.mutable


  private val spectra: mutable.ListBuffer[SpectrumRepresentation] = ListBuffer()
  private val spectraState: mutable.ListBuffer[DownloadState.Value] = ListBuffer()
  private var completed: Int = 0
  private val observers: ListBuffer[ActorRef] = ListBuffer()

  def receive = {
    case InitiateDownloading(votable, configuration) =>
      constructUrls(votable, configuration)
      //debug TODO
      println(spectra.map{_.url})
  }

  private def constructUrls(votable: IndexedSSAPVotable, configuration: SpectraDownloadConfiguration): Unit = {
    import scala.collection.JavaConversions._
    configuration.datalink match {
      case None => //use accrefs
        spectra ++= votable.getRows.toList.map {
          votable.getAccrefColumn(_)
        }.map{SpectrumRepresentation(_, "testFile")}
      case Some(datalinkConfig) if (votable.isDatalinkAvailable) => //use datalink
        val enc: String => String = java.net.URLEncoder.encode(_, "ASCII")
        val baseUrl = votable.getDatalinkResourceUrl
        val queryPostfix = datalinkConfig.queryParams
        val urlWithoutId = if (baseUrl.contains("?")) raw"$baseUrl&$queryPostfix" else raw"$baseUrl?$queryPostfix"
        val idPrefix = (if (urlWithoutId.endsWith("&") || urlWithoutId.endsWith("?")) "" else "&") + idParamName(votable) + "="
        spectra ++= votable.getRows.toList.map { r =>
          urlWithoutId + idPrefix + enc(votable.getPubDIDColumn(r))
        }.map{
          SpectrumRepresentation(_, "suggestedTodo")
        }
    }
  }

  private def idParamName(votable: IndexedSSAPVotable): String = {
    import scala.collection.JavaConversions._
    votable.getDatalinkInputParams.toList.filter(_.isIdParam)(0).getName
  }

  private case class SpectrumRepresentation(url: String, suggestedName: String, state: DownloadState.Value = DownloadState.Pending)

}
