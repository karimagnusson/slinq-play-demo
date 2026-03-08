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

// INSERT, UPDATE, DELETE with RETURNING.

@Singleton
class OperationsRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: SlinqPg) extends BaseController
                            with PlayJson
                            with PlayJsonDemo {

  val trip = Model.get[Trip]

  // INSERT with RETURNING
  def insertTrip = Action.async(parse.json) { request =>

    val cityId = (request.body \ "city_id").as[Int]
    val price = (request.body \ "price").as[Int]

    sql
      .insert(trip)
      .cols(t => (
        t.cityId,
        t.price
      ))
      .values((
        cityId,
        price
      ))
      .returningNamed(t => Seq(
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadAs[JsValue]
      .map(jsonObj(_))
  }

  // UPDATE with RETURNING
  def updateTrip = Action.async(parse.json) { request =>

    val id = (request.body \ "id").as[Int]
    val price = (request.body \ "price").as[Int]

    sql
      .update(trip)
      .set(_.price ==> price)
      .where(_.id === id)
      .returningNamed(t => Seq(
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }

  // DELETE with RETURNING
  def deleteTrip = Action.async(parse.json) { request =>

    val id = (request.body \ "id").as[Int]

    sql
      .delete(trip)
      .where(_.id === id)
      .returningNamed(t => Seq(
        t.id,
        t.cityId,
        t.price
      ))
      .runHeadOptAs[JsValue]
      .map(jsonOpt(_))
  }
}




























