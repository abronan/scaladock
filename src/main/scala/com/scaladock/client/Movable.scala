package com.scaladock.client

import scala.util.{Try, Success, Failure}

/** Movable is a Trait defining the capability to move a container
  * between Docker Hosts. [[com.scaladock.client.Container]] mixes
  * the Trait to enable this migration.
  *
  * ==Overview==
  * With a container, simply calls the >> (shift) method which copies
  * the container onto the host passed as a parameter
  * {{{
  * scala> val base = new DockerConnection("172.16.241.132")
  * scala> val dest = new DockerConnection("172.16.241.133")
  * scala> val registry = new DockerRegistry()
  * scala> val cont = base.create()
  * scala> using(registry) { cont >> dest }
  * }}}
  *
  * You could then use the new container as any other container
  * {{{
  * scala> val list = new.info
  * }}}
  *
  */
trait Movable {

  /**
   * Exports the container using [[com.scaladock.client.Container.exportContainer()]]
   * on the original host and calling [[com.scaladock.client.DockerConnection.importContainer()]]
   * to move it onto the destination host
   *
   * using(registry) { container1 >> destHost }
   *
   * @param dest
   * @return
   */
  def >>(dest: DockerConnection): Try[String] = Try {

    // TODO migration
    // Commit an image from the container changes
    // Push image onto registry
    // Pull the image on the destination connection
    // Step 1 : Call export() on the container
    // Step 2 : Call import() on the connection
    // Step 3 : Return new container instance (with a new Id) bound with the destination connection
    // return container
    ""
  }

}
