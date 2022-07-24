import java.io.InputStream

import sbt.AutoPlugin
import sbt._
import Keys._
import org.scalajs.linker.interface.Report
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.fastLinkJS
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.fullLinkJS
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSLinkerOutputDirectory
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSStage
import org.scalajs.sbtplugin.Stage
import sbt.Def.Initialize
import sbt.Def.task
import sbt.Def.taskDyn
import sbt.Project.projectToRef
import sbt.internal.util.ManagedLogger

import scala.collection.immutable.ListSet
import scala.sys.process.Process
import scala.sys.process.ProcessIO

object ElectronSplitPlugin extends AutoPlugin {

  object autoImport {
    val mainProject = settingKey[Project]("")
    val rendererProject = settingKey[Project]("")

    val electronInstall: TaskKey[Unit] = taskKey[Unit]("")
    val electronStart = taskKey[Unit]("")
  }

  import autoImport._

  private def cmd(name: String) = sys.props("os.name").toLowerCase match {
    case os if os.contains("win") => "cmd" :: "/c" :: name :: Nil
    case _                        => name :: Nil
  }

  private lazy val emitModules = taskDyn {
    lazy val filter = ScopeFilter(
      inProjects(
        projectToRef(mainProject.value),
        projectToRef(rendererProject.value)
      ),
      inConfigurations(Compile)
    )
    task(scalaJSTaskFiles.all(filter).value)
  }

  private lazy val scalaJSTaskFiles = onScalaJSStage(
    Def.task {
      fastLinkJS.value
      (fastLinkJS / scalaJSLinkerOutputDirectory).value
    },
    Def.task {
      fullLinkJS.value
      (fullLinkJS / scalaJSLinkerOutputDirectory).value
    }
  )

  def onScalaJSStage[A](
      onFastOpt: => Initialize[A],
      onFullOpt: => Initialize[A]
  ): Initialize[A] =
    Def.settingDyn {
      scalaJSStage.value match {
        case Stage.FastOpt => onFastOpt
        case Stage.FullOpt => onFullOpt
      }
    }

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(perConfigSettings) ++
      inConfig(Test)(perConfigSettings)

  def eagerIO(log: ManagedLogger) = {
    val toInfoLog = (is: InputStream) => {
      scala.io.Source
        .fromInputStream(is)
        .getLines
        .foreach(msg => log.info(msg))
      is.close()
    }
    val toErrorLog = (is: InputStream) => {
      scala.io.Source
        .fromInputStream(is)
        .getLines
        .foreach(msg => log.error(msg))
      is.close()
    }
    new ProcessIO(
      _.close(),
      toInfoLog,
      toErrorLog
    )
  }

  private lazy val perConfigSettings: Seq[Setting[_]] = Seq(
    electronInstall / crossTarget := {
      crossTarget.value /
        "electron" /
        (if (configuration.value == Compile) "main" else "test")
    },
    electronInstall := {
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

      Process(cmd("npm") ::: "install" :: Nil, targetDir)
        .run(eagerIO(s.log))
        .exitValue()

      IO.copyFile(targetDir / lockFile, baseDirectory.value / lockFile)
    },
    electronStart := {
      val s = streams.value

      val targetDir = (electronInstall / crossTarget).value

      emitModules.value.foreach { linkerOutputDirectory =>
        linkerOutputDirectory
          .listFiles()
          .foreach(file => IO.copyFile(file, targetDir / file.name))
      }

      Process(cmd("npm") ::: "start" :: Nil, targetDir)
        .run(eagerIO(s.log))
        .exitValue()

//      emitModules.value.foreach {}
      //      projectToRef(mainProject.value)
      //      (mainProject / fastLinkJS).value
      //      (projectToRef(mainProject.value) / fastLinkJS).value
      //      (projectToRef(rendererProject.value) / fastLinkJS).value
      //      Def.task(())
    }
    //TODO setup compilation
    // (Compile / compile) := ((Compile / compile) dependsOn electronStart).value
  )
}
