package pl.stswn.graph_db

import zio.console._
import zio.random.Random
import zio.{ App, ExitCode, URIO, ZIO, random }

import pl.stswn.graph_db.adapters.{ Neo4JAdapter, PostgresAdapter, TestAdapter }

object Main extends App {

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    putStrLn("Graph database performance testing") *>
      postgresLogic *>
      noe4JLogic
  }.exitCode

  val numOfEntities = 100L

  def logic(database: String): ZIO[Console with Random with TestAdapter, Throwable, Unit] = for {
    _ <- putStrLn(s"$database :: start")
    elements = model.elements(numOfEntities, 250, 25000)
    initTime <- TestAdapter.insertTestData(elements)
    _        <- putStrLn(s"$database :: init :: ${toMsString(initTime)}")
    _ <- (for {
      entityId <- random.nextLongBetween(0L, numOfEntities)
      testTime <- TestAdapter.test1(entityId)
      _        <- putStrLn(s"$database :: test1 :: ${toMsString(testTime)}")
    } yield ()).repeatN(7)
  } yield ()

  val postgresLogic: ZIO[zio.ZEnv, Throwable, Unit] =
    logic("PostgreSQL").provideCustomLayer(PostgresAdapter.live)

  val noe4JLogic: ZIO[zio.ZEnv, Throwable, Unit] =
    logic("Neo4J").provideCustomLayer(Neo4JAdapter.live)

  private def toMsString(nanos: Long): String =
    (nanos / 1000000).toString + "ms"
}
