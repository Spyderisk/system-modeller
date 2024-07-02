import {combineReducers} from 'redux';
import admin from "./admin";
import auth from "../../common/reducers/auth";
import about from "../../common/reducers/about";

const reducers = combineReducers({
    admin,
    auth,
    about,
    //form: reduxFormReducer, // this has to be named form for reduxForm to work
});

export default reducers;