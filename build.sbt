import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.interface.ModuleSplitStyle

name := "scalajs-electron-quick-start"

lazy val `scalajs-electron-quick-start` = (project in file("."))
  .aggregate(main, renderer)

def commonSettings = Seq(
  scalaVersion := "2.13.8",
  libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.2.0"
)

lazy val main = (project in file("main"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings)
  .settings(
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
  .settings(commonSettings)
  .settings(
    scalaJSModuleInitializers := Seq(
      ModuleInitializer
        .mainMethodWithArgs("quickstart.Renderer", "main")
        .withModuleID("renderer")
    )
  )

//TODO setup tests https://www.electronjs.org/docs/latest/tutorial/automated-testing
