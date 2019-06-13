
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.example.PageContent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.Future
import scala.io.StdIn

object AkkaQuickstart {
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val serverSource = Http().bind(interface = "localhost", port = 8080)

    val requestHandler = (req: HttpRequest) => {
      println(s"req : $req")
      req match {
        case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
          HttpResponse(entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            PageContent.pageHeader() + PageContent.homepageContent() + PageContent.pageFooter()))

        case HttpRequest(POST, Uri.Path("/submit"), _, entity, _) =>
          println(s"entdata: ${req.entity.dataBytes}")
          HttpResponse(entity = "PONG!")

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