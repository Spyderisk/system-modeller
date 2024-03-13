const webpack = require("webpack");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const path = require("path");
const autoprefixer = require("autoprefixer");

const ROOT = path.resolve(__dirname, "../");
const SRC = path.resolve(ROOT, "app");
const NODE_MODULES = path.resolve(ROOT, "node_modules");
const BUILD = path.join(ROOT, "dist");

module.exports = {
    mode: 'development',
    devtool: "cheap-module-source-map",
    entry: {
        dashboard: ["react-hot-loader/patch", SRC + "/dashboard/index.js", "webpack-hot-middleware/client", "webpack/hot/dev-server"],
        modeller: ["react-hot-loader/patch", SRC + "/modeller/index.js", "webpack-hot-middleware/client", "webpack/hot/dev-server"],
        admin: ["react-hot-loader/patch", SRC + "/admin/index.js", "webpack-hot-middleware/client", "webpack/hot/dev-server"],
        domainManager: ["react-hot-loader/patch", SRC + "/domainManager/index.js", "webpack-hot-middleware/client", "webpack/hot/dev-server"]
    },
    output: {
        path: BUILD,
        filename: "[name].[contenthash].js",
        publicPath: "/"
        // publicPath: "http://" + basehost + ":3000/"
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                include: SRC,
                exclude: /node_modules/,
                use: [
                    {
                        loader: "babel-loader",
                        options: {
                            cacheDirectory: true,
                            presets: ["@babel/preset-react", "@babel/preset-env"],
                            plugins: ["react-hot-loader/babel", "@babel/plugin-transform-runtime"]
                        }
                    }
                ]
            },
            {
                test: /\.scss$/,
                include: SRC,
                exclude: /node_modules/,
                use: [
                    {
                        loader: 'style-loader',  // creates <style> nodes from JS
                    },
                    {
                        loader: 'css-loader',  // converts from CSS to CommonJS
                        options: {
                            importLoaders: 1,
                            // 0 => no loaders (default);
                            // 1 => postcss-loader;
                            // 2 => postcss-loader, sass-loader
                            modules: {
                                mode: "icss"  // need this for vars.scss ":export" statement to work
                            }
                        }
                    },
                    {
                        loader: "sass-loader"  // compiles SASS to CSS
                    }
                ],
            },
            {
                test: /\.css$/,
                include: [SRC, NODE_MODULES],
                use: [
                    {
                        loader: 'style-loader',  // creates <style> nodes from JS
                    },
                    {
                        loader: 'css-loader',  // converts from CSS to CommonJS
                    },
                    {
                        loader: "postcss-loader"
                    }
                ],
            }
        ]
    },
    resolve: {
        alias: { 'react-dom': '@hot-loader/react-dom'  }
    },
    plugins: [
        new webpack.HotModuleReplacementPlugin(),
        new webpack.NoEmitOnErrorsPlugin(),
        new webpack.DefinePlugin({
            "process.env": require("../config/dev.js")
        }),
        new webpack.optimize.AggressiveMergingPlugin(),
        new HtmlWebpackPlugin({
            title: "Secure System Modeller - Dashboard (DEV)",
            template: path.join(ROOT, "app/common/templates/dashboard-template.html"),
            chunks: ["dashboard"],
            inject: "body",
            filename: "dashboard.html"
        }),
        new HtmlWebpackPlugin({
            title: "Secure System Modeller - Editor (DEV)",
            template: path.join(ROOT, "app/common/templates/modeller-template.html"),
            chunks: ["modeller"],
            inject: "body",
            filename: "modeller.html"
        }),
        new HtmlWebpackPlugin({
            title: "Secure System Modeller - Admin (DEV)",
            template: path.join(ROOT, "app/common/templates/template.html"),
            chunks: ["admin"],
            inject: "body",
            filename: "admin.html"
        }),
        new HtmlWebpackPlugin({
            title: "Secure System Modeller - Domain Manager (DEV)",
            template: path.join(ROOT, "app/common/templates/template.html"),
            chunks: ["domainManager"],
            inject: "body",
            filename: "domain-manager.html"
        }),
        new webpack.LoaderOptionsPlugin({
            options: {
                postcss: function () {
                    return [
                        autoprefixer({
                            browsers: [
                                ">1%",
                                "last 4 versions",
                                "Firefox ESR",
                                "not ie < 9", // React doesn't support IE8 anyway
                            ]
                        }),
                    ];
                },
            }
        })
        //PROD only, the compress flag is configured to REMOVE unused code from the bundle, not include it.
        //new webpack.optimize.UglifyJsPlugin({mangle: true, compress: {unused: false}})
    ]
};
