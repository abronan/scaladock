package com.scaladock.client

import scala.util.{Failure, Success, Try}
import scala.io._
import net.liftweb.json.{DefaultFormats, JsonParser}
import com.scaladock.client.util.PrettyPrinter
import scalaj.http.Base64
import net.liftweb.json.Serialization._

sealed case class RegistryConfig
(
  configs: Map[String, AuthToken])
  extends PrettyPrinter

sealed case class AuthToken
(
  auth: String,
  email: String)
  extends PrettyPrinter

sealed case class AuthConfig
(
  username: String,
  password: String,
  email: String,
  serveraddress: String)
  extends PrettyPrinter

sealed case class Author
(
  name: String,
  email: String)
  extends PrettyPrinter

/**
 * A Registry with Authentication configuration
 * The authentication file is in Json Format and of the form :
 *
 * == .dockercfg ==
 * {{{
 * {
 * "https://index.docker.io/v1/": {
 * "auth": "xXxXxXxXxXxX=",
 * "email": "email@example.com"
 * },
 * [...]
 * }
 * }}}
 *
 * To generate the file see {{{docker login}}} command
 *
 * @param indexURL
 * @param dockercfg
 */
class DockerRegistry
(
  val indexURL: String = "https://index.docker.io/v1/",
  val dockercfg: String = ".dockercfg",
  val rootPath: Option[String] = None)
  extends HttpHelper {

  implicit val formats = DefaultFormats

  val default = "https://index.docker.io/v1/"

  def authBase64: String = {
    val encoded = Base64.encode(buildAuth.get.getBytes("ASCII"))
    new String(encoded)
  }

  /**
   * Resolve Auth Configuration for a given registry
   */
  def AuthConfig: AuthToken = {
    val config: Map[String, AuthToken] = loadConfig match {
      case Success(c) => c
      case Failure(_) => null
    }

    val configList = RegistryConfig(configs = config)
    val authConfig = configList.configs.get(indexURL)

    authConfig.get
  }

  /**
   * Builds an AuthConfig object
   * @return
   */
  def buildAuth: Try[String] = Try {
    val decoded = Base64.decode(AuthConfig.auth)
    val auth = new String(decoded).split(":")

    val authConf = new AuthConfig(
      username = auth(0),
      password = auth(1),
      email = AuthConfig.email,
      serveraddress = indexURL
    )

    writePretty(authConf)
  }

  /**
   * Tries to load the authentication token for the registry
   * @return
   */
  def loadConfig: Try[Map[String, AuthToken]] = Try {
    JsonParser.parse(
      Source
        .fromFile(s"${rootPath.getOrElse(System.getenv("HOME"))}/$dockercfg")
        .getLines()
        .mkString
    ).extract[Map[String, AuthToken]]
  }

  // TODO Infer registry configuration
  def resolveConf: String = ""

  // TODO Resolve repository name
  def resolveRepositoryName: String = ""

  def pingRegistry: Boolean = {
    ping(s"$indexURL/", "_ping")
  }

}
