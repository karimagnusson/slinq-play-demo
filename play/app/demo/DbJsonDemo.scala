package demo.responses

import play.api._
import play.api.mvc._
import play.api.http.HttpEntity
import org.apache.pekko.util.ByteString
import slinq.pg.api.Jsonb

// Responses for json delivered as a string from the database.

trait DbJsonDemo {

  def rsp(jsString: String, code: Int) = Result(
    header = ResponseHeader(code, Map.empty),
    body = HttpEntity.Strict(
      ByteString(jsString),
      Some("application/json")
    )
  )
  
  val notFound = """{"message": "not found"}"""
  val okTrue = """{"ok": true}"""

  val jsonObj: Jsonb => Result = obj => rsp(obj.value, 200)
  val jsonList: List[Jsonb] => Result = { list =>
    rsp("[%s]".format(list.map(_.value).mkString(",")), 200)
  }
  val jsonSuccess: Any => Result = _ => rsp("""{"success": true}""", 200)

  val jsonOpt: Option[Jsonb] => Result = {
    case Some(obj) => jsonObj(obj)
    case None => rsp("""{"message": "not found"}""", 404)
  }

  val jsonError: Throwable => Result = e => {
    rsp("""{"error": "%s"}""".format(e.getMessage), 500)
  }
}