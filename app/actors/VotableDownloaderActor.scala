package actors

import akka.actor.{Actor, Props}
import scala.io

/**
  * Created by radiokoza on 3.6.16.
  */

object VotableDownloaderActor {

  def props = Props[VotableDownloaderActor]

  case class DownloadVotable(url: String)

}

class VotableDownloaderActor extends Actor {

  import VotableDownloaderActor._

  def receive = {
    case DownloadVotable(url) =>
      try {
        val votable: String = io.Source.fromURL(url).mkString
        sender() ! VotableResolverActor.DownloadSuccess(votable)
      } catch {
        case e: Exception =>
          sender() ! VotableResolverActor.DownloadFailed(e)
      }
  }

}
