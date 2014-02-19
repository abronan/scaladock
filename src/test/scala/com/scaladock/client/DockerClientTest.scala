package com.scaladock.client

import org.scalatest._
import java.lang.Exception
import scala.collection.mutable.ListBuffer

/**
 * ScalaTest : scaladock client
 */
class DockerClientTest extends FlatSpec with BeforeAndAfter with TryValues with Matchers {

  var client: DockerConnection = _
  val tmpContainers: ListBuffer[Container] = ListBuffer()

  before {
    client = new DockerConnection("172.16.241.156", "4243")
  }

  after {
    // Kill all temporary created containers
    for (container <- tmpContainers) {
      try {
        container.stop()
        container.kill
        container.remove()
      } catch {
        case ignore: Exception =>
      }
    }
  }

  "Calling for container list" should "return a valid list of containers with their ID set in Info structure" in {
    val list = client.listContainers().get
    for (entry <- list) {
      entry.Info should not be empty
      entry.Info match {
        case Some(v) => assert(!v.Id.isEmpty)
        case None =>
      }
    }
  }

  "Create a container" should "return a container object with its ID set" in {
    val container = client.createContainer(
      Some(CreationConfig(
        Tty = true,
        Cmd = Some(Array("/bin/bash", "-c", "while true; do echo Hello world; sleep 1; done"))
      ))
    ).success.value
    tmpContainers += container
    assert(!container.id.isEmpty)
  }

  "Stopping a container" should "properly stop the container and running state of Inspect should be set to `false`" in {
    val container = client.createContainer().success.value
    tmpContainers += container
    container.start()
    container.stop()
    val inspect = container.inspect.success.value

    assertResult(inspect.State.Running) { false }
    assert(inspect.State.ExitCode != 0)
  }

}
