package com.scaladock.client

import scala.util.{Failure, Success, Try}
import net.liftweb.json.Serialization._
import net.liftweb.json.{JsonParser, DefaultFormats}
import com.scaladock.client.util.PrettyPrinter
import io.backchat.hookup._

import java.net.URI
import akka.actor.{ActorSystem}
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicInteger
import java.io.File
import scala.concurrent.Future


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
  SizeRw: Option[Int],
  SizeRootFs: Option[Int],
  Names: Array[String])
  extends PrettyPrinter

sealed case class Port
(
  PrivatePort: Option[Long] = None,
  PublicPort: Option[Long] = None,
  Type: Option[String] = None,
  IP: Option[String] = None)
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

sealed case class ContainerId
(
  Id: String)
  extends PrettyPrinter

sealed case class Resource
(
  Resource: String)
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

  var AttachInterface = None: Option[HookupClient]

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
   * Removes the container
   * @param removeVolume
   * @return
   */
  def remove(removeVolume: Boolean = false) = Try {
    val params = Map("v" -> removeVolume.toString)
    val (responseCode, _, _) = postParseHeaders(connection)(s"containers/$id/remove", Some(params))
    responseCode match {
      case 204 => Success("Container Removed")
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
      post(connection)(s"containers/$id/wait")
    ).extract[WaitStatus]
  }

  /**
   * Exports the container in tar format
   * TODO
   * @return
   */
  def export = Try {
    get(connection)(s"containers/$id/export")
  }

  /**
   * Copy a file or a directory content from the container
   * @param resource
   * @return
   */
  def copy(resource: String): Try[Array[Byte]] = Try {
    val json = write(Resource(resource))
    postJsonGetStream(connection)(s"containers/$id/copy", Some(json))
  }

  /**
   * Attach to container stdout through websocket
   */
  def attach(logfile: Option[String] = None) = Try {
    logfile match {
      case Some(v) =>
        AttachInterface = Some(
          ContainerAttach.makeClient(id, connection.host, connection.port, logfile)
        )
      case None => AttachInterface = Some(
        ContainerAttach.makeClient(id, connection.host, connection.port, None)
      )
    }
  }

  /**
   * Detach from the container
   */
  def detach = Try {
    AttachInterface match {
      case Some(v) => v.disconnect()
      case None => // ignore
    }
  }

  /**
   * Create a new Image from container changes
   * @param repository
   * @param message
   * @param tag
   * @param author
   * @param run
   * @return
   */
  def commit(repository: Option[String] = None, message: Option[String] = None, tag: Option[String] = None,
             author: Option[Author] = None, run: Option[CreationConfig] = None) = Try {

    var params = Map[String, String]()
    params += ("container" -> id)
    repository match {
      case Some(v) => params += ("repo" -> v)
      case None =>
    }
    tag match {
      case Some(v) => params += ("tag" -> v)
      case None =>
    }
    message match {
      case Some(v) => params += ("m" -> v)
      case None =>
    }
    author match {
      case Some(v) => params += ("author" -> s"${v.name} <${v.email}>")
      case None =>
    }
    val json: Option[String] = run match {
      case Some(v) => Some(write(v))
      case None => None
    }

    val response = postJson(connection)("commit", json, Some(params))
    JsonParser.parse(response).extract[ContainerId]
  }

}

object ContainerAttach {

  val messageCounter = new AtomicInteger(0)

  implicit def stringToTextMessage(s: String) = TextMessage(s)

  val system = ActorSystem("ContainerAttach")

  /**
   * Websocket client to attach to a container
   * @param id
   * @param host
   * @param port
   * @param logfile
   * @param stream
   * @param stderr
   * @param stdout
   * @return HookupClient
   */
  def makeClient(id: String, host: String, port: String, logfile: Option[String], stream: Boolean = true,
                 stderr: Boolean = true, stdout: Boolean = true): HookupClient = {

    val logs = new File(s"${System.getenv("HOME")}/${id}.log")

    new HookupClient() {

      val uri = new URI(s"ws://${host}:${port}/containers/$id/attach/ws?stream=$stream&stderr=$stderr&stdout=$stdout")

      val settings: HookupClientConfig = HookupClientConfig(
        uri = uri,
        throttle = IndefiniteThrottle(5 seconds, 30 minutes),
        buffer = Some(new FileBuffer(new File(logfile.getOrElse(System.getenv("HOME") + "/buffer.log"))))
      )

      def receive = {
        case Disconnected(_) => {
          println("The websocket to " + uri.toASCIIString + " disconnected.")
          system.shutdown()
        }
        case TextMessage(message) => {
          println(message)
          scala.tools.nsc.io.File(logs).appendAll(message)
        }
      }

      connect() onSuccess {
        case _ =>
          println(s"Attached to $id at %s" format uri.toASCIIString)
          system.scheduler.schedule(0 seconds, 1 second) {
            send(s"message " + messageCounter.incrementAndGet().toString)
          }
      }
    }
  }
}
