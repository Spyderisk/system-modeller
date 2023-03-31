import * as actions from "../reducers/admin";
import {polyfill} from "es6-promise";
import {axiosInstance} from "../../common/rest/rest";

polyfill();

export function getUsers() {
    return function (dispatch) {
        axiosInstance
            .get("/administration/users")
            .then((response) => {
                dispatch({
                    type: actions.GET_USERS,
                    payload: response.data
                })
            });
    };
}
