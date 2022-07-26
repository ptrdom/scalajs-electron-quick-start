import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.interface.ModuleSplitStyle
import org.scalajs.sbtplugin.Stage.FullOpt

name := "scalajs-electron-quick-start"

lazy val `scalajs-electron-quick-start` = (project in file("."))
  .aggregate(app, main, renderer)

def commonSettings = Seq(
  scalaVersion := "2.13.8"
)

def commonScalajsSettings = Seq(
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.2.0"
)

val app = (project in file("app"))
  .enablePlugins(ElectronPlugin)
  .settings(
    commonSettings,
    mainProject := main,
    rendererProject := renderer
  )

lazy val main = (project in file("main"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    commonScalajsSettings,
    scalaJSModuleInitializers := Seq(
      ModuleInitializer
        .mainMethodWithArgs("quickstart.Main", "main")
        .withModuleID("main"),
      ModuleInitializer
        .mainMethodWithArgs("quickstart.Preload", "main")
        .withModuleID("preload")
    ),
    // Suppress meaningless 'multiple main classes detected' warning
    Compile / mainClass := None,
    Compile / fastLinkJS / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("quickstart"))
        )
    },
    Compile / fullLinkJS / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
        .withClosureCompiler(false)
    }
  )

lazy val renderer = (project in file("renderer"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    commonScalajsSettings,
    scalaJSModuleInitializers := Seq(
      ModuleInitializer
        .mainMethodWithArgs("quickstart.Renderer", "main")
        .withModuleID("renderer")
    )
  )
