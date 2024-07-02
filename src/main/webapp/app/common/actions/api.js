import * as actions from "../reducers/auth";
import * as about_actions from "../reducers/about";
import {polyfill} from "es6-promise";
import {axiosInstance, axiosInstanceRestricted} from "../rest/rest";

polyfill();

export function getAboutInfo() {
    return function (dispatch) {
        axiosInstance
            .get("/about")
            .then((response) => {
                dispatch({
                    type: about_actions.GET_ABOUT_INFO,
                    payload: response
                });
            })
    };
}

export function hideAboutModal() {
    return function (dispatch) {
        dispatch({
            type: about_actions.HIDE_ABOUT_MODAL,
        });
    };
}

export function getUser() {
    return function (dispatch) {
        axiosInstance
            .get("/auth/me")
            .then((response) => {
                dispatch({
                    type: actions.GET_USER,
                    payload: response
                });
            })
            .catch((error) => {
                dispatch({
                    type: actions.GET_USER,
                    payload: {
                        data: null
                    }
                });
            });
    };
}

export function saveDownload(url) {
    return function (dispatch) {
        downloadThenSave(axiosInstance, url);
    };
}

export function saveRestrictedDownload(url) {
    return function (dispatch) {
        downloadThenSave(axiosInstanceRestricted, url);
    };
}

function downloadThenSave(axiosRef, url) {

    // Perform a GET request to download the file.
    // If the HEAD request fails the error handler associated with the
    // particular axios instance will handle that.
    // Otherwise invoke the file save dialog using the filename
    // set by the server in the "content-disposition" header.
    axiosRef
        .get(url, {responseType: 'blob'})
        .then((response) => {
            const link = document.createElement("a");
            link.download = response.headers["content-disposition"].split("filename=")[1];
            link.href = window.URL.createObjectURL(new Blob([response.data]));
            link.click();
            link.remove();
        });
}

export function loadPage(url, restricted, event) {
    return function (dispatch) {

        // loadPage() is called from within an <a> or a <MenuItem>.
        // We MUST stop the default action (from those elements) of directly
        // fetching the URL. If we do not, our attempt to perform the request
        // via axios will be wasted, as the page will just directly load.
        event.preventDefault();

        // Choose which axios instance to use depending upon how we want to
        // handle 403s.
        if (restricted) {
            checkThenLoad(axiosInstanceRestricted, url);
        } else {
            checkThenLoad(axiosInstance, url);
        }
    };
}

function checkThenLoad(axiosRef, url) {

    // Perform a HEAD request to see if the GET will succeed.
    // If the HEAD request fails the error handler associated with the
    // particular axios instance will handle that.
    // Otherwise it loads the page.
    axiosRef
        .head(url)
        .then((response) => {
            window.location.href = process.env.config.END_POINT + url;
        });
}
