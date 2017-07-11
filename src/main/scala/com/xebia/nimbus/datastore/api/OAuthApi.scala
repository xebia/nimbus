package com.xebia.nimbus.datastore.api

import java.io.File
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, PrivateKey, Signature}
import java.time.Instant
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream._
import akka.stream.stage._
import com.xebia.nimbus.Connection.AccessToken
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.{Future, Promise}

object OAuthApi extends DefaultJsonProtocol {
  val apiHost = "www.googleapis.com"
  val googleAPIEndPoint = s"https://$apiHost"

  private final case class OAuthResponse(access_token: String, token_type: String, expires_in: Int)

  private implicit val oAuthResponseFormat = jsonFormat3(OAuthResponse.apply)

  private implicit val accessTokenFormatFormat = jsonFormat2(AccessToken.apply)

  def getAccessToken(email: String, privateKey: PrivateKey, fromWhen: Instant)(implicit as: ActorSystem, materializer: Materializer): Future[AccessToken] = {
    import materializer.executionContext
    val expiresAt = fromWhen.getEpochSecond + 3600
    val request = buildAuthRequest(email, fromWhen.getEpochSecond, expiresAt, privateKey)

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
           | "scope": "https://www.googleapis.com/auth/datastore",
           | "aud": "https://www.googleapis.com/oauth2/v4/token",
           | "exp": $expiresAt,
           | "iat": $currentTimeSecondsUTC
           |}
      """.stripMargin.getBytes("UTF-8")
      )

    val sign = Signature.getInstance("SHA256withRSA")
    sign.initSign(privateKey)
    sign.update(s"$header.$request".getBytes("UTF-8"))

    val signature = base64(sign.sign())

    s"$header.$request.$signature"
  }

  private case class KeyInformation(`type`: String, project_id: String, private_key_id: String, private_key: String, client_email: String, client_id: String, auth_uri: String, token_uri: String, auth_provider_x509_cert_url: String, client_x509_cert_url: String)

  private implicit val keyInformationFormat = jsonFormat10(KeyInformation.apply)

  case class Credentials(email: String, privateKey: PrivateKey)

  def readCredentialsFromFile(file: File): Credentials = {
    val keyInformation = scala.io.Source.fromFile(file, "UTF-8").getLines().mkString.parseJson.convertTo[KeyInformation]
    val kf = KeyFactory.getInstance("RSA")
    val trimmed = keyInformation.private_key.replace("\\n", "").replace("\n", "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
    val encodedPv = Base64.getDecoder.decode(trimmed)
    val keySpecPv = new PKCS8EncodedKeySpec(encodedPv)
    Credentials(keyInformation.client_email, kf.generatePrivate(keySpecPv))
  }

  def readCredentialsFromEnvironment() = {
    val path = sys.env.get("GOOGLE_APPLICATION_CREDENTIALS").getOrElse("Environment variable : 'GOOGLE_APPLICATION_CREDENTIALS' wasn't set")
    readCredentialsFromFile(new File(path))
  }

  class AccessTokenStage(f: () => Future[AccessToken]) extends GraphStage[FlowShape[(HttpRequest, Promise[HttpResponse]), Future[(HttpRequest, Promise[HttpResponse])]]] {

    private val in = Inlet[(HttpRequest, Promise[HttpResponse])]("AccessTokenStage.In")
    private val out = Outlet[Future[(HttpRequest, Promise[HttpResponse])]]("AccessTokenStage.Out")

    val shape = new FlowShape(in, out)

    override def createLogic(attr: Attributes): GraphStageLogic = {
      new GraphStageLogic(shape) {
        implicit lazy val ec = materializer.executionContext

        var accessToken = Promise[AccessToken]()

        def now() = Instant.now()

        def expiresSoon(g: AccessToken) =
          g.expiresAt < (now.getEpochSecond + 60)

        def getNewToken() = {
          println("Requesting new one")
          accessToken = Promise[AccessToken]()
          accessToken.completeWith(f())
          accessToken.future.onFailure { case e =>
            failStage(throw new IllegalStateException(s"Couldn't request `AccessToken`, not able to exchange requests without authentication", e))
          }
          accessToken.future
        }

        def withAccessToken(h: HttpRequest, token: AccessToken) = {
          h.addCredentials(OAuth2BearerToken(token.accessToken))
        }

        val defaultInHandler = new InHandler {
          override def onPush(): Unit = {
            if (!accessToken.future.isCompleted || expiresSoon(accessToken.future.value.get.get)) {
              val grabbed = grab(in)
              push(out, getNewToken().map(x => (withAccessToken(grabbed._1, x) -> grabbed._2)))
            } else {
              println("Recycling old one")
              val grabbed = grab(in)
              val token = accessToken.future.value.get.get // Yikes
              push(out, Future(withAccessToken(grabbed._1, token) -> grabbed._2))
            }
          }
        }
        val defaultOutHandler = new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        }
        setHandler(in, defaultInHandler)
        setHandler(out, defaultOutHandler)
      }
    }
  }

}