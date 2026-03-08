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

// Cached queries with pickWhere for runtime arguments.

@Singleton
class CacheRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: SlinqPg) extends BaseController
                            with PlayJson // implicit conversion to Json
                            with PlayJsonDemo {

  val city = Model.get[City]
  val country = Model.get[Country]
  val language = Model.get[Language]
  val trip = Model.get[Trip]

  // Cached SELECT with pickWhere for runtime arguments
  val selectCountryStm = sql
    .select(country)
    .colsNamed(t => Seq(
      t.code,
      t.name,
      t.continent,
      t.region
    ))
    .all
    .pickWhere(_.code.use === Arg)
    .cache

  // Cached JOIN with multiple pickWhere arguments
  val selectJoinStm = sql
    .select(city, country)
    .colsNamed(t => Seq(
      t.a.code,
      t.a.population,
      "city_name" -> t.a.name,
      "country_name" -> t.b.name,
      t.b.gnp,
      t.b.continent,
      t.b.region
    ))
    .joinOn(_.code, _.code)
    .where(t => Seq(
      t.b.continent === "Asia",
      t.b.gnp.isNotNull
    ))
    .orderBy(_.a.population.desc)
    .limit(5)
    .pickWhere(t => (
      t.b.population.use >= Arg,
      t.b.gnp.use >= Arg
    ))
    .cache

  // Cached INSERT
  val insertTripStm = sql
    .insert(trip)
    .cols(t => (
      t.cityId,
      t.price
    ))
    .returningNamed(t => Seq(
      t.id,
      t.cityId,
      t.price
    ))
    .cache

  // Cached UPDATE with pickSet and pickWhere
  val updateTripStm = sql
    .update(trip)
    .pickSet(_.price.use ==> Arg)
    .pickWhere(_.id.use === Arg)
    .returningNamed(t => Seq(
      t.id,
      t.cityId,
      t.price
    ))
    .cache

  // Cached DELETE
  val deleteTripStm = sql
    .delete(trip)
    .pickWhere(_.id.use === Arg)
    .returningJson(t => Seq(
      t.id,
      t.cityId,
      t.price
    ))
    .cache

  // Run cached SELECT with single argument
  def selectCountry(code: String) = Action.async {
    selectCountryStm
      .runHeadOptAs[JsValue](code.toUpperCase)
      .map(jsonOpt(_))
  }

  // Run cached JOIN with multiple arguments
  def selectJoin(pop: Int, gnp: Int) = Action.async {
    selectJoinStm
        .runAs[JsValue](pop, BigDecimal(gnp))
        .map(jsonList(_))
  }

  // Run cached INSERT
  def insertTrip = Action.async(parse.json) { request =>
    val cityId = (request.body \ "city_id").as[Int]
    val price = (request.body \ "price").as[Int]
    insertTripStm
      .runHeadAs[JsValue]((cityId, price))
      .map(jsonObj(_))
  }

  // Run cached UPDATE
  def updateTrip = Action.async(parse.json) { request =>
    val id = (request.body \ "id").as[Int]
    val price = (request.body \ "price").as[Int]
    updateTripStm
      .runHeadOptAs[JsValue](price, id)
      .map(jsonOpt(_))
  }

  // Run cached DELETE
  def deleteTrip = Action.async(parse.json) { request =>
    val id = (request.body \ "id").as[Int]
    deleteTripStm
      .runHeadOptAs[JsValue](id)
      .map(jsonOpt(_))
  }
}















