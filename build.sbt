import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.interface.ModuleSplitStyle

enablePlugins(ScalaJSPlugin)

name := "Scala.js Tutorial"
scalaVersion := "2.13.8"

// This is an application with a main method
scalaJSUseMainModuleInitializer := false
scalaJSModuleInitializers +=
  ModuleInitializer
    .mainMethodWithArgs("quickstart.Main", "main")
    .withModuleID("main")
scalaJSModuleInitializers +=
  ModuleInitializer
    .mainMethodWithArgs("quickstart.Preload", "main")
    .withModuleID("preload")

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.2.0"
libraryDependencies += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.0.0"

// Add support for the DOM in `run` and `test`
//TODO use selenium env for tests
//jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

// uTest settings
libraryDependencies += "com.lihaoyi" %%% "utest" % "0.7.4" % "test"
testFrameworks += new TestFramework("utest.runner.Framework")

fastOptJS / scalaJSLinkerConfig ~= {
  _.withModuleKind(ModuleKind.CommonJSModule)
    .withModuleSplitStyle(
      ModuleSplitStyle.SmallModulesFor(List("quickstart"))
    )
}

fullOptJS / scalaJSLinkerConfig ~= {
  _.withModuleKind(ModuleKind.CommonJSModule)
}
