package demo.responses

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.http.HttpEntity
import org.apache.pekko.util.ByteString
import slinq.pg.api.Jsonb

// Responses for Play Json.

trait PlayJsonDemo {

  def rsp(obj: JsValue, code: Int) = Result(
    header = ResponseHeader(code, Map.empty),
    body = HttpEntity.Strict(
      ByteString(Json.stringify(obj)),
      Some("application/json")
    )
  )
  
  val jsonObj: JsValue => Result = obj => rsp(obj, 200)
  val jsonList: List[JsValue] => Result = list => rsp(JsArray(list), 200)
  val jsonSuccess: Any => Result = _ => jsonObj(Json.obj("success" -> true))

  val jsonOpt: Option[JsValue] => Result = {
    case Some(obj) => jsonObj(obj)
    case None => rsp(Json.obj("message" -> "not found"), 404)
  }
  
  val jsonError: Throwable => Result = e => {
    rsp(Json.obj("error" -> e.getMessage), 500)
  }
}