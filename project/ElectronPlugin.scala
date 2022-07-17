import sbt.AutoPlugin
import sbt._
import Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.fastLinkJS
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.fullLinkJS
import org.scalajs.sbtplugin.Stage

object ElectronPlugin extends AutoPlugin {

  override lazy val requires = ScalaJSPlugin

  object autoImport {
    val electronInstall = settingKey[Attributed[File]]("")
    val electronStart = taskKey[Unit]("")

    val hello = taskKey[Unit]("say hello")
  }

  import autoImport._

  sealed trait Stage
  object Stage {
    case object FullOpt extends Stage
    case object FastOpt extends Stage
  }

  override lazy val projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(perConfigSettings) ++
      inConfig(Test)(perConfigSettings)

  private lazy val perConfigSettings: Seq[Setting[_]] =
    perScalaJSStageSettings(Stage.FastOpt) ++
      perScalaJSStageSettings(Stage.FullOpt)

  private def perScalaJSStageSettings(stage: Stage): Seq[Setting[_]] = {
    val stageTask = stage match {
      case Stage.FastOpt => fastLinkJS
      case Stage.FullOpt => fullLinkJS
    }

    Seq(
      stageTask / hello := Def.task {
        stageTask.value
      }.value
    )
  }
}
