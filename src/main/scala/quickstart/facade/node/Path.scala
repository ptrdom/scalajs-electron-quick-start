package quickstart.facade.node

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Path {
  @js.native
  @JSImport("path")
  def path: Path = js.native
}

@js.native
trait Path extends js.Object {
  def join(paths: String*): String = js.native
}
