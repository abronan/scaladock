package com.scaladock.client.util

import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization._

/**
 * Trait Mixin to Pretty Print Case Classes
 */
trait PrettyPrinter {

  implicit val formats = DefaultFormats

  def pretty: String = writePretty(this)
}
