package com.scaladock.client

import scala.util.{Failure, Success, Try}
import com.scaladock.client.util.PrettyPrinter
import net.liftweb.json.{DefaultFormats, JsonParser}
import scalaj.http.Base64
import java.nio.charset.Charset

sealed case class ImageInfo
(
  RepoTags: Array[String],
  Id: String,
  Created: Int,
  Size: Int,
  VirtualSize: Int)
  extends PrettyPrinter

sealed case class ImageInspect
(
  id: String,
  parent: Option[String],
  created: Option[String],
  container: Option[String],
  container_config: ContainerConfig,
  Size: Long)
  extends PrettyPrinter

sealed case class History
(
  Id: String,
  Tags: Option[Array[String]],
  Created: String,
  CreatedBy: Option[String],
  Size: Int)
  extends PrettyPrinter

sealed case class Progress
(
  status: Option[String],
  progress: Option[String],
  progressDetail: Option[ProgressDetail],
  error: Option[String])
  extends PrettyPrinter

sealed case class Untagged
(
  Untagged: String)
  extends PrettyPrinter

sealed case class Deleted
(
  Deleted: String)
  extends PrettyPrinter


/**
 * Created by abronan on 20/01/14.
 */
class Image(val id: Option[String] = None, val name: Option[String] = None, val connection: DockerConnection) extends HttpHelper {

  implicit val formats = DefaultFormats

  var Info = None: Option[ImageInfo]

  /**
   * Inspect for low level information on the container
   * @return
   */
  def inspect: Try[ImageInspect] = Try {
    JsonParser.parse(
      get(connection)(s"images/${id.getOrElse(name.get)}/json")
    ).extract[ImageInspect]
  }

  /**
   * Get the History of a container
   * @return
   */
  def history: Try[Array[History]] = Try {
    // TODO Handle Special case of containers that are <none>:<none>
    JsonParser.parse(
      get(connection)(s"images/${id.getOrElse(name.get)}/history")
    ).extract[Array[History]]
  }

  /**
   * Push a container onto a registry
   * @param registry
   * @return
   */
  def push(registry: DockerRegistry): Try[List[Progress]] = Try {
    val params = Map("registry" -> registry.indexURL)
    println(new String(Base64.decode(registry.authBase64)))
    val response =
      postAuth(connection)(s"images/${id.getOrElse(name.get)}/push", Some(params), registry.authBase64)
    println(response)
    val list = response.split("(?<=[}?!])").toList
    list map (x => JsonParser.parse(x).extract[Progress])
  }

  /**
   * Insert a file in the specified path into a container
   * @param path
   * @param url
   * @return
   */
  def insert(path: String = "/usr", url: String = ""): Try[Progress] = Try {
    val params = Map("path" -> path, "url" -> url)
    JsonParser.parse(
      post(connection)(s"images/${id.getOrElse(name.get)}/insert", Some(params))
    ).extract[Progress]
  }

  /**
   * Tags an image {{{name}}} into a repository
   * @param registry
   * @param force
   * @return
   */
  def tag(registry: DockerRegistry, force: Boolean = false): Int = {
    var params = Map[String, String]()
    params += ("registry" -> registry.indexURL)
    if (force != false) params += ("force" -> "true")

    val (responseCode, _, _) =
      postParseHeaders(connection)(s"images/${id.getOrElse(name.get)}/push")

    responseCode
  }

  /**
   * Removes an image {{{name}}} from the filesystem
   * @return
   */
  def remove: Try[Array[Any]] = Try {
    val (_, _, response) = delete(connection)(s"images/${id.getOrElse(name.get)}")
    JsonParser.parse(
      response
    ).extract[Array[Any]]
  }

}
