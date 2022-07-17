package tutorial.webapp

import scala.scalajs.js.annotation.JSExportTopLevel
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.document
import org.scalajs.dom.window
import tutorial.webapp.facade.node.NodeGlobals.process

import scala.scalajs.js

object AppWindow {
  def main(): Unit = {
    window.addEventListener(
      "DOMContentLoaded",
      (_ => {
        val replaceText = (selector: String, text: String) => {
          val element = document.getElementById(selector)
          if (element != null) element.innerText = text
        }

        js.Array("chrome", "node", "electron")
          .foreach(`type` =>
            replaceText(
              s"${`type`}-version",
              process.versions.get(`type`).orNull
            )
          )
      }): js.Function1[Event, Unit]
    )
  }
}
