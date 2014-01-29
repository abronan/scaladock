package com.scaladock.client

import scala.util.{Failure, Success, Try}
import scala.io._
import net.liftweb.json.{DefaultFormats, JsonParser}
import com.scaladock.client.util.PrettyPrinter
import scalaj.http.Base64
import java.nio.charset.Charset

sealed case class RegistryConfig
(
  configs: Map[String, AuthConfig])
  extends PrettyPrinter

sealed case class AuthConfig
(
  auth: String,
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
  val dockercfg: String = ".dockercfg")
  extends HttpHelper {

  implicit val formats = DefaultFormats

  val default = "https://index.docker.io/v1/"

  /**
   * Resolve Auth Configuration for a given registry
   */
  def AuthConfig: Try[AuthConfig] = Try {

    val configList = RegistryConfig(configs = loadConfig().get)
    val authConfig = configList.configs.get(indexURL)

    authConfig.get
  }

  /**
   * Tries to load the authentication configuration for the registry
   * @param root
   * @return
   */
  def loadConfig(root: Option[String] = None): Try[Map[String, AuthConfig]] = Try {
    JsonParser.parse(
      Source
        .fromFile(s"${root.getOrElse(System.getenv("HOME"))}/$dockercfg")
        .getLines()
        .mkString
    ).extract[Map[String, AuthConfig]]
  }

  def authBase64: String = {
    Base64.encode(AuthConfig.get.auth.getBytes(Charset.forName("ASCII"))).toString
  }

  // TODO Infer registry configuration
  def resolveConf: String = ""

  // TODO Resolve repository name
  def resolveRepositoryName: String = ""


  def pingRegistry: Boolean = {
    ping(s"$indexURL/", "_ping")
  }


}
