
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink


import scala.concurrent.Future

object WebServer {
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val serverSource = Http().bind(interface = "localhost", port = 8080)

    val requestHandler = (req: HttpRequest) => {
      println(s"req : $req")
      req match {
        case HttpRequest(GET, Uri.Path("/"), headers, _, _) =>
          println(s"header: $headers")
          HttpResponse(entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "<html><body>Hello world!</body></html>"))

        case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
          HttpResponse(entity = "PONG!")


        case default =>
          println(s"header: ${default.headers}")
          println(s"uri ${default.uri}")
          println(s"uri ${default.getUri()}")
          println(s"uri ${default.effectiveUri(securedConnection = false)}")

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
  }
}