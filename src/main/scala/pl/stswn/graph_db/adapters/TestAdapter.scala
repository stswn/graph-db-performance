package pl.stswn.graph_db.adapters

import zio.blocking.Blocking
import zio.random.Random
import zio.stream.ZStream
import zio.{ Task, ZIO, Has }

import pl.stswn.graph_db.model.ModelElement

type TestAdapter = Has[TestAdapter.Service]

object TestAdapter:
  trait Service:
    def insertTestData(elements: ZStream[Random with Blocking, Nothing, ModelElement]): Task[Long]
    def test1(entityId: Long): Task[Long]

  def insertTestData(
    elements: ZStream[Random with Blocking, Nothing, ModelElement]
  ): ZIO[TestAdapter, Throwable, Long] =
    ZIO.accessM(_.get.insertTestData(elements))

  def test1(entityId: Long): ZIO[TestAdapter, Throwable, Long] =
    ZIO.accessM(_.get.test1(entityId))
