package com.scaladock.client

import org.scalatest._
import java.lang.Exception
import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.ScalaFutures

/**
 * ScalaTest : scaladock client
 */
class DockerClientTest extends FlatSpec with BeforeAndAfter with TryValues with ScalaFutures with Matchers {

  var client: DockerConnection = _
  val tmpContainers: ListBuffer[Container] = ListBuffer()

  before {
    client = new DockerConnection("172.16.241.156", "4243", 10000)
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

  "Starting a container" should "start the container and the running state of Inspect should be set to `true`" in {
    val container = client.createContainer().success.value
    tmpContainers += container
    container.start()
    val inspect = container.inspect.success.value

    assertResult(inspect.State.Running) { true }
    if (!inspect.State.Running) {
      assert(inspect.State.ExitCode == 1)
    }
  }

  "Stopping a container" should "stop the container and the running state of Inspect should be set to `false`" in {
    val container = client.createContainer().success.value
    tmpContainers += container
    container.start()
    container.stop()
    val inspect = container.inspect.success.value

    assertResult(inspect.State.Running) { false }
    // assert(inspect.State.ExitCode != 0)
  }

  "Waiting for a container" should "block until the container stops and the exit code should be equal to 0" in {
    val container = client.createContainer().success.value
    tmpContainers += container
    container.start()

    val fut = future { container.Wait }
    whenReady(fut) { exit => exit.success.value.StatusCode should (equal(0) or equal(255)) }

    container.stop()

    val inspect = container.inspect.success.value

    assertResult(inspect.State.Running) { false }
    inspect.State.ExitCode should (equal(0) or equal(255))
  }


}
