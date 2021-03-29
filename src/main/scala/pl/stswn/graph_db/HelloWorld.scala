package pl.stswn.graph_db

import zio.App
import zio.console._
import java.io.IOException
import zio.ZIO

object HelloWorld extends App {

  def run(args: List[String]) =
    myAppLogic.exitCode

  val myAppLogic: ZIO[Console, IOException, Unit] =
    for {
      _    <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _    <- putStrLn(s"Hello, $name, welcome to ZIO!")
    } yield ()
}
