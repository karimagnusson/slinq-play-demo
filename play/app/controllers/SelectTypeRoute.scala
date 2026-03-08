package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import slinq.pg.pekko.api.{*, given}
import models.world.*

// Type-safe queries with case classes using Play JSON for serialization.

@Singleton
class SelectTypeRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: SlinqPg) extends BaseController {

  val country = Model.get[Country]
  val trip = Model.get[Trip]

  // Writes
  case class CountryType(code: String, name: String, population: Int)
  given Writes[CountryType] = Json.writes[CountryType]

  case class TripType(id: Long, cityId: Int, price: Int)
  given Writes[TripType] = Json.writes[TripType]

  // Reads

  case class TripDataType(cityId: Int, price: Int)
  given Reads[TripDataType] = Json.reads[TripDataType]

  case class TripPriceType(id: Long, price: Int)
  given Reads[TripPriceType] = Json.reads[TripPriceType]

  // SELECT with type-safe result mapping
  def selectCountry(code: String) = Action.async {
    sql
      .select(country)
      .cols(t => (
        t.code,
        t.name,
        t.population
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOptAs[CountryType]
      .map {
        case Some(res) => Ok(Json.toJson(res))
        case None => Ok(Json.obj("message" -> "not found"))
      }
  }

  // INSERT with type-safe input and output
  def insertTrip = Action.async(parse.json) { req =>
    val data = req.body.as[TripDataType]
    sql
      .insert(trip)
      .cols(t => (
        t.cityId,
        t.price
      ))
      .values((data.cityId, data.price))
      .returning(t => (
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadAs[TripType]
      .map(res => Ok(Json.toJson(res)))
  }

  // UPDATE with type-safe input and output
  def updateTrip = Action.async(parse.json) { req =>
    val data = req.body.as[TripPriceType]

    sql
      .update(trip)
      .set(_.price ==> data.price)
      .where(_.id === data.id)
      .returning(t => (
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadOptAs[TripType]
      .map {
        case Some(res) => Ok(Json.toJson(res))
        case None => Ok(Json.obj("message" -> "not found"))
      }
  }
}














