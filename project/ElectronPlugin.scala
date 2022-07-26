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
import sbt.nio.Keys.watchOnIteration
import sbt.nio.Keys.watchOnTermination

import scala.collection.immutable.ListSet
//import scala.sys.process._

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

  //TODO rework for Java process
//  def eagerIO(log: ManagedLogger) = {
//    val toInfoLog = (is: InputStream) => {
//      scala.io.Source
//        .fromInputStream(is)
//        .getLines
//        .foreach(msg => log.info(msg))
//      is.close()
//    }
//    val toErrorLog = (is: InputStream) => {
//      scala.io.Source
//        .fromInputStream(is)
//        .getLines
//        .foreach(msg => log.error(msg))
//      is.close()
//    }
//    new ProcessIO(
//      _.close(),
//      toInfoLog,
//      toErrorLog
//    )
//  }

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(perConfigSettings) ++
      inConfig(Test)(perConfigSettings)

  private lazy val perConfigSettings: Seq[Setting[_]] = {
    var electronStartProcess: Option[Process] = None
    def terminateProcess() = {
      electronStartProcess.foreach { process =>
        //required for win
        process
          .descendants()
          .forEach(process => process.destroy())
        process.destroy()
      }
    }
    Seq(
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

        val pb = new ProcessBuilder(cmd("npm") ::: "install" :: Nil: _*)
        pb.directory(targetDir)
        val process = pb.start()
        process.waitFor()
        process.exitValue()

        //      val sslContextField = classOf[Configs].getDeclaredField("sslContext")
        //      sslContextField.setAccessible(true)
        //      val configsField = classOf[CosmosClientBuilder].getDeclaredField("configs")
        //      configsField.setAccessible(true)
        //      sslContextField.set(configsField.get(builder), sslContext)

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

        //TODO use java process API to kill descendants
        //https://bugs.openjdk.org/browse/JDK-4770092
        //https://stackoverflow.com/questions/5879791/how-to-kill-subprocesses-of-a-java-process
        //https://stackoverflow.com/questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
        //https://stackoverflow.com/questions/32705857/cant-kill-child-process-on-windows

        //      Process(cmd("npm") ::: "start" :: Nil, targetDir)
        //        .run(eagerIO(s.log))
        terminateProcess()

        val pb = new ProcessBuilder(cmd("npm") ::: "start" :: Nil: _*)
        pb.directory(targetDir)
        val process = pb.start()
        electronStartProcess = Some(process)

        //      emitModules.value.foreach {}
        //      projectToRef(mainProject.value)
        //      (mainProject / fastLinkJS).value
        //      (projectToRef(mainProject.value) / fastLinkJS).value
        //      (projectToRef(rendererProject.value) / fastLinkJS).value
        //      Def.task(())
      },
      watchOnTermination := { (action, count, command, state) =>
        terminateProcess()
        state
      },
      (Compile / compile) := ((Compile / compile) dependsOn electronStart).value
    )
  }
}
