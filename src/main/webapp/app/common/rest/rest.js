import React from "react";
import ReactDOM from "react-dom";
import axios from "axios";
import {API_VERSION} from "../../../config/config";
import * as vars from "../reducers/constants";
import {
    Button,
    MenuItem,
    Modal,
    Alert
} from "react-bootstrap";
import { render } from "react-dom";
import { lockedModel } from "../../dashboard/components/modelItem/ModelItemFunctions";


// Axios config for most REST calls.
const config = {
    baseURL: process.env.config.API_END_POINT,
    headers: {
        "Content-Type": "application/vnd.itinno.v"+API_VERSION+"+json",
        "Cache-Control": "no-cache,no-store,must-revalidate,max-age=-1,private",
        "X-Requested-With": "XMLHttpRequest" //Ensure a 401 not a 302 when not authenticated
    },
    data: {}
};

// Axios config for REST calls that upload files.
const fileConfig = {
    baseURL: process.env.config.API_END_POINT,
    headers: {
        "X-Requested-With": "XMLHttpRequest" //Ensure a 401 not a 302 when not authenticated
    }
}

// Default axios objects - to be used by most REST calls.
export const axiosInstance = axios.create(config);
export const axiosFileInstance = axios.create(fileConfig);
export const axiosInstanceDashboard = axios.create(config);

// Restricted axios objects - to be used by REST calls that require our default handling of 403s.
export const axiosInstanceRestricted = axios.create(config);
export const axiosFileInstanceRestricted = axios.create(fileConfig);

// This helps diagnose issues on the requests made by logging the request itself.
// axiosInstance.interceptors.request.use(request => {
//     console.log('Starting Request', request);
//     return request;
// });

// Add default error handler - gets called before catch.
axiosInstance.interceptors.response.use((response) => response, defaultErrorHandler);
axiosFileInstance.interceptors.response.use((response) => response, defaultErrorHandler);

// Add restricted error handler - gets called before catch.
axiosInstanceRestricted.interceptors.response.use((response) => response, restrictedErrorHandler);
axiosFileInstanceRestricted.interceptors.response.use((response) => response, restrictedErrorHandler);

// No specific action on a 403 beyond putting up an alert.
function defaultErrorHandler(error) {
    return globalErrorHandler(error, () => {});
}

// Loads the welcome page on a 403 after putting up an alert.
function restrictedErrorHandler(error) {
    return globalErrorHandler(error, loadWelcomePage);
}

// Global error handler that applies our policy for handling REST call errors.
function globalErrorHandler(error, responseOnForbidden) {
    if (error.response) {
        switch (error.response.status) {
            case vars.HTTPStatusCode.BAD_REQUEST:
                console.log(error);
                reportError(error, "Bad request: check browser console logs for details");
                break;
            case vars.HTTPStatusCode.UNAUTHORIZED:
                // Header.js will always make a request to /auth/me.
                // This includes the case when editing a model anonymously.
                // In this special case a 401 should not trigger loading the welcome page,
                // in all other cases the user should be directed to the welcome page.
                if (error.config.url !== "/auth/me") {
                    loadWelcomePage();
                }
                break;
            case vars.HTTPStatusCode.UNPROCESSABLE_ENTITY:
                console.log(error);
                break;
            case vars.HTTPStatusCode.FORBIDDEN:
                reportError(error, "Only an administrator can perform that operation.");
                responseOnForbidden();
                break;
            case vars.HTTPStatusCode.NOT_FOUND:
                reportError(error, "Not found. Check with an administrator for further help.");
                break;
            case vars.HTTPStatusCode.LOCKED:
                restLockedModal();
                break;
            default:
                reportError(error, "Something went wrong. Check with an administrator for further help.");
        }
    } else {
        reportError(error, "Something went wrong. Check with an administrator for further help.");
    }

    return Promise.reject(error);
}

// Put up an error alert with the a message for the user.
function reportError(error, userMessage) {

    // It would be nice to augment the error message
    // with information from error. Unfortunately
    // there is no consistency in the information
    // provided.
    alert(userMessage);
}

function restLockedModal() {
    
    // Calling function that gets the modal for handling Locking
    lockedModel()
}


export function loadWelcomePage() {
    window.location.href = process.env.config.END_POINT;
}
