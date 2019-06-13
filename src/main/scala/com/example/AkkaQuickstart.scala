
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.example.{DecodeURL, PageContent}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.StdIn
import scala.util.Random

object AkkaQuickstart {
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val serverSource = Http().bind(interface = "localhost", port = 8080)

    var urlMap = mutable.Map[String, String]()
    val requestHandler = (req: HttpRequest) => {
      println(s"req : $req")
      req match {
        case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
          HttpResponse(entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            PageContent.header() + PageContent.homepageContent() + PageContent.footer()))

        case HttpRequest(POST, Uri.Path("/submit"), _, entity, _) =>
          val url = Await.result(Unmarshal(req).to[String], 1.second)
          val (decodedUrl, ok) = DecodeURL(url)
          println(s"output: $decodedUrl")
          if (ok) {
            val randomString = Random.alphanumeric.take(10).mkString
            urlMap(decodedUrl) = randomString
            val shortenedUrl = s"http://localhost:8080/$randomString"
            HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
              PageContent.header + s"Success, go to <a href=$shortenedUrl>$shortenedUrl</a>" + PageContent.footer()))
          } else {
            HttpResponse(entity = "Error in post request!")
          }


        case default =>
          println(s"uri ${default.getUri()}")
          default.discardEntityBytes() // important to drain incoming HTTP Entity stream
          HttpResponse(404, entity = "Unknown resource!")
      }
    }

    val bindingFuture: Future[Http.ServerBinding] =
      serverSource.to(Sink.foreach { connection =>
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