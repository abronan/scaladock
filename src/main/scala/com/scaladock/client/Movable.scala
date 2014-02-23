package com.scaladock.client

import scala.util.{Try}

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
   * Moves a container from a source host to a remote host by doing :
   * - On the source host:
   * Committing changes using [[com.scaladock.client.Container.commit()]]
   * Pushing onto the registry using [[com.scaladock.client.Image.push()]]
   * - On the remote host:
   * Pulling the image using [[com.scaladock.client.DockerConnection.createImage()]]
   * Create a new [[com.scaladock.client.Container]] with this image
   * Run the container using [[com.scaladock.client.Container.start()]]
   *
   * using(registry) { container1 >> destHost }
   *
   * @param dest
   * @return
   */
  def >>(dest: DockerConnection): Try[String] = Try {

    // TODO
    ""
  }

}
