import {combineReducers} from 'redux';
import auth from "../../common/reducers/auth";
import {domainManager} from "./domainManager";


const reducers = combineReducers({
    domainManager,
    auth
    //form: reduxFormReducer, // this has to be named form for reduxForm to work
});

export default reducers;