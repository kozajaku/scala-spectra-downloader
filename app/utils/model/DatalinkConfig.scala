package utils.model

import java.net.URLEncoder

/**
  * Created by radiokoza on 4.6.16.
  */
case class DatalinkConfig(configuration: List[(String, String)]){

  lazy val queryParams: String = {
    val enc: String => String = URLEncoder.encode(_,"ASCII")
    configuration.map{case (k, v) => enc(k) + '=' + enc(v)}.mkString("&")
  }

}
