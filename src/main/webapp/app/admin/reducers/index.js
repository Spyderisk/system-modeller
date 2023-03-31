import {combineReducers} from 'redux';
import admin from "./admin";
import auth from "../../common/reducers/auth";


const reducers = combineReducers({
    admin,
    auth
    //form: reduxFormReducer, // this has to be named form for reduxForm to work
});

export default reducers;