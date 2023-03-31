export const GET_USER = "authentication/GET_USER";
export const RESET_GET_USER = "authentication/RESET_GET_USER";

export default function auth(state = {
    hadResponse: false,
    isAuthenticated: false,
    user: null
}, action) {
    switch (action.type) {
        case GET_USER:
            return {
                ...state,
                hadResponse: true,
                isAuthenticated: action.payload.data !== null,
                user: action.payload.data
            };
        case RESET_GET_USER:
            return {
                ...state,
                hadResponse: false
            };
    }

    return state;
}
