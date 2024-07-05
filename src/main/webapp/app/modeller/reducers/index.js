import {combineReducers} from 'redux';
import modeller from './modeller';
import view from './view';
import auth from "../../common/reducers/auth";
import about from "../../common/reducers/about";
import interaction from "./interaction"

const reducers = combineReducers({
    interaction,
    modeller,
    view,
    auth,
    about,
    //form: reduxFormReducer, // this has to be named form for reduxForm to work
});

export default reducers;