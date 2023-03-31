import {polyfill} from "es6-promise";

export const GET_USERS = "GET_USERS";

polyfill();

export default function admin(state = {
    userList: [],
}, action) {
    switch (action.type) {
        case GET_USERS:
            return {
                ...state,
                userList: action.payload
            };
    }
    return state;
}
