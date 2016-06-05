package actors

import actors.DownloadObserverActor.CurrentDownloadingState
import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import utils.model.SpectraDownloadConfiguration
import utils.parser.model.IndexedSSAPVotable

/**
  * Created by radiokoza on 4.6.16.
  */
object SpectraDownloadingActor {
  def props = Props[SpectraDownloadingActor]

  case class InitiateDownloading(votable: IndexedSSAPVotable, configuration: SpectraDownloadConfiguration)

  case object RegisterObserver

  case object UnregisterObserver

  case class SpectrumDownloadSuccess(id: Int)

  case class SpectrumDownloadFailed(id: Int, ex: Exception)

  case object AskDownloadState

  case class DownloadStateResponse(successful: Int, failed: Int, remaining: Int)

  object DownloadState extends Enumeration {
    type DownloadState = Value
    val Pending, Success, Failed = Value
  }

}

class SpectraDownloadingActor extends Actor {

  import SpectraDownloadingActor._

  import scala.collection.mutable


  private val spectra: mutable.ArrayBuffer[SpectrumRepresentation] = mutable.ArrayBuffer()
  private var completed: Int = 0
  private val observers: mutable.Set[ActorRef] = mutable.Set()
  private val exceptions: mutable.Map[Int, Exception] = mutable.Map()

  def receive = {
    case InitiateDownloading(votable, configuration) =>
      constructUrls(votable, configuration)
      val downloader: ActorRef = context.actorOf(SingleDownloadActor.props)
      spectra.zipWithIndex.foreach {
        case (repr, index) =>
          repr match {
            case SpectrumRepresentation(url, name, _) =>
              downloader ! SingleDownloadActor.DownloadSpectrum(index, url, name, configuration.directory, configuration.authorization)
          }
      }
    case RegisterObserver =>
      observers += sender()
      val state = CurrentDownloadingState(spectra.map(_.url).toArray, spectra.map(_.state).toArray, spectra.indices.map(exceptions.get).toArray)
      sender() ! state
    case UnregisterObserver =>
      observers -= sender()
    case SpectrumDownloadSuccess(id) =>
      val oldRepr = spectra(id)
      spectra(id) = SpectrumRepresentation(oldRepr.url, oldRepr.suggestedName, DownloadState.Success)
      completed += 1
      if (finished) sender() ! PoisonPill
      //inform all observers
      observers.foreach(_ ! SpectrumDownloadSuccess(id))
    case SpectrumDownloadFailed(id, ex) =>
      val oldRepr = spectra(id)
      spectra(id) = SpectrumRepresentation(oldRepr.url, oldRepr.suggestedName, DownloadState.Failed)
      exceptions.put(id, ex)
      completed += 1
      if (finished) sender() ! PoisonPill
      //inform all observers
      observers.foreach(_ ! SpectrumDownloadFailed(id, ex))
    case AskDownloadState =>
      sender() ! DownloadStateResponse(completed - exceptions.size, exceptions.size, spectra.size - completed)
  }

  private def finished = completed == spectra.size

  private def constructUrls(votable: IndexedSSAPVotable, configuration: SpectraDownloadConfiguration): Unit = {
    import scala.collection.JavaConversions._
    configuration.datalink match {
      case None => //use accrefs
        spectra ++= votable.getRows.toList.map {
          votable.getAccrefColumn
        }.map { accRef => SpectrumRepresentation(accRef, suggestName(accRef)) }
      case Some(datalinkConfig) if votable.isDatalinkAvailable => //use datalink
        val enc: String => String = java.net.URLEncoder.encode(_, "ASCII")
        val baseUrl = votable.getDatalinkResourceUrl
        val queryPostfix = datalinkConfig.queryParams
        val urlWithoutId = if (baseUrl.contains("?")) raw"$baseUrl&$queryPostfix" else raw"$baseUrl?$queryPostfix"
        val idPrefix = (if (urlWithoutId.endsWith("&") || urlWithoutId.endsWith("?")) "" else "&") + idParamName(votable) + "="
        spectra ++= votable.getRows.toList.map { r =>
          (urlWithoutId + idPrefix + enc(votable.getPubDIDColumn(r)), votable.getAccrefColumn(r))
        }.map {
          case (url, accRef) =>
            SpectrumRepresentation(url, suggestName(accRef))
        }
    }
  }

  private def idParamName(votable: IndexedSSAPVotable): String = {
    import scala.collection.JavaConversions._
    votable.getDatalinkInputParams.toList.filter(_.isIdParam).head.getName
  }

  private def suggestName(accRef: String): String = accRef.replaceAll("^.*?([^/]+)$", "$1").replaceAll("^(.*?)\\.?[^\\.]*$", "$1")

  private case class SpectrumRepresentation(url: String, suggestedName: String, state: DownloadState.Value = DownloadState.Pending)

}
