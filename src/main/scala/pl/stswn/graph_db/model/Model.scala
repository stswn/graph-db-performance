package pl.stswn.graph_db.model

import java.time.LocalDateTime

sealed trait ModelElement

case class Entity(id: Long, name: String, country: String) extends ModelElement

case class Account(id: Long, entityId: Long, number: String, institution: String) extends ModelElement

case class Transaction(
  id: Long,
  sender: Long,
  receiver: Long,
  amount: BigDecimal,
  date: LocalDateTime,
  description: Option[String]
) extends ModelElement
