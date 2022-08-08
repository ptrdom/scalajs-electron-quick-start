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
  // Create the browser window.
  def createWindow(): Unit = {
    val mainWindow = new BrowserWindow(new BrowserWindowConfig {
      override val height = 600
      override val width = 800
      override val webPreferences = new WebPreferences {
        override val preload = Path.join(__dirname, "preload.js")
      }
    })

    // and load the index.html of the app.
    mainWindow.loadFile("index.html")

    // Open the DevTools.
    // mainWindow.webContents.openDevTools()
  }

  // This method will be called when Electron has finished
  // initialization and is ready to create browser windows.
  // Some APIs can only be used after this event occurs.
  app
    .whenReady()
    .`then`((_ => {
      createWindow()

      // On macOS it's common to re-create a window in the app when the
      // dock icon is clicked and there are no other windows open.
      app.on(
        "activate",
        () => {
          if (BrowserWindow.getAllWindows().length == 0) createWindow()
        }
      )
    }): js.Function1[Unit, Unit | js.Thenable[Unit]])

  // Quit when all windows are closed, except on macOS. There, it's common
  // for applications and their menu bar to stay active until the user quits
  // explicitly with Cmd + Q.
  app.on(
    "window-all-close",
    () => {
      if (process.platform != "darwin") app.quit()
    }
  )
}
