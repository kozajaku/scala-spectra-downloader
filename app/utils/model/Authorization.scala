package utils.model

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
  * Created by radiokoza on 4.6.16.
  */
case class Authorization(username: String, password: String){

  lazy val basicAuthorizationHeader: String = {
    val secret: String = Base64.getEncoder.encodeToString(s"$username:$password".getBytes(StandardCharsets.UTF_8))
    s"Basic $secret" //return
  }

}
