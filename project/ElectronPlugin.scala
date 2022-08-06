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
      taskKey[Unit](
        "Copies over resources to target directory and runs `npm install`"
      )
    val electronCompile: TaskKey[Unit] =
      taskKey[Unit](
        "Compiles main and renderer modules, copies output to target directory."
      )
    val electronStart = taskKey[Unit]("Runs `npm start` on target directory.")
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

      FileFunction.cached(
        streams.value.cacheDirectory /
          "electron" /
          (if (configuration.value == Compile) "main" else "test"),
        inStyle = FilesInfo.hash
      ) { filesToCopy =>
        filesToCopy
          .filter(_.exists())
          .foreach(file => IO.copyFile(file, targetDir / file.getName))

        Process(cmd("npm") ::: "install" :: Nil, targetDir)
          .run(eagerIO(s.log))
          .exitValue()

        IO.copyFile(targetDir / lockFile, baseDirectory.value / lockFile)

        Set.empty
      }(
        ListSet(
          "index.html",
          "styles.css",
          "package.json",
          lockFile
        ).map(baseDirectory.value / _)
      )
    },
    electronCompile := {
      electronInstall.value

      val targetDir = (electronInstall / crossTarget).value

      emitModules.value.foreach { linkerOutputDirectory =>
        linkerOutputDirectory
          .listFiles()
          .foreach(file => IO.copyFile(file, targetDir / file.name))
      }
    },
    (Compile / compile) := ((Compile / compile) dependsOn electronCompile).value,
    electronStart := {
      electronCompile.value

      val s = streams.value

      val targetDir = (electronInstall / crossTarget).value

      Process(cmd("npm") ::: "start" :: Nil, targetDir)
        .run(eagerIO(s.log))
        .exitValue()
    }
  )
}
