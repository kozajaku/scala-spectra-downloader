package actors

import akka.actor.{ActorRef, Actor, Props}
import utils.parser.model.IndexedSSAPVotable

/**
  * Created by radiokoza on 3.6.16.
  */
object VotableResolverActor {
  def props = Props[VotableResolverActor]

  case class VotableByDownload(url: String)

  case class VotableByUpload(votable: String)

  case class DownloadFailed(exception: Exception)

  case class DownloadSuccess(votable: String)

  case class ParsingSuccess(votable: IndexedSSAPVotable)

  case object ParsingFailed

}


class VotableResolverActor extends Actor {

  import VotableResolverActor._

  var parent: Option[ActorRef] = _

  def receive = {
    case VotableByDownload(url) =>
      parent = Option(sender())
      context.actorOf(VotableDownloaderActor.props) ! VotableDownloaderActor.DownloadVotable(url)
    case VotableByUpload(votable) =>
      parent = Option(sender())
      context.actorOf(VotableParserActor.props) ! VotableParserActor.ParseVotable(votable)
    case DownloadFailed(exception) =>
    //TODO
    case DownloadSuccess(votable) =>
      context.actorOf(VotableParserActor.props) ! VotableParserActor.ParseVotable(votable)
    case ParsingSuccess(votable) =>
      parent.get ! ParsingSuccess(votable)
    case ParsingFailed =>
    //TODO
  }
}
