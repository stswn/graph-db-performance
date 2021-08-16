package pl.stswn.graph_db.model

import zio._
import zio.blocking.Blocking
import zio.random.Random
import zio.stream.ZSink.collectAll
import zio.stream.ZStream
import zio.stream.ZTransducer._
import zio.test.Gen

import java.time.{LocalDate, LocalDateTime}

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

def elements(
  numOfEntities: Long,
  numOfAccounts: Long,
  numOfTransactions: Long
): ZStream[Random with Blocking, Nothing, ModelElement] =
  def genEntity =
    for {
      name    <- genName
      country <- genCountry
    } yield Entity(0L, name, country)

  def genName = for {
    adj     <- genAdjective
    pokemon <- genPokemon
  } yield s"$adj $pokemon"

  def genAdjective: Gen[Blocking with Random, String] = genFromResource("adjectives.txt")

  def genPokemon: Gen[Blocking with Random, String] = genFromResource("pokemon_names.txt")

  def genFromResource(name: String): Gen[Blocking with Random, String] =
    Gen.fromEffect {
      ZStream
        .fromResource(name)
        .transduce(utf8Decode)
        .transduce(splitLines)
        .run(collectAll)
        .map(_.toList)
        .catchAll(ex => ZIO.succeed(List(ex.getMessage)))
    }.flatMap { items =>
      Gen.elements(items: _*)
    }

  def genCountry = Gen.elements(
    "GB",
    "PL",
    "NL",
    "US",
    "DE",
    "FR",
    "ES",
    "RU",
    "CN"
  )

  def genMandatoryAccount(entity: Entity) =
    for {
      bank   <- genBank(entity.country)
      number <- genNumber(entity.country)
    } yield Account(0L, entity.id, number, bank)

  def genBank(country: String) =
    Gen.stringBounded(4, 6)(Gen.alphaChar).map(bic => bic.toUpperCase + country)

  def genNumber(country: String) =
    Gen.stringN(30)(Gen.numericChar).map(bban => country + bban)

  def genAccount(numOfEntities: Long) = for {
    entityId <- Gen.long(0, numOfEntities - 1)
    country  <- genCountry
    number   <- genNumber(country)
    bank     <- genBank(country)
  } yield Account(0L, entityId, number, bank)

  def genMandatoryTransaction(accountId: Long, numOfAccounts: Long) = for {
    outgoing       <- Gen.boolean
    otherAccountId <- Gen.long(0L, numOfAccounts - 1).filter(_ != accountId)
    amount         <- Gen.bigDecimal(5, 100000)
    date           <- Gen.localDateTime(LocalDate.of(2000, 1, 1).atStartOfDay, LocalDate.of(2021, 1, 1).atStartOfDay)
    description    <- Gen.option(Gen.elements("Short description", "Test", "Slightly longer description, oh yeah!"))
  } yield Transaction(
    0L,
    if (outgoing) accountId else otherAccountId,
    if (outgoing) otherAccountId else accountId,
    amount,
    date,
    description
  )

  def genTransaction(numOfAccounts: Long) = for {
    senderId   <- Gen.long(0L, numOfAccounts - 1)
    receiverId <- Gen.long(0L, numOfAccounts - 1).filter(_ != senderId)
    amountBase <- Gen.int(1, 100)
    amountExp  <- Gen.int(0, 3)
    amount = BigDecimal(amountBase) * (BigDecimal(10) pow amountExp)
    date        <- Gen.localDateTime(LocalDate.of(2020, 7, 1).atStartOfDay, LocalDate.of(2021, 1, 1).atStartOfDay)
    description <- Gen.option(Gen.elements("Short description", "Test", "Slightly longer description, oh yeah!"))
  } yield Transaction(
    0L,
    senderId,
    receiverId,
    amount,
    date,
    description
  )

  val entities = ZStream.iterate(0L)(_ + 1).take(numOfEntities).flatMap { entityId =>
    genEntity.sample.map(_.value.copy(id = entityId))
  }
  val mandatoryEntitiesWithAccounts = entities.flatMap { entity =>
    genMandatoryAccount(entity).sample.map(_.value).flatMap { account =>
      ZStream(entity, account.copy(id = entity.id))
    }
  }
  val otherAccounts = ZStream.iterate(numOfEntities)(_ + 1).take(numOfAccounts - numOfEntities).flatMap { accountId =>
    genAccount(numOfEntities).sample.map(_.value.copy(id = accountId))
  }
  val mandatoryTransactions = ZStream.iterate(0L)(_ + 1).take(numOfAccounts).flatMap { accountId =>
    genMandatoryTransaction(accountId, numOfAccounts).sample.map(_.value.copy(id = accountId))
  }
  val otherTransactions =
    ZStream.iterate(numOfAccounts)(_ + 1).take(numOfTransactions - numOfAccounts).flatMap { transactionId =>
      genTransaction(numOfAccounts).sample.map(_.value.copy(id = transactionId))
    }

  mandatoryEntitiesWithAccounts ++ otherAccounts ++ mandatoryTransactions ++ otherTransactions

end elements


