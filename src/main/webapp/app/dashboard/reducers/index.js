import {combineReducers} from 'redux';
import dashboard from './dashboard';
import auth from "../../common/reducers/auth";
import about from "../../common/reducers/about";

const reducers = combineReducers({
    dashboard,
    auth,
    about,
    //form: reduxFormReducer, // this has to be named form for reduxForm to work
});

export default reducers;