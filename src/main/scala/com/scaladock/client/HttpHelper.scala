package com.scaladock.client

import java.io.{BufferedReader, InputStreamReader}
import scalaj.http.{HttpException, HttpOptions, Http}

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
    var call = Http
      .get(s"${caller.URL}/$request")
      .options(HttpOptions.connTimeout(caller.timeout), HttpOptions.readTimeout(caller.timeout))

    call = params match {
      case Some(_) => call.params(params.get)
      case None => call
    }

    call.asString
  }

  /**
   * GET Stream method helper
   * @param caller
   * @param request
   * @param params
   * @tparam A
   * @return
   */
  def getStreamAndClose[A <: Connection](caller: A)(request: String, params: Option[Map[String, String]] = None)
  : (Int, Map[String, List[String]], String) = {
    var call = Http
      .get(s"${caller.URL}/$request")
      .options(HttpOptions.connTimeout(caller.timeout), HttpOptions.readTimeout(caller.timeout))

    call = params match {
      case Some(_) => call.params(params.get)
      case None => call
    }

    call.asHeadersAndParse {
      inputStream => {
        val in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))
        val data = new StringBuilder
        val buffer = Array[Char](4096)

        def readOnce {
          if (in.ready()) {
            in.read(buffer)
            data.appendAll(buffer)
            readOnce
          }
        }

        readOnce
        data.mkString
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
  def postJson[A <: Connection](caller: A)(request: String, json: Option[String] = None,
                                           params: Option[Map[String, String]] = None): String = {
    var call = json match {
      case Some(_) => {
        Http
          .postData(s"${caller.URL}/$request", json.getOrElse(""))
          .header("content-type", "application/json")
      }
      case None => Http.post(s"${caller.URL}/$request")
    }

    call = params match {
      case Some(_) => call.params(params.get)
      case None => call
    }

    call.asString
  }

  /**
   * POST method helper with Json send and raw stream receive
   * @param caller
   * @param request
   * @param json
   * @tparam A
   * @return String
   */
  def postJsonGetStream[A <: Connection](caller: A)(request: String, json: Option[String] = None,
                                           params: Option[Map[String, String]] = None): Array[Byte] = {
    var call = json match {
      case Some(_) => {
        Http
          .postData(s"${caller.URL}/$request", json.getOrElse(""))
          .header("content-type", "application/json")
      }
      case None => Http.post(s"${caller.URL}/$request")
    }

    call = params match {
      case Some(_) => call.params(params.get)
      case None => call
    }

    call.asBytes
  }

  /**
   * POST method helper with Json send and parse headers
   * @param caller
   * @param request
   * @param json
   * @tparam A
   * @return String
   */
  def postJsonWithHeaders[A <: Connection](caller: A)(request: String, json: Option[String] = None, params: Option[Map[String, String]] = None):
  (Int, Map[String, List[String]], InputStreamReader) = {
    var call = json match {
      case Some(_) => {
        Http
          .postData(s"${caller.URL}/$request", json.getOrElse(""))
          .header("content-type", "application/json")
      }
      case None => Http.post(s"${caller.URL}/$request")
    }

    call = params match {
      case Some(_) => call.params(params.get)
      case None => call
    }

    call.asHeadersAndParse {
      inputStream => new InputStreamReader(inputStream)
    }
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
    var call = Http
      .post(s"${caller.URL}/$request")
      .options(HttpOptions.connTimeout(caller.timeout), HttpOptions.readTimeout(caller.timeout))
      .header("content-type", "application/json")

    call = params match {
      case Some(_) => call.params(params.get)
      case None => call
    }

    call.asString
  }

  /**
   * POST method helper with standard params Map and Authentication
   * @param caller
   * @param request
   * @param params
   * @tparam A
   * @return String
   */
  def postAuth[A <: Connection](caller: A)(request: String, params: Option[Map[String, String]] = None, auth: String): String = {
    var call = Http
      .post(s"${caller.URL}/$request")
      .options(HttpOptions.connTimeout(caller.timeout), HttpOptions.readTimeout(caller.timeout))
      .header("X-Registry-Auth", auth.toString)

    call = params match {
      case Some(_) => call.params(params.get)
      case None => call
    }

    call.asString
  }

  /**
   * POST method helper with headers parsing
   * @param caller
   * @param request
   * @tparam A
   * @return InputStreamReader
   */
  def postParseHeaders[A <: Connection](caller: A)(request: String, params: Option[Map[String, String]] = None)
  : (Int, Map[String, List[String]], InputStreamReader) = {
    var call = Http
      .post(s"${caller.URL}/$request")
      .options(HttpOptions.connTimeout(caller.timeout), HttpOptions.readTimeout(caller.timeout))

    call = params match {
      case Some(_) => call.params(params.get)
      case None => call
    }

    call.asHeadersAndParse {
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
    val (responseCode, _, _) = Http
      .get(s"$url/$request").asHeadersAndParse {
      inputStream => new InputStreamReader(inputStream)
    }
    return responseCode >= 400
  }

  /**
   * Do a HEAD request
   * @param caller
   * @param request
   * @tparam A
   * @return
   */
  def getHead[A <: Connection](caller: A)(request: String): String = {
    Http(s"${caller.URL}/$request")
      .method("HEAD")
      .asString
  }
}
