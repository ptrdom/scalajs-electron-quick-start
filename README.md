# scalajs-electron-quick-start

**[electron-quick-start](https://github.com/electron/electron-quick-start) conversion to [Scala.js](https://www.scala-js.org/)**

## How to run

Requires `npm` to be installed in the system.

Run `sbt app/electronStart`.

## Design

The application is split into three `sbt` modules:

- `app`
  - Combines Scala.js output of `main` and `renderer` modules with additional resources into an Electron application.
  - Implements a simple Electron `sbt` plugin with following tasks:
    - `app/electronInstall` - copies over resources (`index.html`, `package.json`, `package-lock.json`, `styles.css`) 
      to target directory and runs `npm install`.
    - `app/electronCompile` - compiles `main` and `renderer` modules, copies output to target directory.
    - `app/electronStart` - runs `npm start` on target directory.
- `main`
  - Contains `main` process of the Electron Process Model. Produces two Scala.js entry points -
  `main.js` and `preload.js`.
  - Because entry points can use `Node.js` APIs, `CommonJS` modules are used
  to share common code.
- `renderer`
  - Contains `renderer` process of the Electron Process Model. 
  - Because it used for producing 
  web content and browsers do not have built-in support `CommonJS` modules, it is built separately
  as a single bundle.

`scalaJSStage` setting in `main` and `renderer` modules can be used to toggle between `FastLinkJS` and `FullLinkJS` output.

## License

This software is licensed under the MIT license
