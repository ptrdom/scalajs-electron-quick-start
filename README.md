# scalajs-electron-quick-start

**[electron-quick-start](https://github.com/electron/electron-quick-start) conversion to [Scala.js](https://www.scala-js.org/)**

> :warning: This template is more of an experiment and does not provide the best workflows for developing
> and building applications. If you need proper dev server implementation with hot reload and module bundler support,
> please check out following plugins:
> 
> - https://github.com/ptrdom/scalajs-esbuild?tab=readme-ov-file#electron-plugin
> - https://github.com/ptrdom/scalajs-vite?tab=readme-ov-file#electron
> 
> Recommendation is to try out esbuild, because esbuild is faster than vite (rollup).

## How to run

Requires `npm` to be installed in the system.

Run `sbt app/electronStart`.

## Design

The application is split into five `sbt` modules to fit a project structure that works well with Scala.js and the
[security guidelines](https://www.electronjs.org/docs/latest/tutorial/security) outlined in Electron documentation:
 
- `app`
  - Combines Scala.js output of other modules with additional resources into an Electron application.
  - Implements a simple Electron `sbt` plugin with following tasks:
    - `app/electronInstall` - copies over resources (`index.html`, `package.json`, `package-lock.json`, `styles.css`) 
      to target directory and runs `npm install`.
    - `app/electronCompile` - compiles Scala.js modules, copies output to target directory.
    - `app/electronStart` - runs `npm start` on target directory.
- `main`
  - Contains `main` process of the [Electron Process Model](https://www.electronjs.org/docs/latest/tutorial/process-model).
  - Can use `Node.js` APIs and `CommonJS` modules.
- `preload`
  - Contains `preload` script.
  - Can use a [polyfilled subset](https://www.electronjs.org/docs/latest/tutorial/sandbox#preload-scripts) of `Node.js`
    APIs, so it does use `CommonJS` modules, but can only `require` a subset of them.
- `node-shared`
  - Contains Scala.js facades that can be shared between `main` and `preload` modules.
- `renderer`
  - Contains `renderer` process of the Electron Process Model. 
  - Because it used for producing 
  web content and browsers do not have built-in support `CommonJS` modules, it is built separately
  as a single bundle.

`scalaJSStage` setting in `main`, `preload` and `renderer` modules can be used to toggle between `FastLinkJS` and `FullLinkJS` output.

## License

This software is licensed under the MIT license
