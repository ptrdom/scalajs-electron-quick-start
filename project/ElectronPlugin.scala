import java.io.InputStream

import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.fastLinkJS
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.fullLinkJS
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSLinkerOutputDirectory
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSStage
import org.scalajs.sbtplugin.Stage
import sbt.AutoPlugin
import sbt.Def.Initialize
import sbt.Def.task
import sbt.Def.taskDyn
import sbt.Keys._
import sbt.Project.projectToRef
import sbt._
import sbt.internal.util.ManagedLogger

import scala.collection.immutable.ListSet
import scala.sys.process.Process
import scala.sys.process.ProcessIO

object ElectronPlugin extends AutoPlugin {

  object autoImport {
    val mainProject = settingKey[Project]("Project for main and preload.")
    val rendererProject = settingKey[Project]("Project for renderer.")

    val electronInstall: TaskKey[Unit] =
      taskKey[Unit]("Copies over web resources and runs `npm install`")
    val electronStart = taskKey[Unit](
      "Compiles main and renderer modules, then runs `npm start`. " +
        "Requires 'electronInstall' to be ran before for resource setup."
    )
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

  private def onScalaJSStage[A](
      onFastOpt: => Initialize[A],
      onFullOpt: => Initialize[A]
  ): Initialize[A] =
    Def.settingDyn {
      scalaJSStage.value match {
        case Stage.FastOpt => onFastOpt
        case Stage.FullOpt => onFullOpt
      }
    }

  private def eagerIO(log: ManagedLogger) = {
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

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(perConfigSettings) ++
      inConfig(Test)(perConfigSettings)

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
    },
    (Compile / compile) := ((Compile / compile) dependsOn electronStart).value
  )
}
