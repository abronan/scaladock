package com.scaladock.client

import java.io.InputStreamReader
import scalaj.http.{HttpOptions, Http}

/**
 * Http Helper class to deal with requests to the remote API
 */
trait HttpHelper {

  /**
   * GET method helper
   * @param caller
   * @param request
   * @param params
   * @tparam A
   * @return String
   */
  def get[A <: Connection](caller: A)(request: String, params: Option[Map[String, String]] = None): String = {
    params match {
      case Some(_) => {
        Http
          .get(s"${caller.URL}/$request")
          .params(params.get)
          .asString
      }
      case None => {
        Http
          .get(s"${caller.URL}/$request")
          .asString
      }
    }
  }

  /**
   * POST method helper with Json
   * @param caller
   * @param request
   * @param json
   * @tparam A
   * @return String
   */
  def postJson[A <: Connection](caller: A)(request: String, json: Option[String] = None): String = {
    Http
      .postData(s"${caller.URL}/$request", json.getOrElse(""))
      .header("content-type", "application/json")
      .asString
  }

  /**
   * POST method helper with standard params Map
   * @param caller
   * @param request
   * @param params
   * @tparam A
   * @return String
   */
  def post[A <: Connection](caller: A)(request: String, params: Option[Map[String, String]] = None): String = {
    params match {
      case Some(_) => {
        Http
          .post(s"${caller.URL}/$request")
          .params(params.get)
          .header("content-type", "application/json")
          .asString
      }
      case None => {
        Http
          .post(s"${caller.URL}/$request")
          .header("content-type", "application/json")
          .asString
      }
    }
  }

  /**
   * POST method helper with standard params Map and Authentication
   * @param caller
   * @param request
   * @param params
   * @tparam A
   * @return String
   */
  def postAuth[A <: Connection](caller: A)(request: String, params: Option[Map[String, String]] = None,
                                           auth: String): String = {
    params match {
      case Some(_) => {
        Http
          .post(s"${caller.URL}/$request")
          .params(params.get)
          .header("X-Registry-Auth", auth.toString)
          .asString
      }
      case None => {
        Http
          .post(s"${caller.URL}/$request")
          .header("X-Registry-Auth", auth.toString)
          .asString
      }
    }
  }

  /**
   * POST method helper with headers parsing
   * @param caller
   * @param request
   * @tparam A
   * @return InputStreamReader
   */
  def postParseHeaders[A <: Connection](caller: A)(request: String): (Int, Map[String, List[String]], InputStreamReader) = {
    Http
      .post(s"${caller.URL}/$request")
      .asHeadersAndParse {
      inputStream => new InputStreamReader(inputStream)
    }
  }

  /**
   * DELETE method helper for deleting a resource
   * @param caller
   * @param request
   * @tparam A
   * @return InputStreamReader
   */
  def delete[A <: Connection](caller: A)(request: String): String = {
    Http
      .post(s"${caller.URL}/$request")
      .option(HttpOptions.method("DELETE"))
      .header("content-type", "application/json")
      .asString
  }

  /**
   * PING command (GET with parse headers)
   * @param url
   * @param request
   * @return
   */
  def ping(url: String, request: String): Boolean = {
    val (responseCode, headers, inputStream) = Http
      .get(s"$url/$request").asHeadersAndParse {
      inputStream => new InputStreamReader(inputStream)
    }
    return responseCode >= 400
  }
}
