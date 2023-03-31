const express = require('express');
const webpack = require('webpack');
const config = require('./webpack/webpack.dev.config');
const settings = require('./config/config');
const history = require('connect-history-api-fallback');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();
const compiler = webpack(config);
let basehost = '0.0.0.0';
const endpoint = JSON.parse(settings.API_END_POINT);

const isWin = /^win/.test(process.platform);
if (isWin) {
    console.log("Running on Windows OS, using 'localhost' for base host.");
    basehost = "localhost";
}

app.use(history());

app.use(createProxyMiddleware(endpoint, {
    target: 'http://localhost:8081', changeOrigin: true, autoRewrite: true
}));

app.use(require('webpack-dev-middleware')(compiler, {
    publicPath: config.output.publicPath,
    stats: {
        colors: true
    }
}));

app.use(require('webpack-hot-middleware')(compiler));

let server = app.listen(3000, basehost, (err) => {
    if (err) {
        console.log(err);
        return;
    }

    console.log('Listening at http://' + basehost + ':3000');
    console.log('Please wait. Compiling app for first time... (~30 seconds)')
});
server.timeout = 600000;
