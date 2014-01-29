package com.scaladock.client

import scala.util.{Failure, Success, Try}
import net.liftweb.json.Serialization._
import net.liftweb.json.{JsonParser, DefaultFormats}
import com.scaladock.client.util.PrettyPrinter


sealed case class CreationConfig
(
  Hostname: String = "",
  User: String = "",
  Memory: Int = 0,
  MemorySwap: Int = 0,
  AttachStdin: Boolean = false,
  AttachStdout: Boolean = true,
  AttachStderr: Boolean = true,
  PortSpecs: Option[Array[String]] = None,
  Tty: Boolean = false,
  OpenStdin: Boolean = false,
  StdinOnce: Boolean = false,
  Env: Option[Array[String]] = None,
  Cmd: Option[Array[String]] = None,
  Dns: Option[Array[String]] = None,
  Image: String = "ubuntu",
  Volumes: Option[Map[String, Array[String]]] = None,
  VolumesFrom: String = "",
  WorkingDir: String = "",
  ExposedPorts: Option[Map[String, String]] = None)
  extends PrettyPrinter

sealed case class ContainerConfig
(
  Hostname: String,
  User: String,
  Memory: Int,
  MemorySwap: Int,
  CpuShares: Int,
  AttachStdin: Boolean,
  AttachStdout: Boolean,
  AttachStderr: Boolean,
  PortSpecs: Array[String],
  ExposedPorts: Option[Map[String, String]],
  Tty: Boolean,
  OpenStdin: Boolean,
  StdinOnce: Boolean,
  Env: Array[String],
  Cmd: Array[String],
  Dns: Array[String],
  Image: String,
  Volumes: Option[Map[String, Array[String]]],
  VolumesFrom: String,
  WorkingDir: String,
  EntryPoint: Array[String],
  NetworkDisabled: Boolean)
  extends PrettyPrinter

sealed case class Info
(
  Id: String,
  Image: String,
  Command: String,
  Created: Long,
  Status: String,
  Ports: Port,
  SizeRw: Int,
  SizeRootFs: Int,
  Names: Array[String])
  extends PrettyPrinter

sealed case class Port
(
  PrivatePort: Long,
  PublicPort: Long,
  PortType: String)
  extends PrettyPrinter

sealed case class Start
(
  Binds: Array[String],
  LxcConf: Map[String, String],
  PortBindings: Map[String, Array[HostBinding]],
  PublishAllPorts: Boolean,
  Privileged: Boolean)
  extends PrettyPrinter

sealed case class HostBinding
(
  HostIp: String,
  HostPort: String)
  extends PrettyPrinter

sealed case class ContainerInspect
(
  ID: String,
  Created: String,
  Path: String,
  Args: Array[String],
  Config: ContainerConfig,
  State: State,
  Image: String,
  NetworkSettings: NetworkSettings,
  ResolvConfPath: String,
  HostnamePath: String,
  HostsPath: String,
  Name: String,
  Driver: String,
  Volumes: Map[String, String],
  VolumesRW: Map[String, Boolean],
  HostConfig: HostConfig)
  extends PrettyPrinter

sealed case class State
(
  Running: Boolean,
  Pid: Int,
  ExitCode: Int,
  StartedAt: String,
  Ghost: Boolean)
  extends PrettyPrinter

sealed case class NetworkSettings
(
  IPAddress: String,
  IPPrefixLen: Int,
  Gateway: String,
  Bridge: String,
  PortMapping: String)
  extends PrettyPrinter

sealed case class HostConfig
(
  Binds: Array[String],
  ContainerIDFile: String,
  LxcConf: Array[KeyValuePair],
  Privileged: Boolean,
  PortBindings: Map[Port, Array[PortBinding]],
  Links: Array[String],
  PublishAllPorts: Boolean)
  extends PrettyPrinter

sealed case class KeyValuePair
(
  Key: String,
  Value: String)

sealed case class PortBinding
(
  HostIp: String,
  HostPort: String)

sealed case class ProcessList
(
  Titles: Array[String],
  Processes: Array[Array[String]])
  extends PrettyPrinter

// TODO Implement Pretty Print on Root Level object
sealed case class Changes
(
  Path: String,
  Kind: Int)
  extends PrettyPrinter


/**
 * A Container in a Docker instance
 * @param id
 * @param connection
 */
class Container(val id: String, val connection: DockerConnection) extends Movable with HttpHelper {

  implicit val formats = DefaultFormats

  var Config = None: Option[ContainerConfig]

  var Info = None: Option[Info]

  /**
   * Starts the container
   * @return
   */
  def start(config: Option[Start] = None) {
    postJson(connection)(s"containers/$id/start", Some(write(config.getOrElse(""))))
  }

  /**
   * Stops the running container after t time
   * default: 5 seconds
   */
  def stop(timeout: Int = 5000) {
    val params = Map("t" -> timeout.toString)
    post(connection)(s"containers/$id/stop", Some(params))
  }

  /**
   * Restarts a container after t time
   * default: 5 seconds
   */
  def restart(timeout: Int = 5000) {
    val params = Map("t" -> timeout.toString)
    post(connection)(s"containers/$id/restart", Some(params))
  }

  /**
   * Immediatly kills a running container
   * Call if [[com.scaladock.client.Container.stop( )]] does not work
   */
  def kill {
    // TODO Handle response code
    val (responseCode, headers, inputStream) =
      postParseHeaders(connection)(s"containers/$id/kill")
  }

  /**
   * Inspect for low level information on the container
   * @return
   */
  def inspect: Try[ContainerInspect] = Try {
    JsonParser.parse(
      get(connection)(s"containers/$id/json")
    ).extract[ContainerInspect]
  }

  /**
   * Inspect changes on the container filesystem
   * @return
   */
  def changes: Try[Array[Changes]] = Try {
    JsonParser.parse(
      get(connection)(s"containers/$id/changes")
    ).extract[Array[Changes]]
  }

  /**
   * List processes running inside a container
   * @return
   */
  def top(args: Option[String] = None): Try[ProcessList] = Try {
    args match {
      case Some(_) => {
        val params = Map("ps_args" -> args.get)
        JsonParser.parse(
          get(connection)(s"containers/$id/top", Some(params))
        ).extract[ProcessList]
      }
      case None => {
        JsonParser.parse(
          get(connection)(s"containers/$id/top")
        ).extract[ProcessList]
      }
    }
  }

  /**
   * Exports the container in tar format
   * @return
   */
  def export {
    val binary = get(connection)(s"containers/$id/export")
  }

}

object Container {

  /*val container = connection create
  with Config(
    User = "abronan"
  )*/
}
