package com.scaladock

import com.scaladock.client._
import scala.util.{Failure, Success}
import java.io.File

object Main extends App {

  import net.liftweb.json.DefaultFormats
  import net.liftweb.json.Serialization._

  implicit val formats = DefaultFormats

  val dest = new DockerConnection("ec2-46-137-21-160.eu-west-1.compute.amazonaws.com", "4243")

  val conn = new DockerConnection("172.16.241.153", "4243")

  val list = conn.events(1391260348)
  list match {
    case Success(v) => v foreach println
    case Failure(e) => e.printStackTrace()
  }

  /*conn.events(1391260348) match {
    case Success(s) => s.foreach(println)
    case Failure(e) => e.printStackTrace()
  }*/

  //------------- Create Registry --------------
  // val registry = new DockerRegistry()

  //------------- List Changes --------------
  /*val res = conn.listContainers()
  for(
    list <- res;
    container <- list
  ) println(container.inspect.get.pretty)*/

  //------------- Container Creation without config --------------
  /*val simple = conn.createContainer()

  simple match {
    case Success(v) => for (c <- v.Config) println(c.pretty)
    case Failure(e) => e.printStackTrace()
  }*/


  //------------- Container Creation with config --------------
  /*val advanced = conn.createContainer(
    Some(CreationConfig(
      Tty = true,
      Cmd = Some(Array("/bin/bash", "-c", "while true; do echo Hello world >> /root/myfile; sleep 1; done"))
    ))
  )*/

  //------------- Start and Attach to the container --------------
  /*advanced match {
    case Success(c) => {
      // Start the container
      c.start()

      // Copy a file or directory from the container
      val data = c.copy("/root/myfile")
      data match {
        case Success(v) => {
          val in = scala.io.Source.fromRawBytes(v)
          val out = new java.io.PrintWriter(new File(System.getenv("HOME") + "/myfile"))
          try { in.getLines().foreach(out.print(_)) }
          finally { out.close }
        }
        case Failure(e) => e.printStackTrace()
      }

      // Create a new image from container changes
      // val newId = c.commit()

      // Attach to the container stdin, stdout, stderr
      // c.attach()
    }
    case Failure(e) => e.printStackTrace()
  }*/


  /*advanced match {
    case Success(c) => {
      println(c.start())
      println(c.inspect.get.pretty)
      println(c.changes)
      println(c.top())*/
      // c.attach
      /*println(c.stop())
      println(c.start())
      println(c.restart())
      println(c.kill)
    }
    case Failure(e) => e.printStackTrace()
  }*/


  //---------------- Container Inspect -----------------
  /*conn.events(1391260348) match {
    case Success(s) => for (x <- s) println(x.pretty)
    case Failure(e) => e.printStackTrace()
  }*/




  //------------- Create image --------------
  // val status = conn.createImage(fromImage = "debian")

  // status.onComplete {
  //   case Success(v) => (s"Got the Callback = ${v.pretty}")
  //   case Failure(e) => e.printStackTrace()
  // }




  //------------- Idea Migration --------------
  /*val cont = new Container("", conn)
  val ret = using(registry) { cont >> dest }*/

  /*for(
    list <- res;
    container <- list
  ) container.attach("ec2-46-137-21-160.eu-west-1.compute.amazonaws.com", container.Info.get.Id)*/


  /*val ima = conn.listImages()*/


  /*for(
    list <- ima;
    image <- list
  ) println(image.history.get)*/


  //------------- Load Config --------------
  /*val reg = new DockerRegistry()
  val config = reg.loadConfig
  println(config.get)*/




  /*// Example 1 = List of containers
  val client = new DockerClient("172.16.241.132")
  val list = client.listContainers()

  for (entry <- list) println(entry)

  // Example 2 = Create container
  val cont = new ContainerDesc(Image = "ubuntu", Cmd = Array[String]("/bin/bash"))
  val resp = client.createContainer(cont, name = "contTest").get

  println(resp)
  for (entry <- resp.Warnings) println(entry)

  // Example 3 = Export Container
  val bytes =  client.exportContainer("228a8e78507b", "/home/abronan/")
  println(bytes)*/

}
