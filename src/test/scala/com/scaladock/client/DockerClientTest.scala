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
    client = new DockerConnection("172.16.241.159", "4243", 10000)
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

    container.stop()

    val inspect = container.inspect.success.value

    assertResult(inspect.State.Running) { false }
    inspect.State.ExitCode should (equal(0) or equal(255))

    whenReady(fut) { exit => exit.success.value.StatusCode should (equal(0) or equal(255)) }
  }

  "Restarting a container" should "restart the container, `StartedAt` should be set to a different value and `Running` state should be set to `true`" in {
    val container = client.createContainer().success.value
    tmpContainers += container
    container.start()

    val inspectBefore = container.inspect.success.value
    val startTime = inspectBefore.State.StartedAt

    container.restart()

    val inspectAfter = container.inspect.success.value
    val endTime = inspectAfter.State.StartedAt

    assert(startTime != endTime)
    assertResult(inspectAfter.State.Running) { true }
  }

  "Kill a container" should "kill the container and no longer list the container as available" in {
    val container = client.createContainer().success.value
    tmpContainers += container
    container.start()
    container.kill
    val inspect = container.inspect.success.value

    assertResult(inspect.State.Running) { false }
    assert(inspect.State.ExitCode != 0)
  }

  "Remove a container" should "remove the container and the container should no longer be listed" in {
    val container = client.createContainer().success.value
    val id = container.id
    tmpContainers += container
    container.start()
    container.stop()

    container.remove()

    val list = client.listContainers().get
    list.foreach(x => assert(x.id != id))
  }

}
