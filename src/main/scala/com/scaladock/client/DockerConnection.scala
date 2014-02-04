package com.scaladock.client

import scala.util.{Failure, Success, Try}
import scala.None
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import net.liftweb.json.{DefaultFormats, JsonParser}
import net.liftweb.json.Serialization._
import scala.util.Success
import scala.util.Failure
import com.scaladock.client.util.PrettyPrinter
import java.io.{InputStreamReader, BufferedReader}
import java.nio.{ByteOrder, CharBuffer}
import scala.collection.mutable.ArrayBuffer
import Stream._


sealed case class SystemInfo
(
  Containers: Int,
  Debug: Boolean,
  Driver: String,
  DriverStatus: Array[Array[String]],
  IPv4Forwarding: Boolean,
  Images: Int,
  IndexServerAddress: String,
  InitPath: String,
  InitSha1: String,
  KernelVersion: String,
  LXCVersion: String,
  MemoryLimit: Int,
  NEventsListener: Int,
  NFd: Int,
  NGoroutines: Int,
  SwapLimit: Boolean)
  extends PrettyPrinter

sealed case class DockerVersion
(
  Version: String,
  GitCommit: String,
  GoVersion: String)
  extends PrettyPrinter

sealed case class CreateContainerResponse
(
  Id: String,
  Warnings: Array[String])
  extends PrettyPrinter

sealed case class CreateImageResponse
(
  status: String,
  progress: Option[String],
  progressDetail: Option[ProgressDetail])
  extends PrettyPrinter

sealed case class ProgressDetail
(
  current: Int,
  total: Int)
  extends PrettyPrinter

sealed case class SearchImage
(
  description: String,
  is_official: Boolean,
  is_trusted: Boolean,
  name: String,
  star_count: Int)
  extends PrettyPrinter

sealed case class Event
(
  status: Option[String],
  id: Option[String],
  from: Option[String],
  time: Option[Long])
  extends PrettyPrinter


/**
 * Connection Trait
 */
trait Connection {

  def host: String

  def port: String

  def timeout: Int

  def default: String

  /**
   * Returns the URL whether it is the default URL or a custom one
   * @return
   */
  def URL: String =
    if (host == default) default else s"http://$host:$port"
}


/**
 * Provides a connection to a remote running Docker daemon.
 *
 * The connection is immutable and each container or image
 * has a unique DockerConnection assigned
 *
 */
class DockerConnection
(
  override val host: String = "unix:///var/run/docker.sock/v1.8",
  override val port: String = "4243",
  override val timeout: Int = 10000)
  extends Connection with HttpHelper {

  implicit val formats = DefaultFormats

  override val default = "unix:///var/run/docker.sock/v1.8"

  /**
   * Shortens the Id of the container for future API calls
   * @param id
   * @return
   */
  def shortenId(id: String): String = id.substring(0, 12)

  /**
   * Get version informations about the running Docker daemon
   * @return
   */
  def Version: Try[DockerVersion] = Try {
    JsonParser.parse(
      get(this)("version")
    ).extract[DockerVersion]
  }

  /**
   * System-wide informations
   * @return
   */
  def SystemInfo: Try[SystemInfo] = Try {
    JsonParser.parse(
      get(this)("info")
    ).extract[SystemInfo]
  }

  /**
   * Validates if the client is compatible with the docker
   * daemon running on the remote machine
   * @return Try[String]
   */
  def validate(): Try[String] = Try {
    Version match {
      case Success(version) => {
        if (version.Version == "0.7.6") {
          "Version match"
        } else {
          "Version mismatch"
        }
      }
      case Failure(err) => "Can't get version"
    }
  }

  /**
   * Creates a new Container with the given optional configuration
   * @return Try[Container]
   */
  def createContainer(configuration: Option[CreationConfig] = Some(CreationConfig())): Try[Container] = Try {
    val json = configuration match {
      case Some(_) => write(configuration)
      case None => ""
    }

    val resp = JsonParser.parse(
      postJson(this)("containers/create", Some(json))
    ).extract[CreateContainerResponse]

    new Container(resp.Id, this) {
      Config = configuration
    }
  }

  /**
   * GET the List of containers on the remote server
   * @param all
   * @param limit
   * @param since
   * @param before
   * @param size
   * @return List[Container]
   */
  def listContainers(all: Boolean = true, limit: Int = 0, since: String = "",
                     before: String = "", size: Boolean = true): Try[List[Container]] = Try {

    var params = Map[String, String]()
    if (all == false) params += ("all" -> "false")
    if (limit != 0) params += ("limit" -> limit.toString)
    if (since != "") params += ("since" -> since)
    if (before != "") params += ("before" -> before)
    if (size == false) params += ("size" -> "false")

    val res = JsonParser.parse(
      get(this)("containers/json", Some(params))
    )

    val list = for (entry <- res.children) yield entry.extract[Info]
    for (
      info <- list
    ) yield new Container(shortenId(info.Id), this) {
      Info = Some(info)
    }
  }

  /**
   * Creates a new Image from a base Image
   * @param fromImage
   * @param fromSrc
   * @param repo
   * @param tag
   * @param registry
   * @return Future[CreateImageResponse]
   */
  def createImage(fromImage: String = "base", fromSrc: String = "", repo: String = "",
                  tag: String = "", registry: Option[DockerRegistry] = None): Future[CreateImageResponse] = Future {

    var params = Map[String, String]()
    params += ("fromImage" -> fromImage)
    if (fromSrc != "") params += ("fromSrc" -> fromSrc)
    if (repo != "") params += ("repo" -> repo)
    if (tag != "") params += ("tag" -> tag)

    // If registry isn't set then we use the default index
    registry match {
      case Some(_) => {
        params += ("registry" -> registry.get.indexURL)
        JsonParser.parse(
          postAuth(this)("images/create", Some(params), registry.get.authBase64)
        ).extract[CreateImageResponse]
      }
      case None => {
        JsonParser.parse(
          post(this)("images/create", Some(params))
        ).extract[CreateImageResponse]
      }
    }
  }

  /**
   * GET the List of images on the remote server
   * @param all
   * @param limit
   * @return List[Container]
   */
  def listImages(all: Boolean = true, limit: Int = 0): Try[List[Image]] = Try {
    var params = Map[String, String]()
    if (all == false) params += ("all" -> "false")
    if (limit != 0) params += ("limit" -> limit.toString)

    val res = JsonParser.parse(
      get(this)("images/json", Some(params))
    )

    val list = for (entry <- res.children) yield entry.extract[ImageInfo]

    for (info <- list) yield new Image(shortenId(info.Id), this) {
      Info = Some(info)
    }
  }

  /**
   * GET Search for an image in the docker index
   * TODO Test
   * @param name
   * @return
   */
  def searchImage(name: String): Try[Array[SearchImage]] = Try {
    val params = Map("name" -> name)
    JsonParser.parse(
      get(this)("images/json", Some(params))
    ).extract[Array[SearchImage]]
  }

  /**
   * Monitors events since the given timestamp
   * @param since
   * @return
   */
  def events(since: Long): Try[List[Event]] = Try {
    val params = Map("since" -> since.toString)
    val (_, _, list) = getStreamAndClose(this)(s"events", Some(params))
    list map (x => JsonParser.parse(x).extract[Event])
  }

}