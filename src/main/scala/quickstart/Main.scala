package quickstart

import quickstart.facade.electron.BrowserWindow
import quickstart.facade.electron.BrowserWindowConfig
import quickstart.facade.electron.ElectronGlobals.app
import quickstart.facade.electron.WebPreferences
import quickstart.facade.node.NodeGlobals.__dirname
import quickstart.facade.node.NodeGlobals.process
import quickstart.facade.node.Path

import scala.scalajs.js
import scala.scalajs.js.|

object Main extends App {

  def createWindow(): Unit = {
    val mainWindow = new BrowserWindow(new BrowserWindowConfig {
      override val height = 600
      override val width = 800
      override val webPreferences = new WebPreferences {
        override val preload = Path.join(__dirname, "preload.js")
      }
    })
    mainWindow.loadFile("index.html")
    mainWindow.webContents.openDevTools()
  }

  app
    .whenReady()
    .`then`((_ => {
      createWindow()
      app.on(
        "activate",
        () => {
          if (BrowserWindow.getAllWindows().length == 0) createWindow()
        }
      )
    }): js.Function1[Unit, Unit | js.Thenable[Unit]])
  app.on(
    "window-all-close",
    () => {
      if (process.platform != "darwin") app.quit()
    }
  )
}
