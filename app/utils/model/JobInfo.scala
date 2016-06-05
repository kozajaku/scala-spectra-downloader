package utils.model

import java.util.Date

import akka.actor.ActorRef
/**
  * Created by radiokoza on 5.6.16.
  */
case class JobInfo(downActor: ActorRef, votableSource: String, recordsCount: Int, directory: Directory, created: Date = new Date())
