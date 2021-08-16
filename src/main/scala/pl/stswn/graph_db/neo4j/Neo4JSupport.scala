package pl.stswn.graph_db.neo4j

import org.neo4j.driver.{ Driver, Query }

import scala.jdk.CollectionConverters._
import zio.Task
import scala.collection.mutable

case class Neo4JQuery(query: Query):
  def execute(driver: Driver): Task[Unit] = Task.effectAsync { callback =>
    driver.asyncSession().runAsync(query).whenComplete { (_, error) =>
      if error != null then
        callback(Task.fail(error))
      else
        callback(Task.unit)
    }
  }

  def list(driver: Driver): Task[List[Map[String, Any]]] = Task.effectAsync { callback =>
    driver.asyncSession().runAsync(query).thenComposeAsync(_.listAsync()).whenComplete { (result, error) =>
      if error != null then
        callback(Task.fail(error))
      else
        callback(Task.succeed(result.asScala.map(_.asMap.asScala.toMap).toList))
    }
  }

end Neo4JQuery

case class Neo4JQueryBuilder(parts: Seq[String], params: Seq[AnyRef]):
  def +(other: Neo4JQueryBuilder): Neo4JQueryBuilder = Neo4JQueryBuilder(
    parts.dropRight(1) ++ ((parts.last + " " + other.parts.head) +: other.parts.tail),
    params ++ other.params
  )

  def query: Neo4JQuery =
    val strings   = parts.iterator
    val paramsIt  = params.iterator
    val query     = new StringBuilder(strings.next())
    val paramsMap = mutable.Map.empty[String, AnyRef]
    var nextParam = 0
    while (strings.hasNext) do
      val paramString = s"p$nextParam"
      query.append("$" + paramString)
      query.append(strings.next())
      val param = paramsIt.next() match
        case None        => null
        case Some(value) => value.asInstanceOf[AnyRef]
        case value       => value
      paramsMap.put(paramString, param)
      nextParam += 1
    Neo4JQuery(new Query(query.toString, paramsMap.asJava))

end Neo4JQueryBuilder

trait Neo4JSupport:
  extension (sc: StringContext)
    def c(args: Any*): Neo4JQueryBuilder = Neo4JQueryBuilder(sc.parts, args.map(_.asInstanceOf[AnyRef]))
