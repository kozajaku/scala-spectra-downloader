package actors

import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.net.{HttpURLConnection, URL}

import akka.actor.{Actor, Props}
import utils.model.{Authorization, Directory}

/**
  * Created by radiokoza on 4.6.16.
  */
object SingleDownloadActor {
  def props = Props[SingleDownloadActor]

  case class DownloadSpectrum(id: Int, url: String, suggestedName: String, directory: Directory, authorization: Option[Authorization])

  val extension = Map(
    "application/fits" -> "fits",
    "image/fits" -> "fit",
    "text/csv" -> "csv",
    "application/x-votable+xml;serialization=tabledata" -> "vot",
    "application/x-votable+xml" -> "vot",
    "text/plain" -> "txt",
    "application/xml" -> "xml"
  )
}

class SingleDownloadActor extends Actor {

  import SingleDownloadActor._

  def receive = {
    case DownloadSpectrum(id, url, name, directory, authorization) =>
      println(s"downloading $name - id: $id")
      try {
        val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
        authorization match {
          case Some(auth) =>
            connection.addRequestProperty("Authorization", auth.basicAuthorizationHeader)
          case None => //nothing to be done here
        }
        //20 sec connect and read timeout
        connection.setConnectTimeout(20000)
        connection.setReadTimeout(20000)
        val contentType = Option(connection.getContentType)
        val postfix = extension.get(contentType.getOrElse("")) match {
          case Some(t) => s".$t"
          case None => ""
        }
        val fileName = name + postfix
        val directoryFile = new File(directory.path)
        directoryFile.mkdirs()
        val outputFile = new File(directoryFile, fileName)
        if (!outputFile.createNewFile()) throw new Exception(s"File with name $fileName already exists")
        val output = new BufferedOutputStream(new FileOutputStream(outputFile))
        val byteArray = Stream.continually(connection.getInputStream.read).takeWhile(-1 != _).map(_.toByte).toArray
        output.write(byteArray)
        output.flush()
        output.close()
        sender() ! SpectraDownloadingActor.SpectrumDownloadSuccess(id)
      } catch {
        case e: Exception => sender() ! SpectraDownloadingActor.SpectrumDownloadFailed(id, e)
      }
  }

}
