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

// JSONB operations.
// JSON operators follow PostgreSQL syntax: -> for object, ->> for text, #>> for deep path.

@Singleton
class JsonbRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: SlinqPg) extends BaseController
                            with PlayJson
                            with PlayJsonDemo {

  val countryData = Model.get[CountryData]

  // Select JSONB field
  def jsonbCountry(code: String) = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        t.uid,
        t.code,
        t.langs,  // array field
        t.data    // jsonb field
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  // Query nested JSONB field and concatenate JSONB objects
  def jsonbCapital(name: String) = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        t.uid,
        t.code,
        t.langs,
        (t.data || t.cities).as("data") // add cities to data
      ))
      .where(_.data -> "capital" ->> "name" === name)
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  // Access JSONB array element and extract nested values
  def jsonbCityPopulation = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        t.uid,
        t.code,
        (t.data ->> "name").as("name"),
        (t.cities -> "cities" -> 0).as("largest_city")
      ))
      .where(t => (t.cities -> "cities" -> 0 ->> "population").isNotNull)
      .orderBy(t => (t.cities -> "cities" -> 0 ->> "population").asInt.desc)
      .limit(5)
      .runAs[JsValue]
      .map(jsonList)

  }

  // Deep path extraction and aggregate
  def jsonbCapitalAvg(cont: String) = Action.async {
    sql
      .select(countryData)
      .colsNamed(t => Seq(
        Agg.avg((t.data #>> Seq("capital", "population")).asInt)
      ))
      .where(t => Seq(
        (t.data #>> Seq("capital", "population")).isNotNull,
        t.data ->> "continent" === cont
      ))
      .runHeadAs[JsValue]
      .map(jsonObj)
  }

  // Add field to JSONB object
  def addPhone = Action.async(parse.json) { request =>

    val code = (request.body \ "code").as[String]
    val phone = (request.body \ "phone").as[String]

    sql
      .update(countryData)
      .set(_.data += Json.obj("phone" -> phone)) // add "phone" to object
      .where(_.code === code)
      .returningNamed(t => Seq(
        t.uid,
        t.data
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  // Remove field from JSONB object
  def delPhone = Action.async(parse.json) { request =>

    val code = (request.body \ "code").as[String]

    sql
      .update(countryData)
      .set(_.data -= "phone") // remove "phone" from the object
      .where(_.code === code)
      .returningNamed(t => Seq(
        t.uid,
        t.data
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }
}









