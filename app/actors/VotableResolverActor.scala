package actors

import akka.actor.{PoisonPill, ActorRef, Actor, Props}
import utils.parser.model.IndexedSSAPVotable

/**
  * Created by radiokoza on 3.6.16.
  */
object VotableResolverActor {
  def props = Props[VotableResolverActor]

  trait ResolverResponse

  trait ResolverSuccess extends ResolverResponse

  trait ResolverFailed extends ResolverResponse

  case class VotableByDownload(url: String)

  case class VotableByUpload(votable: String)

  case class DownloadFailed(exception: Exception) extends ResolverFailed

  case class DownloadSuccess(votable: String)

  case class ParsingSuccess(votable: IndexedSSAPVotable) extends ResolverSuccess

  case object ParsingFailed extends ResolverFailed

}


class VotableResolverActor extends Actor {

  import VotableResolverActor._

  private var parent: Option[ActorRef] = _

  def receive = {
    case VotableByDownload(url) =>
      parent = Option(sender())
      context.actorOf(VotableDownloaderActor.props) ! VotableDownloaderActor.DownloadVotable(url)
    case VotableByUpload(votable) =>
      parent = Option(sender())
      context.actorOf(VotableParserActor.props) ! VotableParserActor.ParseVotable(votable)
    case DownloadFailed(exception) =>
      parent.get ! DownloadFailed(exception)
      sender() ! PoisonPill
    case DownloadSuccess(votable) =>
      context.actorOf(VotableParserActor.props) ! VotableParserActor.ParseVotable(votable)
      sender() ! PoisonPill
    case ParsingSuccess(votable) =>
      parent.get ! ParsingSuccess(votable)
      sender() ! PoisonPill
    case ParsingFailed =>
      parent.get ! ParsingFailed
      sender() ! PoisonPill
  }
}
