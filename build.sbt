import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.interface.ModuleSplitStyle

name := "scalajs-electron-quick-start"

lazy val `scalajs-electron-quick-start` = (project in file("."))
  .aggregate(app, main, preload, `node-shared`, renderer)

def commonSettings = Seq(
  scalaVersion := "2.13.8"
)

def commonDomSettings = Seq(
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0"
)

val app = (project in file("app"))
  .enablePlugins(ElectronPlugin)
  .settings(
    commonSettings,
    mainProject := main,
    preloadProject := preload,
    rendererProject := renderer
  )

lazy val main = (project in file("main"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(`node-shared`)
  .settings(
    commonSettings,
    scalaJSModuleInitializers := Seq(
      ModuleInitializer
        .mainMethodWithArgs("quickstart.Main", "main")
        .withModuleID("main")
    ),
    Compile / fastLinkJS / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("quickstart"))
        )
    },
    Compile / fullLinkJS / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    }
  )

lazy val preload = (project in file("preload"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(`node-shared`)
  .settings(
    commonSettings,
    commonDomSettings,
    scalaJSModuleInitializers := Seq(
      ModuleInitializer
        .mainMethodWithArgs("quickstart.Preload", "main")
        .withModuleID("preload")
    ),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    }
  )

lazy val `node-shared` = (project in file("node-shared"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings)

lazy val renderer = (project in file("renderer"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    commonSettings,
    commonDomSettings,
    scalaJSModuleInitializers := Seq(
      ModuleInitializer
        .mainMethodWithArgs("quickstart.Renderer", "main")
        .withModuleID("renderer")
    )
  )
