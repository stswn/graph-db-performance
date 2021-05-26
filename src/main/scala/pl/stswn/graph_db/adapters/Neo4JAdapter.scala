package pl.stswn.graph_db.adapters

import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.stream.ZStream
import zio.{ Task, ZIO, ZLayer }

import com.dimafeng.testcontainers.Neo4jContainer
import neotypes.generic.auto._
import neotypes.implicits.syntax.cypher._
import neotypes.{ Driver, GraphDatabase }
import pl.stswn.graph_db.DbContainer
import pl.stswn.graph_db.model.{ Account, Entity, ModelElement, Transaction }

object Neo4JAdapter {
  val live: ZLayer[Blocking with Clock with Random, Throwable, TestAdapter] = {
    val filledDriver = DbContainer.neo4J >>> driver
    (filledDriver ++ ZLayer.identity[Blocking with Clock with Random]) >>> layer
  }

  private def driver = {
    import neotypes.zio.implicits._
    import org.neo4j.driver.AuthTokens
    for {
      container <- ZIO.service[Neo4jContainer].toManaged_
      driver    <- GraphDatabase.driver[Task](container.boltUrl, AuthTokens.basic(container.username, container.password))
    } yield driver
  }.toLayer

  private def layer = (
    for {
      streamEnv <- ZIO.environment[Random with Blocking]
      driver    <- ZIO.service[Driver[Task]]
      clock     <- ZIO.service[Clock.Service]
    } yield new TestAdapter.Service {
      private def time(task: Task[_]): Task[Long] = for {
        start <- clock.nanoTime
        _     <- task
        end   <- clock.nanoTime
      } yield end - start

      override def insertTestData(elements: ZStream[Random with Blocking, Nothing, ModelElement]): Task[Long] = time {
        c"CREATE CONSTRAINT ON (e:Entity) ASSERT (e.id) IS UNIQUE".query[Unit].execute(driver) *>
          c"CREATE CONSTRAINT ON (a:Account) ASSERT (a.id) IS UNIQUE".query[Unit].execute(driver) *>
          elements.provide(streamEnv).foreach {
            case Entity(id, name, country) =>
              c"CREATE (e:Entity {id: $id, name: $name, country: $country})".query[Unit].execute(driver)
            case Account(id, eId, n, ins) =>
              (
                c"MATCH (e:Entity {id: $eId})" +
                  c"CREATE (a:Account {id: $id, number: $n, institution: $ins})" +
                  c"CREATE (e)-[o:OWNS]->(a)" +
                  c"CREATE (a)-[b:BELONGS_TO]->(e)"
              ).query[Unit].execute(driver)
            case Transaction(_, s, r, a, d, desc) =>
              (
                c"MATCH (sa:Account {id: $s})" +
                  c"MATCH (ra:Account {id: $r})" +
                  c"CREATE (sa)-[r:TRANSACTS {amount: ${(a * 10).toLong}, date: $d, description: $desc}]->(ra)"
              ).query[Unit].execute(driver)
          }
      }

      override def test1(entityId: Long): Task[Long] = time {
        (
          c"MATCH (e:Entity {id: $entityId})" +
            c"CALL {" +
            c"WITH e MATCH (e)-[:OWNS]->()-[]-()-[:BELONGS_TO]->(c)" +
            c"RETURN c, 1 as level" +
            c"UNION" +
            c"WITH e MATCH (e)-[:OWNS]->()-[]-()-[:BELONGS_TO]->()-[:OWNS]->()-[]-()-[:BELONGS_TO]->(c)" +
            c"RETURN c, 2 as level" +
            c"}" +
            c"RETURN c.id, c.name, MIN(level) as level" +
            c"ORDER BY level ASC"
        ).query[(Long, String, Int)].list(driver)
      }
    }
  ).toLayer
}
