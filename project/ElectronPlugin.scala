import sbt.AutoPlugin
import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.fastLinkJS
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.fullLinkJS
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSLinkerOutputDirectory
import org.scalajs.sbtplugin.Stage

import scala.collection.immutable.ListSet
import scala.sys.process.Process

//TODO split off the renderer to separate project
//TODO either do plugin work in root module or separate module that only does gluing
object ElectronPlugin extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  object autoImport {
    val electronInstall: TaskKey[Unit] = taskKey[Unit]("")
    val electronStart: TaskKey[Unit] = taskKey[Unit]("")
  }

  import autoImport._

  sealed trait Stage
  object Stage {
    case object FullOpt extends Stage
    case object FastOpt extends Stage
  }

  private def cmd(name: String) = sys.props("os.name").toLowerCase match {
    case os if os.contains("win") => "cmd" :: "/c" :: name :: Nil
    case _                        => name :: Nil
  }

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(perConfigSettings) ++
      inConfig(Test)(perConfigSettings)

  private lazy val perConfigSettings: Seq[Setting[_]] = Def.settings(
    electronInstall := {
      //TODO copy index.html
      //TODO copy styles.css
      //TODO copy package.json
      //TODO copy package-lock.json
      //TODO run npm install
      val s = streams.value

      val targetDir = (electronInstall / crossTarget).value

      val lockFile = "package-lock.json"
      val filesToCopy = ListSet(
        "index.html",
        "styles.css",
        "package.json",
        lockFile
      )

      filesToCopy
        .foreach(file =>
          IO.copyFile(baseDirectory.value / file, targetDir / file)
        )

      Process(cmd("npm") ::: "install" :: Nil, targetDir) ! s.log

      IO.copyFile(targetDir / lockFile, baseDirectory.value / lockFile)
    },
    electronInstall / crossTarget := {
      crossTarget.value /
        "electron" /
        (if (configuration.value == Compile) "main" else "test")
    }
  ) ++
    perScalaJSStageSettings(Stage.FastOpt) ++
    perScalaJSStageSettings(Stage.FullOpt)

  private def perScalaJSStageSettings(stage: Stage): Seq[Setting[_]] = {
    val stageTask = stage match {
      case Stage.FastOpt => fastLinkJS
      case Stage.FullOpt => fullLinkJS
    }

    Def.settings(
      stageTask / electronStart := Def.task {
        val s = streams.value

        stageTask.value

        val linkerOutputDirectory =
          (stageTask / scalaJSLinkerOutputDirectory).value

        val targetDir = (electronInstall / crossTarget).value

        linkerOutputDirectory
          .listFiles()
          .foreach(file => IO.copyFile(file, targetDir / file.name))

        Process(cmd("npm") ::: "start" :: Nil, targetDir) ! s.log
//
//        s.log.info(s"${linkerOutputDirectory}")
//
//        val report = stageTask.value
//
//        s.log.info(s"${report.data}")
//
//        s.log.info((electronInstall / crossTarget).value.getAbsolutePath)

      }.value
    )
  }
}
