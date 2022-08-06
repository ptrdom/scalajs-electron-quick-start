package quickstart

import org.scalajs.dom.Event
import org.scalajs.dom.document
import org.scalajs.dom.window
import quickstart.facade.node.NodeGlobals.process

import scala.scalajs.js

/** All of the Node.js APIs are available in the preload process.
  * It has the same sandbox as a Chrome extension.
  */
object Preload extends App {
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
