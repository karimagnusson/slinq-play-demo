package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import slinq.pg.pekko.api.{*, given}
import slinq.pg.play.json.PlayJson
import demo.responses.PlayJsonDemo
import models.world.*

// PostgreSQL array operations.

@Singleton
class ArrayRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: SlinqPg) extends BaseController
                            with PlayJson // implicit conversion to Json
                            with PlayJsonDemo {

  val countryData = Model.get[CountryData]

  // Get country with its languages array
  def arrayLangs(code: String) = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        t.code,
        t.langs
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  // Add language to array (unique + sorted ascending)
  def arrayAdd = Action.async(parse.json) { request =>

    val code = (request.body \ "code").as[String]
    val lang = (request.body \ "lang").as[String]

    sql
      .update(countryData)
      .set(_.langs addUniqueAsc lang)
      .where(_.code === code)
      .returningNamed(t => Seq(
        t.code,
        t.langs
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  // Remove all instances of a language from array
  def arrayDel = Action.async(parse.json) { request =>

    val code = (request.body \ "code").as[String]
    val lang = (request.body \ "lang").as[String]

    sql
      .update(countryData)
      .set(_.langs -= lang)
      .where(_.code === code)
      .returningNamed(t => Seq(
        t.code,
        t.langs
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }
}