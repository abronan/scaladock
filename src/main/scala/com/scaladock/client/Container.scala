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
  Cmd: Option[Array[String]] = Some(Array("/bin/bash")),
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
  Type: String,
  IP: String)
  extends PrettyPrinter

sealed case class Start
(
  Binds: Option[Array[String]] = None,
  LxcConf: Option[Map[String, String]] = None,
  PortBindings: Option[Map[String, Array[HostBinding]]] = None,
  PublishAllPorts: Boolean = false,
  Links: Option[Map[String, Array[String]]] = None,
  Privileged: Boolean = false)
  extends PrettyPrinter

sealed case class HostBinding
(
  HostIp: Option[String] = None,
  HostPort: Option[String] = None)
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
  Volumes: Option[Map[String, String]],
  VolumesRW: Option[Map[String, Boolean]],
  HostConfig: Option[HostConfig])
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
  extends PrettyPrinter

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

sealed case class WaitStatus
(
  StatusCode: Int)
  extends PrettyPrinter


/**
 * A Container in a Docker instance
 * @param id
 * @param connection
 */
class Container(val id: String, val connection: DockerConnection) extends Movable with HttpHelper {

  implicit val formats = DefaultFormats

  var Config = None: Option[CreationConfig]

  var Info = None: Option[Info]

  /**
   * Starts the container
   * @return
   */
  def start(config: Option[Start] = None) = Try {
    val (responseCode, _, _) = config match {
      case Some(c) => postJsonWithHeaders(connection)(s"containers/$id/start", Some(write(c)))
      case None => postJsonWithHeaders(connection)(s"containers/$id/start", Some(write(Start())))
    }
    responseCode match {
      case 204 => Success("Container Started")
      case 404 => Failure(new Throwable("404 - no such container"))
      case 500 => Failure(new Throwable("500 - internal server error"))
    }
  }

  /**
   * Stops the running container after t time
   * default: 5 seconds
   */
  def stop(wait: Int = 5000) = Try {
    val params = Map("t" -> wait.toString)
    val (responseCode, _, _) = postParseHeaders(connection)(s"containers/$id/stop", Some(params))
    responseCode match {
      case 204 => Success("Container Stopped")
      case 404 => Failure(new Throwable("404 - no such container"))
      case 500 => Failure(new Throwable("500 - internal server error"))
    }
  }

  /**
   * Restarts a container after t time
   * default: 5 seconds
   */
  def restart(wait: Int = 5000) = Try {
    val params = Map("t" -> wait.toString)
    val (responseCode, _, _) = postParseHeaders(connection)(s"containers/$id/restart", Some(params))
    responseCode match {
      case 204 => Success("Container Restarted")
      case 404 => Failure(new Throwable("404 - no such container"))
      case 500 => Failure(new Throwable("500 - internal server error"))
    }
  }

  /**
   * Immediatly kills a running container
   * Call if [[com.scaladock.client.Container.stop( )]] does not work
   */
  def kill = Try {
    val (responseCode, _, _) = postParseHeaders(connection)(s"containers/$id/kill")
    responseCode match {
      case 204 => Success("Container Killed")
      case 404 => Failure(new Throwable("404 - no such container"))
      case 500 => Failure(new Throwable("500 - internal server error"))
    }
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
   * Waits for a container to stop and return its status
   * @return
   */
  def Wait: Try[WaitStatus] = Try {
    JsonParser.parse(
      get(connection)(s"containers/$id/wait")
    ).extract[WaitStatus]
  }

  /**
   * Exports the container in tar format
   * @return
   */
  def export {
    val binary = get(connection)(s"containers/$id/export")
  }

}
