import React from 'react';
import {Provider} from 'react-redux';
import '../common/styles/index.scss';
import thunkMiddleware from 'redux-thunk';
import {createStore, applyMiddleware, compose} from 'redux'
import reducers from './reducers';
import Admin from "./components/Admin";
import Page from "../common/components/page/Page";

export const store = createStore(
    reducers,
    compose(applyMiddleware(thunkMiddleware), window.devToolsExtension ? window.devToolsExtension() : f => f)
);

if (module.hot) {
    module.hot.accept("./reducers/index.js", () =>
        store.replaceReducer(require("./reducers/index.js").default)
    );
}

const App = () => (
    <Provider store={store}>
        <Page>
            <Admin/>
        </Page>
    </Provider>
);

export default App;