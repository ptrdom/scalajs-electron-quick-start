package tutorial.webapp

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits._
import tutorial.webapp.Electron.app
import tutorial.webapp.facade.node.NodeGlobals.__dirname
import tutorial.webapp.facade.node.NodeGlobals.process
import tutorial.webapp.facade.node.Path.path

import scala.scalajs.js.Thenable.Implicits._

object TutorialApp {

  def createWindow(): Unit = {
    val mainWindow = new BrowserWindow(new BrowserWindowConfig {
      override val height = 600
      override val width = 800
      override val webPreferences = new WebPreferences {
        override val preload =
          s"${__dirname}\\AppWindow.js"
      }
    })
    mainWindow.loadFile("index.html")
//    mainWindow.webContents.openDevTools()
  }

  def main(): Unit = {
    app
      .whenReady()
      .foreach { _ =>
        createWindow()
        app.on(
          "activate",
          () => {
            if (BrowserWindow.getAllWindows().length == 0) createWindow()
          }
        )
      }
    app.on(
      "window-all-close",
      () => {
        if (process.platform != "darwin") app.quit()
      }
    )
  }
}
