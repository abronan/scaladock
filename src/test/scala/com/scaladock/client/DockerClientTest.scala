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
  val tmpImages: ListBuffer[Image] = ListBuffer()

  before {
    client = new DockerConnection("localhost", "4243", 10000)
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

    // Kill all temporary images
    for (image <- tmpImages) {
      try {
        image.remove
      } catch {
        case ignore: Exception =>
      }
    }
  }

  /*-------------------------------------
             Information Tests
   -------------------------------------*/

  "Get docker informations" should "contain non null informations" in {
    val info = client.SystemInfo.success.value

    assert(info.Containers > 0)
    assert(info.Images > 0)
    assert(info.NFd > 0);
    assert(info.NGoroutines > 0)
    assert(info.MemoryLimit > 0)
  }

  "Search Image" should "return a list with the searched image inside" in {
    val array = client.searchImage("busybox").success.value
    assert(array.exists(v => v.name == "busybox"))
    array.find(v => v.name == "busybox").size shouldBe 1
  }

  /*-------------------------------------
              Listing Tests
   -------------------------------------*/

  "Calling for image list" should "return a valid list of images with their ID set in Info structure" in {
    val list = client.listImages().get
    for (entry <- list) {
      entry.Info should not be empty
      entry.Info match {
        case Some(v) => assert(!v.Id.isEmpty)
        case None =>
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

  /*-------------------------------------
              Container Tests
   -------------------------------------*/

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

    assertResult(inspect.State.Running) {
      true
    }
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

    assertResult(inspect.State.Running) {
      false
    }
    // assert(inspect.State.ExitCode != 0)
  }

  "Waiting for a container" should "block until the container stops and the exit code should be equal to 0" in {
    val container = client.createContainer().success.value
    tmpContainers += container
    container.start()

    val fut = future {
      container.Wait
    }

    container.stop()

    val inspect = container.inspect.success.value

    assertResult(inspect.State.Running) {
      false
    }
    val exitCode = inspect.State.ExitCode

    whenReady(fut) {
      exit => exit.success.value.StatusCode shouldBe exitCode
    }
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
    assertResult(inspectAfter.State.Running) {
      true
    }
  }

  "Kill a container" should "kill the container and no longer list the container as available" in {
    val container = client.createContainer().success.value
    tmpContainers += container
    container.start()
    container.kill
    val inspect = container.inspect.success.value

    assertResult(inspect.State.Running) {
      false
    }
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

  "Get changes of a container" should "return a list of added and modified files" in {
    val container = client.createContainer(
      Some(CreationConfig(
        Tty = true,
        Cmd = Some(Array("touch", "/newFile"))
      ))
    ).success.value
    container.start()
    tmpContainers += container

    val listDiff = container.changes.success.value
    assert(listDiff.size == 3)

    listDiff.foreach(
      x => {
        if(x.Path == "/newFile"){
          x.Kind shouldBe 1
          x.Path shouldBe "/newFile"
        }
      }
    )
  }

  "Commit a container" should "create a new image from container's changes" in {
    val container = client.createContainer(
      Some(CreationConfig(
        Tty = true,
        Cmd = Some(Array("touch", "/newFile"))
      ))
    ).success.value
    container.start()
    tmpContainers += container

    val image = container.commit().success.value
    tmpImages += image

    val inspectImage = image.inspect.success.value
    assert(inspectImage.container.get.startsWith(container.id))
    inspectImage.container_config.Image shouldBe "busybox"

    val parent = new Image(name = Some("busybox"), connection = client)
    val busybox = parent.inspect.success.value

    inspectImage.parent.get shouldBe busybox.id
  }

  /*-------------------------------------
               Images Tests
   -------------------------------------*/

}
