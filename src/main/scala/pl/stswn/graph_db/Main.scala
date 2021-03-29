package pl.stswn.graph_db

import java.io.IOException

import zio.blocking.Blocking
import zio.console._
import zio.random.Random
import zio.{ App, ZIO }

object Main extends App {

  def run(args: List[String]) =
    logic.exitCode

  val logic: ZIO[Blocking with Console with Random, IOException, Unit] =
    model.elements(10, 20, 100).foreach { element =>
      putStrLn(element.toString)
    }
}
