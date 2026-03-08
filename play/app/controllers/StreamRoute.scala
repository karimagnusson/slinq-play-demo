package controllers

import java.util.UUID
import java.sql.Timestamp
import javax.inject._
import scala.concurrent.{Future, ExecutionContext}
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.http.HttpEntity
import scala.util.{Try, Success, Failure}
import org.apache.pekko.util.ByteString
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.actor.ActorSystem
import slinq.pg.pekko.api.{*, given}
import slinq.pg.fn.*
import demo.responses.PlayJsonDemo
import models.world.*

// Streaming data export and import.

@Singleton
class StreamRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           as: ActorSystem,
           db: SlinqPg) extends BaseController
                            with PlayJsonDemo {

  val coinPrice = Model.get[CoinPrice]
  val tempCoinPrice = Model.get[TempCoinPrice]

  val makeLine: Tuple3[String, String, Timestamp] => String = {
    case (coin, price, takenAt) =>
      "%s,%s,%s\n".format(coin, price, takenAt.toString)
  }

  val splitLine = Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true)

  val parseLine: String => Tuple3[String, BigDecimal, Timestamp] = { line =>
    line.split(',') match {
      case Array(coin, price, takenAt) =>
        (coin, BigDecimal(price), Timestamp.valueOf(takenAt))
      case _ =>
        throw new Exception("invalid file")
    }
  }

  val insertCoinPriceStm = sql
    .insert(coinPrice)
    .cols(t => (
      t.coin,
      t.price,
      t.created
    ))
    .cache

  // Stream database query results as CSV file
  def streamExport(code: String) = Action {
    val src = sql
      .select(coinPrice)
      .cols(t => (
        t.coin,
        Fn.roundStr(t.price, 2),
        t.created
      ))
      .where(_.coin === code.toUpperCase)
      .orderBy(_.created.asc)
      .source()
      .map(makeLine)
      .map(ByteString(_))

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Streamed(src, None, Some("text/csv"))
    )
  }

  // Stream CSV file upload directly to database with batching (sample file in csv folder)
  def streamImport = Action.async(parse.temporaryFile) { request =>
    FileIO
      .fromPath(request.body.path)
      .via(splitLine)
      .map(_.utf8String)
      .map(parseLine)
      .grouped(100) // insert 100 in each transaction.
      .runWith(insertCoinPriceStm.asListSink)
      .map(jsonSuccess)
  }

  // Safe import using temporary table with rollback on failure (sample file in csv folder)
  def streamSafeImport = Action.async(parse.temporaryFile) { request =>
    
    val uid = UUID.randomUUID

    (for {

      _ <- FileIO
        .fromPath(request.body.path)
        .via(splitLine)
        .map(_.utf8String)
        .map(parseLine)
        .map {  // Add UUID used by the temp table.
          case (code, price, created) =>
            (uid, code, price, created)
        }
        .runWith(sql
          .insert(tempCoinPrice)
          .cols(t => (
            t.uid,
            t.coin,
            t.price,
            t.created
          ))
          .cache
          .asSink
        )

      _ <- sql  // Add UUID used by the temp table.
        .insert(coinPrice)
        .cols(t => (
          t.coin,
          t.price,
          t.created
        ))
        .fromSelect(
          sql
            .select(tempCoinPrice)
            .cols(t => (
              t.coin,
              t.price,
              t.created
            ))
            .where(_.uid === uid)
        )
        .run

    } yield ()).transformWith { res =>
      val rsp = res match { 
        case Success(_) => jsonSuccess(())
        case Failure(ex) => jsonError(ex)
      }
      for {
        _ <- sql
          .delete(tempCoinPrice)
          .where(_.uid === uid)
          .run
      } yield rsp
    }
  }
}




















