package actors

import actors.DownloadObserverActor.CurrentDownloadingState
import akka.actor.{ActorRef, Actor, Props}
import play.api.libs.json._

/**
  * Created by radiokoza on 5.6.16.
  */
object DownloadObserverActor {
  def props(client: ActorRef, downloader: ActorRef) = Props(new DownloadObserverActor(client, downloader))

  case class CurrentDownloadingState(urls: Array[String], state: Array[SpectraDownloadingActor.DownloadState.Value], exceptions: Array[Option[Exception]])

}

class DownloadObserverActor(client: ActorRef, downloader: ActorRef) extends Actor {

  override def preStart(): Unit = {
    //register this observer
    downloader ! SpectraDownloadingActor.RegisterObserver
  }

  override def postStop(): Unit = {
    //unregister this observer
    downloader ! SpectraDownloadingActor.UnregisterObserver
  }

  def receive = {
    case CurrentDownloadingState(urls, state, exceptions) =>
      client ! jsonTotalState(urls, state, exceptions)
    case SpectraDownloadingActor.SpectrumDownloadSuccess(id) =>
      client ! jsonChangedState(id, None)
    case SpectraDownloadingActor.SpectrumDownloadFailed(id, ex) =>
      client ! jsonChangedState(id, Some(ex))
  }

  def jsonTotalState(urls: Array[String], state: Array[SpectraDownloadingActor.DownloadState.Value], exs: Array[Option[Exception]]): JsValue = {
    Json.obj(
      "messageType" -> JsString("total"),
      "spectraState" ->
        (urls, state, exs).zipped.toList.zipWithIndex.map {
          case ((url, s, ex), index) =>
            Json.obj(
              "id" -> JsNumber(index),
              "url" -> JsString(url),
              "state" -> JsString(s.toString.toUpperCase()),
              "exception" -> (ex match {
                case None => JsNull
                case Some(e) => JsString(e.getMessage)
              })
            )
        }
    )
  }

  def jsonChangedState(id: Int, ex: Option[Exception]): JsValue = {
    import SpectraDownloadingActor.DownloadState._
    Json.obj(
      "messageType" -> JsString("update"),
      "spectraState" -> Json.obj(
        "id" -> JsNumber(id),
        "state" -> JsString(ex.map(e => Failed).getOrElse(Success).toString.toUpperCase()),
        "exception" -> (ex match {
          case None => JsNull
          case Some(e) => JsString(e.getMessage)
        })
      )
    )
  }

}
