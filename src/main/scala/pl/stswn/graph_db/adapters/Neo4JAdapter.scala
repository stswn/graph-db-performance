package pl.stswn.graph_db.adapters

import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.stream.ZStream
import zio.{ Has, Task, ZIO, ZLayer }
import com.dimafeng.testcontainers.Neo4jContainer
import org.neo4j.driver.{ Driver, GraphDatabase }
import pl.stswn.graph_db.DbContainer
import pl.stswn.graph_db.model.{ ModelElement, Account, Entity, Transaction }
import pl.stswn.graph_db.neo4j.Neo4JSupport

object Neo4JAdapter:
  type Neo4JDriver = Has[Driver]

  val live: ZLayer[Blocking with Clock with Random, Throwable, TestAdapter] = 
    val filledDriver = DbContainer.neo4J >>> driver
    (filledDriver ++ ZLayer.identity[Blocking with Clock with Random]) >>> layer

  private def driver = {
    import org.neo4j.driver.AuthTokens
    for {
      container <- ZIO.service[Neo4jContainer].toManaged_
      driver = GraphDatabase.driver(container.boltUrl, AuthTokens.basic(container.username, container.password))
    } yield driver
  }.toLayer

  private def layer
    : ZLayer[Has[Clock.Service] with Has[Driver] with Random with Blocking, Nothing, Has[TestAdapter.Service]] = (
    for {
      streamEnv <- ZIO.environment[Random with Blocking]
      driver    <- ZIO.service[Driver]
      clock     <- ZIO.service[Clock.Service]
    } yield new TestAdapter.Service with Neo4JSupport {
      private def time(task: Task[_]): Task[Long] = for {
        start <- clock.nanoTime
        _     <- task
        end   <- clock.nanoTime
      } yield end - start

      override def insertTestData(elements: ZStream[Random with Blocking, Nothing, ModelElement]): Task[Long] = time {
        c"CREATE CONSTRAINT ON (e:Entity) ASSERT (e.id) IS UNIQUE".query.execute(driver) *>
          c"CREATE CONSTRAINT ON (a:Account) ASSERT (a.id) IS UNIQUE".query.execute(driver) *>
          elements.provide(streamEnv).foreach {
            case Entity(id, name, country) =>
              c"CREATE (e:Entity {id: $id, name: $name, country: $country})".query.execute(driver)
            case Account(id, eId, n, ins) =>
              (
                c"MATCH (e:Entity {id: $eId})" +
                  c"CREATE (a:Account {id: $id, number: $n, institution: $ins})" +
                  c"CREATE (e)-[o:OWNS]->(a)" +
                  c"CREATE (a)-[b:BELONGS_TO]->(e)"
              ).query.execute(driver)
            case Transaction(_, s, r, a, d, desc) =>
              (
                c"MATCH (sa:Account {id: $s})" +
                  c"MATCH (ra:Account {id: $r})" +
                  c"CREATE (sa)-[r:TRANSACTS {amount: ${(a * 10).toLong}, date: $d, description: $desc}]->(ra)"
              ).query.execute(driver)
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
        ).query.list(driver)
      }
    }
  ).toLayer

end Neo4JAdapter
