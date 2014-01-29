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

sealed case class PushResponse
(
  status: String,
  progress: Option[String],
  progressDetail: Option[ProgressDetail])
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
class Image(id: String, connection: DockerConnection) extends HttpHelper {

  implicit val formats = DefaultFormats

  var Info = None: Option[ImageInfo]

  /**
   * Inspect for low level information on the container
   * @return
   */
  def inspect: Try[ImageInspect] = Try {
    JsonParser.parse(
      get(connection)(s"images/${Info.get.RepoTags(0)}/json")
    ).extract[ImageInspect]
  }

  /**
   * Get the History of a container
   * @return
   */
  def history: Try[Array[History]] = Try {
    JsonParser.parse(
      get(connection)(s"images/${Info.get.RepoTags(0)}/history")
    ).extract[Array[History]]
  }

  /**
   * Push a container onto a registry
   * @param registry
   * @return
   */
  def push(registry: DockerRegistry): Try[PushResponse] = Try {
    val params = Map("registry" -> registry.indexURL)
    JsonParser.parse(
      postAuth(connection)(s"images/${Info.get.RepoTags(0)}/push", Some(params), registry.authBase64)
    ).extract[PushResponse]
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
      postParseHeaders(connection)(s"images/${Info.get.RepoTags(0)}/push")

    responseCode
  }

  /**
   * Removes an image {{{name}}} from the filesystem
   * @return
   */
  def remove: Try[Array[Any]] = Try {
    JsonParser.parse(
      delete(connection)(s"images/${Info.get.RepoTags(0)}")
    ).extract[Array[Any]]
  }

}
