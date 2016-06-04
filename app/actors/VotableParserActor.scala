package actors

import akka.actor.{Props, Actor}
import utils.parser.{UnparseableVotableException, VotableParser}
import utils.parser.model.IndexedSSAPVotable

/**
  * Created by radiokoza on 3.6.16.
  */
object VotableParserActor {
  def props = Props[VotableParserActor]

  case class ParseVotable(votable: String)

}

class VotableParserActor extends Actor {

  import VotableParserActor._

  def receive = {
    case ParseVotable(votable) =>
      try {
        val parsedVotable: IndexedSSAPVotable = VotableParser.parseVotable(votable)
        sender() ! VotableResolverActor.ParsingSuccess(parsedVotable)
      } catch {
        case e: UnparseableVotableException => sender() ! VotableResolverActor.ParsingFailed
      }
  }

}
