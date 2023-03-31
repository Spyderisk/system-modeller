import * as instr from "../reducers/domainManager";
import {polyfill} from "es6-promise";
import {
    axiosInstance,
    axiosInstanceRestricted,
    axiosFileInstanceRestricted
} from "../../common/rest/rest";

polyfill();

export function getDomains() {
    return function (dispatch) {
        axiosInstance
            .get("/domains/")
            .then((response) => {
                //console.log(response);
                dispatch({
                    type: instr.GET_ONTOLOGIES,
                    payload: response.data
                })
            });
    };
}

export function updateUploadProgress(percentage) {
    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_UPLOAD_PROGRESS,
            payload: {
                progress: percentage,
                failed: false
            }
        });
    };
}

export function uploadDomain(data) {
    return function (dispatch) {
        const config = {
            onUploadProgress: function (progressEvent) {
                let percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total) - 10;
                percentCompleted = (percentCompleted < 0 ? 5 : percentCompleted);
                dispatch(updateUploadProgress(percentCompleted))
            },
        };

        axiosFileInstanceRestricted
            .post("/domains/upload", data, config)
            .then((response) => {
                //console.log("uploadDomain response code:", response.status);
                dispatch({
                    type: instr.UPLOAD_DOMAIN,
                    payload: response.data
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.UPDATE_UPLOAD_PROGRESS,
                    payload: {
                        //progress: 0,
                        status: {
                            failed: true,
                            reason: error.response ? error.response.data : "Upload failed - report this to an administrator."
                        }
                    }
                });
            });
    };
}

export function toggleUploadModal(flag, domainUri, domain, newDomain) {
    return function (dispatch) {
        dispatch({
            type: instr.TOGGLE_UPLOAD_MODAL,
            payload: {
                isOpen: flag,
                domainUri: domainUri,
                domain: domain,
                newDomain: newDomain
            }
        });
    };
}

export function getDomainPalette(modelUrl) {
    return function (dispatch) {
        axiosInstance
            .get("/models/" + modelUrl + "/palette")
            .then((response) => {
                dispatch({
                    type: instr.GET_PALETTE,
                    payload: response.data
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.GET_PALETTE,
                    payload: {}
                });
            });
    };
}

export function updateDomainPalette(domainUri, palette) {
    return function (dispatch) {
        domainUri = domainUri.substr(domainUri.lastIndexOf("/") + 1);

        axiosInstanceRestricted
            .post("/domains/" + domainUri  + "/palette", palette)
            .then((response) => {
                dispatch({
                    type: instr.GET_PALETTE,
                    payload: response.data
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.GET_PALETTE,
                    payload: {}
                });
            });
    };
}

export function deleteDomainModel(domainUri) {
    return function (dispatch) {
        let domain = domainUri.substr(domainUri.lastIndexOf("/") + 1);

        axiosInstanceRestricted
            .delete("/domains/" + domain)
            .then((response) => {
                dispatch({
                    type: instr.GET_ONTOLOGIES,
                    payload: response.data
                });
            });
    };
}

export function getUsers() {
    return function (dispatch) {
        axiosInstanceRestricted
            .get("/domains/users")
            .then((response) => {
                dispatch({
                    type: instr.GET_USERS_ALL,
                    payload: response.data
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.GET_USERS,
                    payload: []
                });
            });
    };
}

export function getDomainUsers(domainUri) {
    //console.log("getDomainUsers: " + domainUri);

    let domain = domainUri ? domainUri.substr(domainUri.lastIndexOf("/") + 1) : undefined;
    
    if (!domain) {
        console.warn("domain is not defined");
        return;
    }
    
    return function (dispatch) {
        axiosInstanceRestricted
            .get("/domains/" + domain + "/users")
            .then((response) => {
                dispatch({
                    type: instr.GET_USERS,
                    payload: response.data.users
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.GET_USERS,
                    payload: []
                });
            });
    };
}

export function updateDomainUsers(domainUri, users) {
    return function (dispatch) {
        domainUri = domainUri.substr(domainUri.lastIndexOf("/") + 1);

        axiosInstanceRestricted
            .post("/domains/" + domainUri + "/users", {users: users})
            .then((response) => {
                dispatch({
                    type: instr.GET_USERS,
                    payload: response.data
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.GET_USERS,
                    payload: []
                });
            });
    };
}
