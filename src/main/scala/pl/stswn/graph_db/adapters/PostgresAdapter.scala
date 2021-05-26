package pl.stswn.graph_db.adapters

import javax.sql.DataSource

import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.stream.ZStream

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.implicits._
import doobie.postgres.implicits._
import io.github.gaelrenoux.tranzactio.doobie._
import org.postgresql.ds.PGSimpleDataSource
import pl.stswn.graph_db.DbContainer
import pl.stswn.graph_db.model.{ Account, Entity, ModelElement, Transaction }

object PostgresAdapter {
  val live: ZLayer[Blocking with Clock with Random, Throwable, TestAdapter] = {
    val postgres         = DbContainer.postgres ++ ZLayer.identity[Blocking]
    val filledDataSource = postgres >>> dataSource
    val database         = (filledDataSource ++ ZLayer.identity[Blocking with Clock]) >>> Database.fromDatasource
    (database ++ ZLayer.identity[Blocking with Clock with Random]) >>> layer
  }

  private def dataSource = (
    for {
      container <- ZIO.service[PostgreSQLContainer]
      blocking  <- ZIO.service[Blocking.Service]
      ds <- blocking.effectBlocking {
        val postgresDs = new PGSimpleDataSource
        postgresDs.setUrl(container.jdbcUrl)
        postgresDs.setUser(container.username)
        postgresDs.setPassword(container.password)
        postgresDs
      }
    } yield ds.asInstanceOf[DataSource]
  ).toLayer

  private def layer = (
    for {
      streamEnv <- ZIO.environment[Random with Blocking]
      dbEnv     <- ZIO.environment[Database]
      _         <- init.provide(dbEnv)
      clock     <- ZIO.service[Clock.Service]
    } yield new TestAdapter.Service {
      private def time(task: TranzactIO[_]): Task[Long] = for {
        start <- clock.nanoTime
        _     <- Database.transactionOrWiden(task).provide(dbEnv)
        end   <- clock.nanoTime
      } yield end - start

      override def insertTestData(elements: ZStream[Random with Blocking, Nothing, ModelElement]): Task[Long] = time {
        elements.provide(streamEnv).foreach {
          case Entity(id, name, country) =>
            tzio {
              sql"INSERT INTO entity(id, name, country) VALUES ($id, $name, $country)".update.run
            }
          case Account(id, eId, n, ins) =>
            tzio {
              sql"INSERT INTO account(id, entity_id, number, institution) VALUES ($id, $eId, $n, $ins)".update.run
            }
          case Transaction(id, s, r, a, d, desc) =>
            tzio {
              sql"INSERT INTO transaction(id, sender, receiver, amount, date, description) VALUES ($id, $s, $r, $a, $d, $desc)".update.run
            }
        }
      }

      override def test1(entityId: Long): Task[Long] = time {
        tzio {
          sql"""
               | WITH RECURSIVE counterparty(id, name, level) AS (
               |     SELECT id, name, 0 FROM entity WHERE id = $entityId
               |   UNION
               |   SELECT ids.counterparty, ids.name, c.level + 1 FROM
               |   (
               |      SELECT sa.entity_id AS entity, ra.entity_id AS counterparty, re.name AS name
               |      FROM
               |      transaction t INNER JOIN
               |      account sa ON t.sender = sa.id INNER JOIN
               |      account ra ON t.receiver = ra.id INNER JOIN
               |      entity re ON ra.entity_id = re.id
               |      UNION
               |      SELECT ra.entity_id AS entity, sa.entity_id AS counterparty, se.name AS name
               |      FROM
               |      transaction t INNER JOIN
               |      account sa ON t.sender = sa.id INNER JOIN
               |      account ra ON t.receiver = ra.id INNER JOIN
               |      entity se ON sa.entity_id = se.id
               |   ) ids
               |   INNER JOIN counterparty c ON ids.entity = c.id
               |   WHERE (c.level + 1) <= 2
               | )
               | SELECT id, name, MIN(level) AS level
               | FROM counterparty
               | WHERE id <> $entityId
               | GROUP BY id, name
               | ORDER BY level, name
               | """.stripMargin.query[(Long, String, Int)].to[List]
        }
      }
    }
  ).toLayer

  private def init = Database.transactionOrWiden(
    for {
      _ <- tzio {
        sql"""
             |CREATE TABLE entity (
             |  id BIGINT PRIMARY KEY,
             |  name TEXT NOT NULL,
             |  country TEXT NOT NULL
             |)""".stripMargin.update.run
      }
      _ <- tzio {
        sql"""
             |CREATE TABLE account (
             |  id BIGINT PRIMARY KEY,
             |  entity_id BIGINT NOT NULL,
             |  number TEXT NOT NULL,
             |  institution TEXT NOT NULL,
             |  CONSTRAINT fk_entity
             |    FOREIGN KEY(entity_id)
             |	  REFERENCES entity(id)
             |)""".stripMargin.update.run
      }
      _ <- tzio {
        sql"""
             |CREATE TABLE transaction (
             |  id BIGINT PRIMARY KEY,
             |  sender BIGINT NOT NULL,
             |  receiver BIGINT NOT NULL,
             |  amount NUMERIC(30, 4) NOT NULL,
             |  date TIMESTAMP NOT NULL,
             |  description TEXT,
             |  CONSTRAINT fk_sender
             |    FOREIGN KEY(sender)
             |	  REFERENCES account(id),
             |  CONSTRAINT fk_receiver
             |    FOREIGN KEY(receiver)
             |	  REFERENCES account(id)
             |)""".stripMargin.update.run
      }
    } yield ()
  )
}
