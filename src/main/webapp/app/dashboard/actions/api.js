import * as actions from "../reducers/dashboard";
import {polyfill} from "es6-promise";
import {axiosFileInstance, axiosInstance} from "../../common/rest/rest";


polyfill();

//Get models for logged in user
export function getModels() {
    return function (dispatch) {
        axiosInstance
            .get("/models/")
            .then((response) => {
                dispatch({
                    type: actions.GET_MODELS,
                    payload: response.data
                });
            });
    };
}

//Get user details (if dashboard is showing details for another user)
export function getUser(userid) {
    return function (dispatch) {
        axiosInstance
            .get("/administration/users/" + userid)
            .then((response) => {
                dispatch({
                    type: actions.GET_USER,
                    payload: response.data
                });
            });
    };
}

//Get models for specified user (admin user only)
export function getModelsForUser(userId) {
    return function (dispatch) {
        axiosInstance
            .get("/usermodels/" + userId)
            .then((response) => {
                dispatch({
                    type: actions.GET_MODELS,
                    payload: response.data
                });
            });
    };
}

export function addNewModel(model, domainModel) {
    console.log("addNewModel: " + model);
    console.log("domainModel: ", domainModel);

    return function (dispatch) {
        axiosInstance
            .post("/models/", {
                "name": model,
                "domainGraph": domainModel.graph,
            })
            .then((response) => {
                dispatch({
                    type: actions.ADD_MODEL,
                    payload: response.data
                });
            });
    };
}

export function updateModel(modelId, updatedModel) {
    return function (dispatch) {
        axiosInstance
            .put("models/" + modelId, updatedModel)
            .then((response) => {
                dispatch({
                    type: actions.EDIT_MODEL,
                    payload: response.data
                })
            });
    };
}

export function copyModel(modelId, templateModel) {
    return function (dispatch) {
        axiosInstance
            .post("models/" + modelId + "/copyModel", templateModel)
            .then((response) => {
                dispatch({
                    type: actions.ADD_COPY_MODEL,
                    payload: response.data
                });
            });
    };
}

export function checkin(modelId) {
    return function (dispatch) {
        axiosInstance
            .post("models/" + modelId + "/checkin")
            .then((response) => {
                dispatch({
                    type: actions.EDIT_MODEL,
                    payload: response.data
                })
            });
    };
}

export function checkout(modelId) {
    return function (dispatch) {
        axiosInstance
            .post("models/" + modelId + "/checkout")
            .then((response) => {
                dispatch({
                    type: actions.EDIT_MODEL,
                    payload: response.data
                })
            });
    };
}

export function deleteModel(modelId) {
    return function (dispatch) {
        axiosInstance
            .delete("/models/" + modelId)
            .then((response) => {
                dispatch({
                    type: actions.DELETE_MODEL,
                    payload: modelId
                });
            });
    };
}

export function getDomains() {
    console.log("getDomains");
    return function (dispatch) {
        dispatch({
            type: actions.GET_ONTOLOGIES,
            payload: {
                loading: true
            }
        })

        axiosInstance
            .get("/domains/")
            .then((response) => {
                dispatch({
                    type: actions.GET_ONTOLOGIES,
                    payload: {
                        ontologies: response.data,
                        loading: false
                    }
                });
            });
    };
}

export function updateUploadProgress(percentage) {
    return function (dispatch) {
        dispatch({
            type: actions.UPDATE_UPLOAD_PROGRESS,
            payload: {
                progress: percentage,
                failed: false
            }
        });
    };
}

export function uploadModel(data) {
    return function (dispatch) {
        const config = {
            onUploadProgress: function (progressEvent) {
                let percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total) - 10;
                percentCompleted = (percentCompleted < 0 ? 5 : percentCompleted);
                dispatch(updateUploadProgress(percentCompleted))
            },
        };

        axiosFileInstance
            .post("/models/import", data, config)
            .then((response) => {
                dispatch({
                    type: actions.UPLOAD_MODEL,
                    payload: response.data
                });
            })
            .catch((error) => {
                dispatch({
                    type: actions.UPDATE_UPLOAD_PROGRESS,
                    payload: {
                        progress: 0,
                        status: {
                            failed: true,
                            reason: error.response ? error.response.data : "Upload failed - report this to an administrator."
                        }
                    }
                });
            });
    };
}

export function exportModel(modelId) {
    return function (dispatch) {
        axiosInstance
            .get("/models/" + modelId + "/export")
            .then((response) => {
                dispatch({
                    type: actions.EXPORT_MODEL,
                    payload: response
                });
            });
    };
}
