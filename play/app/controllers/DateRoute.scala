package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import slinq.pg.pekko.api.{*, given}
import slinq.pg.fn.*
import slinq.pg.play.json.PlayJson
import demo.responses.PlayJsonDemo
import models.world.*

// Timestamp operations and date part extraction.

@Singleton
class DateRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: SlinqPg) extends BaseController
                            with PlayJson
                            with PlayJsonDemo {

  val btcPrice = Model.get[BtcPrice]

  // Extract date parts (year, day of year) and format timestamp
  def btcHour = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0))
    sql
      .select(btcPrice)
      .colsNamed(t => Seq(
        t.high.round(2),
        t.low.round(2),
        t.open.round(2),
        t.close.round(2),
        t.created.format("DD Mon YYYY HH24:MI")
      ))
      .where(t => Seq(
        t.created.year === params("year").toInt, // pick year from timestamp
        t.created.doy === params("doy").toInt    // pick day of year from timestamp
      ))
      .orderBy(_.created.asc)
      .runAs[JsValue]
      .map(jsonList(_))
  }

  // Aggregate data by quarter
  def btcQuarterAvg = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0))
    sql
      .select(btcPrice)
      .colsNamed(t => Seq(
        "avg" -> Agg.avg(t.close).round(2),
        "max" -> Agg.max(t.close).round(2),
        "min" -> Agg.min(t.close).round(2)
      ))
      .where(t => Seq(
        t.created.year === params("year").toInt,
        t.created.quarter === params("quarter").toInt
      ))
      .runHeadAs[JsValue]
      .map(jsonObj(_))
  }

  // Extract multiple date parts (year, quarter, week)
  def btcBreak = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0))
    sql
      .select(btcPrice)
      .colsNamed(t => Seq(
        "price" -> t.high.round(2),
        "year" -> t.created.year,
        "quarter" -> t.created.quarter,
        "week" -> t.created.week,
        "date" -> t.created.format("DD Mon YYYY HH24:MI")
      ))
      .where(_.high >= BigDecimal(params("price")))
      .orderBy(_.high.asc)
      .limit(1)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }
}













