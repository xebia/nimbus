package nl.gideondk.nimbus.api

import java.security.{PrivateKey, Signature}
import java.time.Instant
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import nl.gideondk.nimbus.Connection.{AccessToken, OAuthResponse}
import nl.gideondk.nimbus.ConnectionSettings

import scala.concurrent.Future

trait OAuthApi extends ConnectionSettings {

  def getAccessToken(email: String, privateKey: PrivateKey, when: Instant)(implicit as: ActorSystem, materializer: Materializer): Future[AccessToken] = {
    import materializer.executionContext
    val expiresAt = when.getEpochSecond + 3600
    val request = buildAuthRequest(email, when.getEpochSecond, expiresAt, privateKey)

    val body = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + request
    val ct = ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`)
    val url: Uri = s"$googleAPIEndPoint/oauth2/v4/token"

    for {
      response <- Http().singleRequest(HttpRequest(HttpMethods.POST, url, entity = HttpEntity(ct, body)))
      result <- Unmarshal(response.entity).to[OAuthResponse]
    } yield {
      AccessToken(
        accessToken = result.access_token,
        expiresAt = expiresAt
      )
    }
  }

  def buildAuthRequest(clientEmail: String, currentTimeSecondsUTC: Long, expiresAt: Long, privateKey: PrivateKey): String = {
    def base64(s: Array[Byte]) =
      new String(Base64.getUrlEncoder.encode(s))

    val header = base64("""{"alg":"RS256","typ":"JWT"}""".getBytes("UTF-8"))
    val request =
      base64(
        s"""
           |{
           | "iss": "$clientEmail",
           | "scope": "https://www.googleapis.com/auth/pubsub",
           | "aud": "https://www.googleapis.com/oauth2/v4/token",
           | "exp": $expiresAt,
           | "iat": $currentTimeSecondsUTC
           |}
      """.stripMargin.getBytes("UTF-8"))

    val sign = Signature.getInstance("SHA256withRSA")
    sign.initSign(privateKey)
    sign.update(s"$header.$request".getBytes("UTF-8"))

    val signature = base64(sign.sign())

    s"$header.$request.$signature"
  }
}