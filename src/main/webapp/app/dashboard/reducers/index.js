import {combineReducers} from 'redux';
import dashboard from './dashboard';
import auth from "../../common/reducers/auth";


const reducers = combineReducers({
    dashboard,
    auth
    //form: reduxFormReducer, // this has to be named form for reduxForm to work
});

export default reducers;