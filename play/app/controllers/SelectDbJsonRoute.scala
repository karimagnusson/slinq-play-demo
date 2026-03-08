package controllers

import javax.inject._
import scala.concurrent.ExecutionContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import slinq.pg.pekko.api.{*, given}
import slinq.pg.fn.*
import demo.responses.DbJsonDemo
import models.world.*

// SELECT queries with JOIN, subqueries, and aggregates.
// Here the database returns each row as a JSON string
// that is passed directly to the client.

@Singleton
class SelectDbJsonRoute @Inject()(
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext,
           db: SlinqPg) extends BaseController
                            with DbJsonDemo {

  val city = Model.get[City]
  val country = Model.get[Country]
  val language = Model.get[Language]


  // Simple SELECT
  def selectCountry(code: String) = Action.async {
    sql
      .select(country)
      .colsJson(t => Seq(
        t.code,
        t.name,
        t.continent,
        t.region
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOpt
      .map(jsonOpt(_))
  }

  // JOIN with custom field names
  def selectCities(code: String) = Action.async {
    sql
      .select(city, country)
      .colsJson(t => Seq(
        t.a.code,
        t.a.population,          // use column name
        "city_name" -> t.a.name, // define the name
        "country_name" -> t.b.name,
        t.b.continent,
        t.b.region
      ))
      .joinOn(_.code, _.code)
      .where(_.b.code === code.toUpperCase)
      .orderBy(_.a.population.desc)
      .limit(5)
      .run
      .map(jsonList(_))
  }

  // Subquery as nested object
  def selectLanguage(code: String) = Action.async {
    sql
      .select(country)
      .colsJson(t => Seq(
        t.code,
        t.name,
        sql             // subquery as a nested object
          .select(language)
          .colsJson(s => Seq(
            s.name,
            s.percentage
          ))
          .where(s => Seq(
            s.code <=> t.code,
            s.isOfficial === true
          ))
          .limit(1)
          .asColumn
          .first
          .as("language")
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOpt
      .map(jsonOpt(_))
  }

  // Nested object with Fn.json and subquery as array
  def selectCountryCities(code: String) = Action.async {
    sql
      .select(country)
      .colsJson(t => Seq(
        t.code,
        t.name,
        Fn.json(Seq(    // put some columns in a nested object
          t.continent,
          t.region,
          t.population
        )).as("info"),
        sql             // subquery as a array of objects
          .select(city)
          .colsJson(s => Seq(
            s.name,
            s.population
          ))
          .where(_.code <=> t.code)
          .orderBy(_.population.desc)
          .limit(5)
          .asColumn
          .as("cities")
      ))
      .where(_.code === code.toUpperCase)
      .runHeadOpt
      .map(jsonOpt(_))
  }

  // Optional WHERE conditions
  def selectOptional = Action.async { request =>
    val params = request.queryString.map(p => p._1 -> p._2(0))
    sql
      .select(country)
      .colsJson(t => Seq(
        t.code,
        t.name,
        t.continent,
        t.region,
        t.population
      ))
      .whereOpt(t => Seq(  // optional filters
        t.continent === params.get("cont"),
        t.region === params.get("region"),
        t.population > params.get("pop_gt").map(_.toInt),
        t.population < params.get("pop_lt").map(_.toInt)
      ))
      .orderBy(_.name.asc)
      .limit(10)
      .run
      .map(jsonList(_))
  }

  // Complex WHERE with AND/OR logic
  def selectAndOr(cont: String) = Action.async {
    sql
      .select(country)
      .colsJson(t => Seq(
        t.code,
        t.name,
        t.continent,
        t.region,
        t.population,
        t.surfaceArea,
        t.lifeExpectancy,
        t.gnp
      ))
      .where(t => Seq(
        t.continent === cont,
        Or(
          And(
            t.population > 20000000,
            t.surfaceArea > 500000
          ),
          And(
            t.lifeExpectancy > 65,
            t.gnp > 150000
          )
        )
      ))
      .orderBy(t => Seq(
        t.population.desc,
        t.lifeExpectancy.desc
      ))
      .limit(10)
      .run
      .map(jsonList(_))
  }

  // Aggregate functions
  def selectPopulation(cont: String) = Action.async {
    sql
      .select(country)
      .colsJson(t => Seq(
        "count" -> Count.all,
        "avg" -> Agg.avg(t.population),
        "max" -> Agg.max(t.population),
        "min" -> Agg.min(t.population)
      ))
      .where(_.continent === cont)
      .runHead
      .map(jsonObj(_))
  }
}












