export const GET_ABOUT_INFO = "about/GET_ABOUT_INFO";
export const HIDE_ABOUT_MODAL = "about/HIDE_ABOUT_MODAL";

export default function about(state = {
    showAboutModal: false,
    info: undefined
}, action) {

    if (action.type === GET_ABOUT_INFO) {
        return {
            ...state,
            showAboutModal: true,
            info: action.payload.data
        };
    }

    if (action.type === HIDE_ABOUT_MODAL) {
        return {
            ...state,
            showAboutModal: false,
        };
    }

    return state;
}
