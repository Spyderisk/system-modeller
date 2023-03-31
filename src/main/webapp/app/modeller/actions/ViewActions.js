import * as instr from "../modellerConstants";


export function bringToFrontWindow(windowName) {
    //console.log("bringToFrontWindow: ", windowName);
    return function (dispatch) {
        dispatch({
            type: instr.OPEN_WINDOW,
            payload: windowName
        });
    };
}

export function closeWindow(windowName) {
    return function (dispatch) {
        dispatch({
            type: instr.CLOSE_WINDOW,
            payload: windowName
        });
    };
}