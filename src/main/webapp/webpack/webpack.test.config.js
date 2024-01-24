const webpack = require('webpack');
const path = require('path');
const autoprefixer = require('autoprefixer');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const ROOT = path.resolve(__dirname, '../');
const SRC = path.resolve(ROOT, 'app');
const NODE_MODULES = path.resolve(ROOT, "node_modules");
const BUILD = path.resolve(ROOT, 'dist');

module.exports = {
    mode: 'development',
    devtool: 'eval',
    entry: {
        dashboard: [SRC + '/dashboard/index.js'],
        admin: [SRC + '/admin/index.js'],
        modeller: [SRC + '/modeller/index.js'],
        domainManager: [SRC + '/domainManager/index.js'],
        vendors: ['react']
    },
    output: {
        path: BUILD,
        filename: '[name].bundle.js',
        publicPath: '/dist/'
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
                            plugins: ["@babel/plugin-transform-runtime"]
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
                        loader: MiniCssExtractPlugin.loader,  // extracts CSS into CSS files
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
                ]
            },
            {
                test: /\.css$/,
                include: [SRC, NODE_MODULES],
                use: [
                    {
                        loader: MiniCssExtractPlugin.loader,  // extracts CSS into CSS files so they can be loaded in parallel
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
    plugins: [
        new MiniCssExtractPlugin(),
        new webpack.NoEmitOnErrorsPlugin(),
        new webpack.DefinePlugin({"process.env": require("../config/test.js")}),
        new webpack.optimize.AggressiveMergingPlugin(),
        new webpack.LoaderOptionsPlugin({
            options: {
                postcss: function() {
                    return [
                        autoprefixer({
                            browsers: [
                                '>1%',
                                'last 4 versions',
                                'Firefox ESR',
                                'not ie < 9', // React doesn't support IE8 anyway
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
