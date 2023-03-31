const webpack = require('webpack');
const path = require('path');
const autoprefixer = require('autoprefixer');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const ROOT = path.resolve(__dirname, '../');
const SRC = path.resolve(ROOT, 'app');
const BUILD = path.resolve(ROOT, 'dist');

module.exports = {
    mode: 'production',
    devtool: 'cheap-module-source-map',
    entry: {
        dashboard: [SRC + '/dashboard/index.js'],
        modeller: [SRC + '/modeller/index.js'],
        domainManager: [SRC + '/domainManager/index.js'],
        admin: [SRC + "/admin/index.js"],
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
                        loader: MiniCssExtractPlugin.loader,  // extracts CSS into CSS files so they can be loaded in parallel
                    },
                    {
                        loader: 'css-loader',  // converts from CSS to CommonJS to add to JS bundle
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
            }
        ]
    },
    plugins: [
        new webpack.NoEmitOnErrorsPlugin(),
        new MiniCssExtractPlugin(),
        new webpack.DefinePlugin({"process.env": require("../config/prod.js")}),
        new webpack.LoaderOptionsPlugin({
            minimize: true,
            debug: false,
            options: {
                postcss: function () {
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
        }),
        new webpack.optimize.AggressiveMergingPlugin(),
        /*new webpack.optimize.UglifyJsPlugin({
            mangle: true,
            compress: {
                warnings: false,
                screw_ie8: true,
                conditionals: true,
                unused: true,
                comparisons: true,
                sequences: true,
                dead_code: true,
                evaluate: true,
                if_return: true,
                join_vars: true
            },
            output: {
                comments: false,
            }
        })*/
    ]
};
