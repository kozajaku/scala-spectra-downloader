package actors.test

import akka.actor.{Props, Actor}
import play.api.libs.json.JsValue

/**
  * Created by radiokoza on 5.6.16.
  */
object PrintingClientActor {
  def props = Props[PrintingClientActor]
}

class PrintingClientActor extends Actor {

  def receive = {
    case json: JsValue =>
      println(json.toString())
  }

}
