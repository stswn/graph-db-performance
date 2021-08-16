package pl.stswn.graph_db

import zio.blocking.{ Blocking, effectBlocking }
import zio.{ Has, ZLayer, ZManaged }

import com.dimafeng.testcontainers.{ Neo4jContainer, PostgreSQLContainer, SingleContainer }
import izumi.reflect.Tag
import org.testcontainers.utility.DockerImageName

object DbContainer:
  type Postgres = Has[PostgreSQLContainer]
  type Neo4J    = Has[Neo4jContainer]

  private def containerLayer[T <: SingleContainer[_]: Tag](createContainer: => T) =
    ZManaged.make {
      effectBlocking {
        val container = createContainer
        container.start()
        container
      }.orDie
    }(container => effectBlocking(container.stop()).orDie).toLayer

  val postgres: ZLayer[Blocking, Nothing, Postgres] =
    containerLayer(PostgreSQLContainer(dockerImageNameOverride = DockerImageName.parse("postgres:13.3")))
  val neo4J: ZLayer[Blocking, Nothing, Neo4J] =
    containerLayer(Neo4jContainer(neo4jImageVersion = DockerImageName.parse("neo4j:4.2")))
