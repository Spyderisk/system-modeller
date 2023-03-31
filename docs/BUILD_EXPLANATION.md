# How the build works

## Web-Client

### Outline:

![Simple outline of how the elements interact with each other](JS_BUILD_DIAGRAM.png)

### Tools:

Gradle - used to orchestrate the build, downloads the newest versions of node and yarn where necessary [Find out more](https://gradle.org/ "Gradle")

Node.js - serverside javascript, used to run the other packages such as webpack and babel. Also used as a proxy server in the development build [Find out more](https://nodejs.org/en/ "NodeJS")

Yarn - the JS package manager we are using (NOT npm). This uses all of the same packages as npm but has extra features for selecting correct versions and for security of open source packages. [Find out more](https://yarnpkg.com/lang/en/ "Yarn")

Webpack - packages the Javascript into bundle files and css/scss files and dumps them in webapp/dist [Find out more](https://webpack.js.org/ "Webpack")

Express - used for the webpack-hot-middleware for development, allowing live changes to the src files to be turned into a new webpack after every save [Find out more](https://expressjs.com/ "Express")

Babel - Used to compile to 'browser-compatible' javascript [Find out more](https://babeljs.io/ "Babel")

React - Framework used to generate dynamic UI elements on the webpage, enabling only the required sections to change when an update occurs [Find out more](https://reactjs.org/ "ReactJS")

### How gradle works:
#### gradle :src:main:webapp:start [Local testing with live changes]
NOTE: This requires 'gradle bootDev' to be run to kick off the Springboot server separately.
Calling 'gradle :src:main:webapp:start' starts a node.js server as defined in webapp/server.js.
This server.js file kicks off the express web framework as a way for creating the webpack with the dev config and for running a proxy.

#### gradle bootTest [Local testing]
Depends on :src:main:webapp:bundle.
bund is an alias for a Yarntask (contained in package.json) which ends up calling webpack with the test config.

#### gradle build | gradle war [Production Build]
Any of these gradle commands depend on :src:main:webapp:bundleProd.
bundleProd is an alias for a Yarntask (contained in package.json) which ends up calling webpack with the prod config.
When war is run, the resultant war file contains the bundled webapp inside the static/dist directory.

#### webapp gradle utilities

To add or remove packages during development, the `src/main/webapp/build.gradle` file can be adjusted to run yarn
commands on the compiled bundle. some example commands are described below:

- `gradle addPackage -P packName="react-dom@4.6.6"`
- `gradle addPackage -P packName="lodash" -P dev="true"`
- `gradle removePackage -P packName="react-dom"`
- `gradle install`

After a commit that has changed the contents of `src/main/webapp/package.json`, a `gradle install`
is necessary to update the local cache. This runs a `yarn install` which removes any unnecessary
packages and installs the packages in `package.json`, in addition to any new additions.
`gradle build` only rebuilds the cache of webapp from scratch if a new clean environment is found.

To create the webapp environment from scratch follow the steps below:

1. `cd $project_root/src/main/webapp`
2. `gradle stopNode` **Note:** _This will fail if node was not already running_
3. `gradle clean`
3. remove contents of `src/main/webapp/node_modules`
4. remove contents of `src/main/webapp/.gradle`
5. `gradle install`

### How webpack works:
Webpack runs on node.js and is executed with a config file (specified by which gradle command was called).
This config file (written in js) contains all of the parameters that webpack needs in order to bundle together the project into a few small files that can be sent to the web browser and run.
There are a few steps in doing this, it has to compile code into standard browser compatible javascript, bundle it into .bundle.js files and copy it into the destination directory.

#### Compilation (using Babel)
Babel is used to compile the source code into runnable javascript that works on web browsers.
Webpack defines the babel-loader as the dependency it wil use to compile.
Babel is given a list of options containing presets and plugins to define how it should work.
We are using three presets: env (this is the standard preset for compiling modern JS), react (special react javascript features) and stage-0 (used for some strange syntax such as :: and ...).

#### Plugins
Webpack allow plugins to be installed in order to add functionality to the packaging of the bundle such as uglifying code, custom chunking algorithms and hot module replacement.

#### Browser Targets
The target browsers are defined using the autoprefixer package, and instansiated with a list of browser specifications.
This is used to decide which version of javascript should be compiled to so that the codebase satisfies all required browsers.

### How the webpack ends up on the deployed server:
Once the webapp has finished building, the webpacked bundles are copied into the resources directory of the main system-modeller project. This is then kept when the war file is built so that Tomcat can supply the webapp to the client when they access the webserver.

### List of yarn dependencies:
#### Standard:

* axios  -  Manages HTTP requests from node.js, deals with sending/recieving all requests to/from the server
* classnames  -  Allows for easy assigning of CSS classes to react components
* es6-promise  -  A polyfill (essentially inserting a library if the browser doesn't natively have it) for the promise API for IE9
* jquery  -  Adds lots of ease-of-use development functionality such as HTML doc traversal and event handling
* jsplumb  -  used to create connected visual elements for graphs/diagrams, we use it on the canvas for assets and relations
* prop-types  -  Adds runtime type checking for react component properties
* react  -  Front-end GUI framework that works around updating individual components only when required
* react-bootstrap  -  A React version of the popular bootstrap libray, contains lots of useful GUI elements
* react-bootstrap-table-next  -  A tool for creating simple and powerful tables in react
*react-bootstrap-table2-filter  -  Extra functionality for bootstrap-table that allows for the table to be filtered
* react-bootstrap-table2-paginator  -  Extra functionality for bootstrap-table that allows for the table to be broken up into pages
* react-dom  -  Adds DOM (Document Object Model) functionality for referencing and managing different DOM nodes in the HTML page
* react-hot-loader  -  Allows hotswapping of react components without losing state, used for the webpack updating in the dev build
* react-portal  -  Portals in a new top level react tree for making temporary GUI elements easier to handle (loading screens/menus/modals)
* react-redux  -  Predictable state container for react apps, allows uniformity across different build environments (client/server/dev)
* react-rnd  -  Functionality for moveable, resizable elements, used for the threat explorer and similar
* redux  -  Predictable state container for javascript, allows uniformity across different build environments (client/server/dev)
* redux-thunk  -  Uses 'thunks' to allow for delayed execution of redux actions and functions

#### Dev:

* autoprefixer  -  Automatically applies prefixes to CSS classes
* @babel/core  -  the core library for the babel compiler, for turning react and modern javascript into versions that work with browsers
* @babel/plugin-transform-runtime  -  Used by babel for linking and including other js files and packages
* @babel/preset-env  -  Babel preset for standard javascript environments, required to get babel to function for basic (modern) JS
* @babel/preset-react  -  Babel preset for React javascript functionality, required for babel as we use React
* connect-history-api-fallback  -  Used for a fallback to a specific page if anything goes wrong, used in the dev build in server.js
* css-loader  -  Used to load in css files into webpack, we use a few css files (not just scss)
* express  -  Serverside JS, we use only for running the dev build with the hot loader
* file-loader  -  plugin for loading files into webpack
* html-webpack-plugin  -  Plugin for creating simple HTML files in webpack, used in the dev build for the templates
* http-proxy-middleware  -  Makes a http proxy server for use with express, this is for the port 3000 version for live changes
* sass-loader  -  Laods and converts scss to css in webpack
* style-loader  -  Allows injection of styles to the DOM with the <style> tag
* webpack  -  used to package the javascript application into bundles that can be sent from the webserver to the client
* webpack-cli  -  Command line interface to get webpack to work, we don't use this but it's a requirement to get webpack v4 to work
* webpack-dev-middleware  -  Used as part of the webpack live updates
* webpack-hot-middleware  -  Used in development to create a new webpack every time a change is made to the source code for quick and easy development

#### Other:

font-awesome (included in template AND on the servers resource files) - Font used for the webpage, includes lots of very useful symbols and icons
