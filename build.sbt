import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.interface.ModuleSplitStyle

enablePlugins(ScalaJSPlugin)

name := "scalajs-electron-quick-start"
scalaVersion := "2.13.8"

// Main classes for Electron processes
scalaJSModuleInitializers +=
  ModuleInitializer
    .mainMethodWithArgs("quickstart.Main", "main")
    .withModuleID("main")
scalaJSModuleInitializers +=
  ModuleInitializer
    .mainMethodWithArgs("quickstart.Preload", "main")
    .withModuleID("preload")
scalaJSModuleInitializers +=
  ModuleInitializer
    .mainMethodWithArgs("quickstart.Renderer", "main")
    .withModuleID("renderer")

// Suppress meaningless 'multiple main classes detected' warning
Compile / mainClass := None

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.2.0"

fastOptJS / scalaJSLinkerConfig ~= {
  _.withModuleKind(ModuleKind.CommonJSModule)
    .withModuleSplitStyle(
      ModuleSplitStyle.SmallModulesFor(List("quickstart"))
    )
}

fullOptJS / scalaJSLinkerConfig ~= {
  _.withModuleKind(ModuleKind.CommonJSModule)
}

//TODO setup tests https://www.electronjs.org/docs/latest/tutorial/automated-testing
