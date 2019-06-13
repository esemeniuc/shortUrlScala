package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.headers
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.StdIn
import scala.util.Random

object AkkaQuickstart {
  val rootUrl = "http://localhost:8080"
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher
  private val urlMap = mutable.Map[String, String]("" -> rootUrl)

  private def requestHandler(req: HttpRequest): HttpResponse = {

    req match {
      case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
        HttpResponse(entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          PageContent.header() + PageContent.homepageContent() + PageContent.footer()))

      case HttpRequest(POST, Uri.Path("/submit"), _, entity, _) =>
        val url = Await.result(Unmarshal(req).to[String], 1.second)
        val (decodedUrl, ok) = DecodeURL(url)
        println(s"user submitted: $decodedUrl")
        if (ok) {
          val randomString = Random.alphanumeric.take(10).mkString
          urlMap(randomString) = decodedUrl
          val shortenedUrl = s"$rootUrl/$randomString"
          return HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
            PageContent.header + s"Success, go to <a href=$shortenedUrl>$shortenedUrl</a>" + PageContent.footer()))
        }
        HttpResponse(entity = "Error in post request!")

      case default => //catch all
        val shortcodeParts = req.getUri().path().split("/")
        default.discardEntityBytes() // important to drain incoming HTTP Entity stream
        if (shortcodeParts.length != 2) {
          return HttpResponse(entity = "Error in shortcode!")
        }
        val shortcode = shortcodeParts(1)
        println(s"shortcode: $shortcode")
        if (!urlMap.contains(shortcode)) {
          val shortenedUrl = s"$rootUrl/$shortcode"
          return HttpResponse(404, entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
            PageContent.header + s"Sorry, couldn't find $shortenedUrl" + PageContent.footer()))
        }

        HttpResponse(
          status = StatusCodes.TemporaryRedirect,
          headers = List(headers.Location(urlMap(shortcode))),
          entity = ""
        )
    }
  }

  def main(args: Array[String]) {

    val serverSource = Http().bind(interface = "localhost", port = 8080)

    val bindingFuture: Future[Http.ServerBinding] =
      serverSource.to(Sink.foreach {
        connection =>
          println("Accepted new connection from " + connection.remoteAddress)
          connection.handleWithSyncHandler(requestHandler)
        // this is equivalent to
        // connection handleWith { Flow[HttpRequest] map requestHandler }
      }).run()

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture.flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}