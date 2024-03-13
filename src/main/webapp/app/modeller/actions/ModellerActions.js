import * as instr from "../modellerConstants";
import * as Constants from "../../common/constants.js";
import {polyfill} from "es6-promise";
import {axiosInstance} from "../../common/rest/rest";
import {addAsset, addRelation} from "./InterActions"

polyfill();

export function getModel(modelId) {
    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_MODEL_LOADING,
            payload: true
        });
        dispatch({
            type: instr.IS_NOT_VALIDATING,
            payload: true
        });
        dispatch({
            type: instr.IS_NOT_CALCULATING_RISKS,
            payload: true
        });

        axiosInstance
            .get("/models/" + modelId + "/")
            .then((response) => {
                    //console.log("getModel response");
                    //console.log(response);
                    //model response is only used here to get the loadingId - full model will arrive later
                    console.log("Received loadingId: " + response.data["loadingId"]);
                    dispatch({
                        type: instr.GET_MODEL_LOADING_ID,
                        payload: response.data["loadingId"]
                    });

                    axiosInstance
                        .get("/models/" + modelId + "/palette")
                        .then((palette) => {
                            console.log("palette.status");
                            console.log(palette.status);
                            palette.data["dataTypes"] = [
                                {
                                    data: "portNum",
                                    appliesTo: [
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Process",
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Host",
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Server",
                                    ],
                                    type: "integer"
                                },
                                {
                                    data: "hasFirewallRule",
                                    appliesTo: [
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Process",
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Host",
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Server",
                                    ],
                                    type: "boolean"
                                },
                                {
                                    data: "description",
                                    appliesTo: [
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Process",
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Host",
                                        "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Server",
                                    ],
                                    type: "string"
                                }
                            ];
                            //response.data["palette"] = palette.data;
                            //response is only used here to enable fetching the palette - full model will arrive later
                            dispatch({
                                type: instr.GET_PALETTE,
                                payload: palette.data
                            });
                            //dispatch({
                            //    type: instr.UPDATE_MODEL_LOADING,
                            //    payload: false
                            //});
                        })
                        .catch((error) => {
                            dispatch({
                                type: instr.UPDATE_MODEL_LOADING,
                                payload: false
                            });
                        });
            })
            .catch((error) => {
                dispatch({
                    type: instr.UPDATE_MODEL_LOADING,
                    payload: false
                });
            });
    };
}

export function updateModel(modelId, updatedModel) {
    //console.log("updateModel (modeller):", updatedModel);
    return function (dispatch) {
        axiosInstance
            .put("models/" + modelId, updatedModel)
            .then((response) => {
                dispatch({
                    type: instr.EDIT_MODEL,
                    payload: response.data
                })
            });
    };
}
export function updateEdit(edit) {
    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_EDIT,
            payload: edit,
        });
    };
}

export function updateAuthz(modelId, updatedAuthz) {
    return function (dispatch) {
        axiosInstance
            .put("models/" + modelId + "/authz", updatedAuthz)
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_AUTHZ,
                    payload: response.data,
                });
            });
    };
}

export function getAuthz(modelId, username) {
    return function (dispatch) {
        axiosInstance
            .get("/models/" + modelId + "/authz")
            .then((response) => {
                dispatch({
                    type: instr.GET_AUTHZ,
                    payload:{
                        data: response.data,
                        username: username
                    }
                });
            });
    };
}


export function pollForValidationProgress(modelId) {
    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_VALIDATION_PROGRESS,
            payload: {
                waitingForUpdate: true,
            }
        });

        axiosInstance
            .get("/models/" + modelId + "/validationprogress")
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_VALIDATION_PROGRESS,
                    payload: {
                        status: response.data["status"],
                        progress: response.data["progress"],
                        message: response.data["message"],
                        error: response.data["error"],
                        waitingForUpdate: false
                    }
                });
            });
    };
}

export function pollForRiskCalcProgress(modelId) {
    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_RISK_CALC_PROGRESS,
            payload: {
                waitingForUpdate: true,
            }
        });

        axiosInstance
            .get("/models/" + modelId + "/riskcalcprogress")
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_RISK_CALC_PROGRESS,
                    payload: {
                        status: response.data["status"],
                        progress: response.data["progress"],
                        message: response.data["message"],
                        error: response.data["error"],
                        waitingForUpdate: false
                    }
                });
            });
    };
}

export function pollForRecommendationsProgress(modelId) {
    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_RECOMMENDATIONS_PROGRESS,
            payload: {
                waitingForUpdate: true,
            }
        });

        axiosInstance
            .get("/models/" + modelId + "/recommendationsprogress")
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_RECOMMENDATIONS_PROGRESS,
                    payload: {
                        status: response.data["status"],
                        progress: response.data["progress"],
                        message: response.data["message"],
                        error: response.data["error"],
                        waitingForUpdate: false
                    }
                });
            });
    };
}

export function pollForLoadingProgress(modelId, loadingId) {
    // console.log("pollForLoadingProgress: loadingId = " + loadingId);
    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_LOADING_PROGRESS,
            payload: {
                waitingForUpdate: true,
            }
        });

        axiosInstance
            .get("/models/" + modelId + "/" + loadingId + "/loadingprogress")
            .then((response) => {
                //console.log(response.data);
                dispatch({
                    type: instr.UPDATE_LOADING_PROGRESS,
                    //payload: response.data
                    payload: {
                        progress: response.data["progress"],
                        message: response.data["message"],
                        status: response.data["status"],
                        error: response.data["error"],
                        waitingForUpdate: false
                    }
                });

                if (response.data.model) {
                    //console.log("Model loaded:", response.data.model);
                    dispatch({
                        type: instr.UPDATE_MODEL_LOADING,
                        payload: false
                    });
                    // The model.id returned from the service is the no-role webkey (that is, it provides no authorisation role when used).
                    // If the client has requested the model using a webkey which has a role (read, write, owner) and they are unauthenticated
                    // (or more precisely, the role from the webkey is greater than their authenticated role) then we must take care not to
                    // lose the initial webkey that we used or subsequent request may fail. Hence this line:
                    response.data.model.id = modelId;
                    dispatch({
                        type: instr.GET_MODEL,
                        payload: response.data.model
                    });
                }
                else if (response.data.status === "failed") {
                    console.log("Model loading failed:", response.data.message, response.data.error);
                    dispatch({
                        type: instr.UPDATE_MODEL_LOADING,
                        payload: false
                    });
                }
            })
            .catch((error) => {
                dispatch({
                    type: instr.UPDATE_MODEL_LOADING,
                    payload: false
                });
            });
    };
}

export function getValidatedModel(modelId) {
    console.log("getValidatedModel");
    return function (dispatch) {
        //first close threat editor and misbehaviour explorer
        console.log("Closing threat editor");
        dispatch({
            type: instr.TOGGLE_THREAT_EDITOR,
            payload: {
                toggle: false,
                threatId: ""
            }
        });
        console.log("Closing misbehaviour explorer");
        dispatch({
            type: instr.CLOSE_MISBEHAVIOUR_EXPLORER
        });
        dispatch({
            type: instr.IS_VALIDATING
        });
        console.log("Validating model: " + modelId);

        axiosInstance
            .get("/models/" + modelId + "/validated")
            .catch((error) => {
                dispatch({
                    type: instr.IS_NOT_VALIDATING
                });
            });
    };
}

export function calculateRisks(modelId, mode) {
    console.log("calculateRisks: ", mode);
    return function (dispatch) {
        /*
        //first close threat editor and misbehaviour explorer
        console.log("Closing threat editor");
        dispatch({
            type: instr.TOGGLE_THREAT_EDITOR,
            payload: {
                toggle: false,
                threatId: ""
            }
        });
        console.log("Closing misbehaviour explorer");
        dispatch({
            type: instr.CLOSE_MISBEHAVIOUR_EXPLORER
        }); */
        dispatch({
            type: instr.IS_CALCULATING_RISKS
        });
        console.log("Calculating risks for model: " + modelId);

        axiosInstance
            .get("/models/" + modelId + "/calc_risks", {params: {mode: mode}})
            .catch((error) => {
                dispatch({
                    type: instr.IS_NOT_CALCULATING_RISKS
                });
            });
    };
}

export function calculateRisksBlocking(modelId, mode, saveResults) {
    console.log("calculateRisksBlocking: ", mode, " saveResults = " + saveResults);
    return function (dispatch) {
        dispatch({
            type: instr.IS_CALCULATING_RISKS
        });
        console.log("Calculating risks for model: " + modelId);

        axiosInstance
            .get("/models/" + modelId + "/calc_risks_blocking", {params: {mode: mode, save: saveResults}})
            .then((response) => { 
                dispatch({
                    type: instr.IS_NOT_CALCULATING_RISKS,
                });
                dispatch({
                    type: instr.RISK_CALC_RESULTS,
                    payload: {saved: saveResults, results: response.data}
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.IS_NOT_CALCULATING_RISKS
                });
            });
    };
}

export function dropInferredGraph(modelId) {
    console.log("dropInferredGraph");
    
    return function (dispatch) {
        dispatch({
            type: instr.IS_DROPPING_INFERRED_GRAPH
        });
    
        axiosInstance
            .post("/models/" + modelId + "/clear_inferred_graph")
            .then((response) => {
                console.log("Dropped inferred graph");
                dispatch({
                    type: instr.IS_NOT_DROPPING_INFERRED_GRAPH,
                });

                //reload model
                dispatch(getModel(modelId));
            })
            .catch((error) => {
                dispatch({
                    type: instr.IS_NOT_DROPPING_INFERRED_GRAPH
                });
            });
    };
}

export function validationCompleted(modelId) {
    console.log("validationCompleted");
    return function (dispatch) {
        dispatch(getModel(modelId));
    };
}

export function validationFailed(modelId) {
    console.log("validationFailed");
    return function (dispatch) {
        //dispatch({
        //    type: instr.IS_NOT_VALIDATING
        //});
        dispatch({
            type: instr.VALIDATION_FAILED
        });
    };
}

export function resetValidation() {
    console.log("resetValidation");
    return function (dispatch) {
        dispatch({
            type: instr.IS_NOT_VALIDATING
        });
    };
}

export function riskCalcCompleted(modelId) {
    console.log("riskCalcCompleted");
    return function (dispatch) {
        dispatch(getModel(modelId));
    };
}

export function riskCalcFailed(modelId) {
    console.log("riskCalcFailed");
    return function (dispatch) {
        dispatch({
            type: instr.RISK_CALC_FAILED
        });
    };
}

export function recommendationsCompleted(modelId, jobId) {
    console.log("recommendationsCompleted");
    return function (dispatch) {
        dispatch({
            type: instr.IS_NOT_CALCULATING_RECOMMENDATIONS,
        });
        axiosInstance
            .get("/models/" + modelId + "/recommendations/result/" + jobId)
            .then((response) => { 
                dispatch({
                    type: instr.RECOMMENDATIONS_RESULTS,
                    payload: response.data
                });
                dispatch({
                    type: instr.OPEN_WINDOW,
                    payload: "recommendationsExplorer"
                });
            })
            .catch((error) => {
                console.log("Error:", error);
            });
    };
}

export function recommendationsFailed(modelId) {
    console.log("recommendationsFailed");
    return function (dispatch) {
        dispatch({
            type: instr.RECOMMENDATIONS_FAILED
        });
    };
}

export function abortRecommendations(modelId) {
    console.log("abortRecommendations for model: ", modelId);
    return function (dispatch) {
        axiosInstance
            .post("/models/" + modelId + "/recommendations/cancel") //TODO: get correct endpoint
            .then((response) => {
                dispatch({
                    type: instr.IS_NOT_CALCULATING_RECOMMENDATIONS
                });
            })
            .catch((error) => {
                console.log("Error:", error);
                dispatch({
                    type: instr.IS_NOT_CALCULATING_RECOMMENDATIONS
                });
            });
    };
}

export function loadingCompleted(modelId) {

}

/* KEM - seems to be unused
export function resetValidationStatus() {
    return function (dispatch) {
        dispatch({
            type: instr.RESET_VALIDATION_STATUS
        });
    };
}
*/

export function modellerSetZoom(zoom, transformOrigin) {
    //console.log("setZoom: ", zoom, transformOrigin);
    return function (dispatch) {
        dispatch({
            type: instr.SET_ZOOM,
            payload: {
                zoom: zoom,
                transformOrigin: transformOrigin
            }
        });
    };
}

export function setTransformOrigin(transformOrigin) {
    console.log("setTransformOrigin: ", transformOrigin);
    return function (dispatch) {
        dispatch({
            type: instr.SET_TRANSFORM_ORIGIN,
            payload: {
                transformOrigin: transformOrigin
            }
        });
    };
}

export function postAssertedAsset(modelId, asset) {
    return function (dispatch) {
        dispatch({
            type: instr.NEW_FACT,
            payload: "asset (" + asset["label"] + ")"
        });

        axiosInstance
            .post("/models/" + modelId + "/assets/", asset)
            .then((response) => {
                dispatch(addAsset(response.data["asset"]));
                dispatch({
                    type: instr.POST_ASSET,
                    payload: response.data
                });
                dispatch({
                    type: instr.NEW_FACT,
                    payload: ""
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.NEW_FACT,
                    payload: ""
                });
            });
    };
}

export function postAssertedRelation(modelId, assetIdFrom, assetIdTo, relType) {
    //console.log("postAssertedRelation: " + modelId + ", " + assetIdFrom + ", " + assetIdTo + ", ", relType);
    let relName = relType["label"];

    return function (dispatch) {
        dispatch({
            type: instr.NEW_FACT,
            payload: "relation (" + relName + ")"
        });

        axiosInstance
            .post("/models/" + modelId + "/relations/", {
                //no from/to URIs here yet - they will be created by the RelationController
                "from": null,
                "fromID": assetIdFrom,
                "to": null,
                "toID": assetIdTo,
                "label": relName,
                "type": relType["type"],
                "asserted": true,
                "visible": true
            })
            .then((response) => {
                dispatch(addRelation(response.data["relation"]));
                dispatch({
                    type: instr.POST_RELATION,
                    payload: response.data
                });
                dispatch({
                    type: instr.NEW_FACT,
                    payload: ""
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.NEW_FACT,
                    payload: ""
                });
            });
    };
}

export function postAssertedAssetGroup(modelId, group) {
    console.log("postAssertedAssetGroup: ", group);
    
    return function (dispatch) {
        dispatch({
            type: instr.NEW_FACT,
            payload: "group (" + group.name + ")"
        });

        axiosInstance
            .post("/models/" + modelId + "/assetGroups/", group)
            .then((response) => {
                dispatch({
                    type: instr.POST_GROUP,
                    payload: response.data
                });
                dispatch({
                    type: instr.NEW_FACT,
                    payload: ""
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.NEW_FACT,
                    payload: ""
                });
            });
    };
}

export function putGroupAddAssertedAsset(modelId, groupId, asset) {
    console.log("putGroupAddAssertedAsset: ", groupId, asset);
    return function(dispatch) {
        //indicate that grouping process has started
        //this includes an add asset event, plus an asset relocation (to follow)
        dispatch({
            type: instr.GROUPING,
            payload: {
                group: groupId,
                asset: asset.id,
                groupUpdated: false,
                locationUpdated: false,
                inProgress: true
            }
        });

        axiosInstance
            .post("/models/" + modelId + "/assetGroups/" + groupId + "/addAsset/" + asset.id, asset)
            .then((response) => {
                dispatch({
                    type: instr.PUT_GROUP_ADD_ASSET,
                    payload: {
                        group: response.data,
                        asset: asset
                    }
                });
            });
    };
}

export function putGroupRemoveAssertedAsset(modelId, groupId, asset) {
    console.log("putGroupRemoveAssertedAsset: ", groupId, asset);
    return function(dispatch) {

        axiosInstance
            .post("/models/" + modelId + "/assetGroups/" + groupId + "/removeAsset/" + asset.id, asset)
            .then((response) => {
                dispatch({
                    type: instr.PUT_GROUP_REMOVE_ASSET,
                    payload: {
                        group: response.data,
                        asset: asset
                    }
                });
            });
    };
}

export function moveAssertedAssetGroup(modelId, assetId, groupId, targetGroupId, updatedAsset) {
    console.log("moveAssertedAssetGroup: ", assetId, groupId, targetGroupId, updatedAsset);
    return function(dispatch) {
        axiosInstance
            .post("/models/" + modelId + "/assetGroups/" + groupId + "/moveAsset/" + assetId + "/toGroup/" + targetGroupId, updatedAsset)
            .then((response) => {
                dispatch({
                    type: instr.MOVE_ASSET_TO_GROUP,
                    payload: {
                        sourceGroup: response.data.sourceGroup,
                        targetGroup: response.data.targetGroup,
                        asset: updatedAsset
                    }
                });
            });
    };
}

export function putAssertedGroupRelocate(modelId, updatedGroup) {

    return function (dispatch) {
        //dispatch({
        //    type: instr.MOVING_GROUP
        //});
        axiosInstance
            .put("/models/" + modelId + "/assetGroups/" + updatedGroup.id + "/location", updatedGroup)
            .then((response) => {
                dispatch({
                    type: instr.PUT_GROUP_LOC,
                    payload: response.data
                });
            });
    };
}

export function putAssertedGroupResize(modelId, updatedGroup) {

    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assetGroups/" + updatedGroup.id + "/size", updatedGroup)
            .then((response) => {
                dispatch({
                    type: instr.PUT_GROUP_RESIZE,
                    payload: response.data
                });
            });
    };
}

export function putAssertedGroupRename(modelId, group) {
    console.log("putAssertedGroupRename for modelId, group", modelId, group);

    let cleanedLabel = group.label.replace(/\\/g, '/');
    let updatedGroup = {
        id: group.id,
        uri: group.uri,
        label: cleanedLabel
    };

    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assetGroups/" + updatedGroup.id + "/label", updatedGroup)
            .then((response) => {
                dispatch({
                    type: instr.PUT_GROUP_RENAME,
                    payload: response.data
                });
            });
    };
}

export function putGroupExpanded(modelId, updatedGroup) {
    console.log("putGroupExpanded: ", updatedGroup);
    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assetGroups/" + updatedGroup.id + "/expanded", updatedGroup)
            .then((response) => {
                dispatch({
                    type: instr.PUT_GROUP_EXPANDED,
                    payload: response.data
                });
            });
    };
}

export function deleteAssertedAssetGroup(modelId, group, deleteAssets) {
    console.log("deleteAssertedAssetGroup: ", group.id, deleteAssets);
    let groupId = group.id;
    
    return function (dispatch) {
        if (!deleteAssets && (group.assetIds.length > 0)) {
            //indicate that (un)grouping process has started
            //this includes a delete group event, plus asset(s) relocation (to follow)
            //N.B. This approach is only used if there are any assets to ungroup!
            dispatch({
                type: instr.GROUPING,
                payload: {
                    group: groupId,
                    groupUpdated: false,
                    locationUpdated: false,
                    inProgress: true
                }
            });
        }

        axiosInstance
            .delete("/models/" + modelId + "/assetGroups/" + groupId + "?deleteAssets=" + deleteAssets)
            .then((response) => {
                dispatch({
                    type: instr.DELETE_GROUP,
                    payload: {
                        groupId: groupId,
                        deleteAssets: deleteAssets,
                        response: response.data
                    }
                });
            });
    };
}

export function printModelState() {
    return function(dispatch) {
        dispatch({
            type: instr.PRINT_MODEL_STATE,
            payload: null
        })
    }
}

export function putAssertedAssetCardinality(modelId, asset) {
    let updatedAsset = {
        id: asset.id,
        uri: asset.uri,
        minCardinality: asset.minCardinality,
        maxCardinality: asset.maxCardinality
    };

    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_CARD_LOADING,
            payload: true
        });

        axiosInstance
            .put("/models/" + modelId + "/assets/" + updatedAsset["id"] + "/cardinality", updatedAsset)
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_CARD_LOADING,
                    payload: false
                });
                dispatch({
                    type: instr.PUT_ASSET_CARD,
                    payload: response.data
                });
            })
            .catch((error) => {
                dispatch({
                    type: instr.UPDATE_CARD_LOADING,
                    payload: false
                });
            });
    };
}

export function putAssertedAssetPopulation(modelId, updatedAsset) {
    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assets/" + updatedAsset["id"] + "/population", updatedAsset)
            .then((response) => {
                dispatch({
                    type: instr.PUT_ASSET_POPULATION,
                    payload: {
                        updatedAsset: updatedAsset
                    }
                });
            });
    };
}

export function putAssertedAssetRename(modelId, assetId, asset) {
    let cleanedLabel = asset.label.replace(/\\/g, '/');
    let updatedAsset = {
        id: asset.id,
        uri: asset.uri,
        label: cleanedLabel
    };

    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assets/" + assetId + "/label", updatedAsset)
            .then((response) => {
                dispatch({
                    type: instr.PUT_ASSET_NAME,
                    payload: response.data
                });
            });
    };
}

export function putAssertedAssetType(modelId, assetId, asset) {
    let updatedAsset = {
        id: asset.id,
        uri: asset.uri,
        label: asset.label,
        iconX: asset.iconX,
        iconY: asset.iconY,
        minCardinality: asset.minCardinality,
        maxCardinality: asset.maxCardinality,
        type: asset.type
    };

    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_ASSET_LOADING,
            payload: true
        });

        axiosInstance
            .put("/models/" + modelId + "/assets/" + assetId + "/type", updatedAsset)
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_ASSET_LOADING,
                    payload: false
                });
                dispatch({
                    type: instr.PUT_ASSET_TYPE,
                    payload: response.data
                });
            });
    };
}

export function relocateAssets(modelId, assetArray) {
    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assets/updateLocations", JSON.stringify({"assets": assetArray}))
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_ASSET_LOCS,
                    payload: assetArray
                });
            });
    };
}

export function putAssertedAssetRelocate(modelId, asset) {
    let updateLocationRequest = {
        iconX: asset.iconX,
        iconY: asset.iconY
    };

    return function (dispatch) {
        dispatch({
            type: instr.MOVING_ASSET
        });
        axiosInstance
            .put("/models/" + modelId + "/assets/" + asset.id + "/location", updateLocationRequest)
            .then((response) => {
                dispatch({
                    type: instr.PUT_ASSET_LOC,
                    payload: {...updateLocationRequest, id: asset.id }
                });
            });
    };
}

export function deleteAssertedAsset(modelId, assetId) {
    return function (dispatch) {
        axiosInstance
            .delete("/models/" + modelId + "/assets/" + assetId)
            .then((response) => {
                dispatch({
                    type: instr.DELETE_ASSET,
                    payload: response.data
                });
            });
    };
}

export function changeSelectedAsset(selectedAssetId) {
    return function (dispatch) {
        dispatch({
            type: instr.CHANGE_SELECTED_ASSET,
            payload: {id: selectedAssetId}
        });
    };
}

export function changeSelectedTwas(twas) {
    return function (dispatch) {
        dispatch({
            type: instr.CHANGE_SELECTED_ASSET,
            payload: {uri: twas.asset}
        });
        dispatch({
            type: instr.CHANGE_SELECTED_TWAS,
            payload: {
                twas: twas
            }
        });
    };
}

export function retrieveMetaData(modelId, asset) {
    return function (dispatch) {
        axiosInstance
            .get("/models/" + modelId + "/assets/" + asset["id"] + "/meta")
            .then((response) => {
                dispatch({
                    type: instr.GET_METADATA,
                    payload: {
                        asset: asset,
                        metaData: response.data
                    }
                });
            });
    };
}

export function updateMetaData(modelId, asset, metaData) {
    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assets/" + asset["id"] + "/meta", JSON.stringify(metaData))
            .then((response) => {
                dispatch({
                    type: instr.PUT_METADATA,
                    payload: {
                        asset: asset,
                        metaData: response.data
                    }
                });
            });
    };
}

export function showInferredRelations(showInferredRelations) {
    return function (dispatch) {
        dispatch({
            type: instr.SHOW_INFERRED_RELATIONS,
            payload: { showInferredRelations }
        })
    }
}

export function showHiddenRelations(showHiddenRelations) {
    return function (dispatch) {
        dispatch({
            type: instr.SHOW_HIDDEN_RELATIONS,
            payload: { showHiddenRelations }
        })
    }
}

export function reCentreCanvas(newState) {
    return function (dispatch) {
        dispatch({
            type: instr.RECENTRE_CANVAS,
            payload: { newState }
        })
    }
}

export function reCentreModel(newState) {
    return function (dispatch) {
        dispatch({
            type: instr.RECENTRE_MODEL,
            payload: { newState }
        })
    }
}

export function changeSelectedInferredAsset(selectedInferredAssetId) {
    //console.log("changeSelectedInferredAsset: " + selectedInferredAssetId);
    return function (dispatch) {
        dispatch({
            type: instr.CHANGE_SELECTED_INFERRED_ASSET,
            payload: {id: selectedInferredAssetId}
        });
    };
}

export function assetHistoryForward() {
    return function (dispatch) {
        dispatch({
            type: instr.ASSET_HISTORY_FORWARD
        });
    };
}

export function assetHistoryBack() {
    return function (dispatch) {
        dispatch({
            type: instr.ASSET_HISTORY_BACK
        });
    };
}

export function deleteAssertedRelation(modelId, relationId, from, type, to) {
    return function (dispatch) {
        dispatch({
            type: instr.DELETE_RELATION,
            payload: {
                relationId: relationId,
                started: true
            }
        });

        axiosInstance
            .delete("/models/" + modelId + "/relations/" + from + "-" + type + "-" + to)
            .then((response) => {
                console.log(response);
                dispatch({
                    type: instr.DELETE_RELATION,
                    payload: {
                        relations: response.data["relations"],
                        valid: response.data["valid"],
                        status: response.status
                    }
                });
            });
    };
}

export function suppressCanvasRefresh(value) {
    //console.log("suppressCanvasRefresh: " + value);
    return function (dispatch) {
        dispatch({
            type: instr.SUPPRESS_CANVAS_REFRESH,
            payload: {
                value: value
            }
        });
    };
}

export function redrawRelations() {
    //console.log("redrawRelations");
    return function (dispatch) {
        dispatch({
            type: instr.REDRAW_RELATIONS
        });
    };
}

export function toggleThreatEditor(open, threatId) {
    //console.log("toggleThreatEditor: " + open + " (threatId = " + threatId + ")");
    return function (dispatch) {
        dispatch({
            type: instr.TOGGLE_THREAT_EDITOR,
            payload: {
                toggle: open,
                threatId: threatId
            }
        });
    };
}

export function togglePanel(panel, expanded) {
    return function (dispatch) {
        dispatch({
            type: instr.TOGGLE_PANEL,
            payload: {
                panel: panel,
                expanded: expanded
            }
        });
    };
}

export function toggleFilter(panel, filter, selected) {
    return function (dispatch) {
        dispatch({
            type: instr.TOGGLE_FILTER,
            payload: {
                panel: panel,
                filter: filter,
                selected: selected
            }
        });
    };
}

export function activateThreatEditor() {
    //console.log("activateThreatEditor");
    return function (dispatch) {
        dispatch({
            type: instr.ACTIVATE_THREAT_EDITOR
        });
    };
}

export function activateMisbehaviourExplorer() {
    //console.log("activateMisbehaviourExplorer");
    return function (dispatch) {
        dispatch({
            type: instr.ACTIVATE_MISBEHAVIOUR_EXPLORER
        });
    };
}

export function activateComplianceExplorer() {
    //console.log("activateComplianceExplorer");
    return function (dispatch) {
        dispatch({
            type: instr.ACTIVATE_COMPLIANCE_EXPLORER
        });
    };
}

export function activateControlExplorer(control) {
    return function (dispatch) {
        dispatch({
            type: instr.ACTIVATE_CONTROL_EXPLORER,
            payload: {
                selectedControl: control
            }
        });
    };
}
export function activateThreatFilter(id, value) {
    //console.log("activateThreatFilter: " + id + " = " + value);
    return function (dispatch) {
        dispatch({
            type: instr.ACTIVATE_THREAT_FILTER,
            payload: {
                id: id,
                active: value
            }
        });
    };
}

export function activateAcceptancePanel(value) {
    //console.log("activateAcceptancePanel: " + value);
    return function (dispatch) {
        dispatch({
            type: instr.ACTIVATE_ACCEPTANCE_PANEL,
            payload: {
                active: value
            }
        });
    };
}

export function updateSidePanelWidths(widthsObj) {
    return function (dispatch) {
        dispatch({
            type: instr.SIDE_PANEL_WIDTHS,
            payload: widthsObj
        });
    };
}

export function sidePanelActivated() {
    //console.log("sidePanelActivated");
    return function (dispatch) {
        dispatch({
            type: instr.SIDE_PANEL_ACTIVATED
        });
    };
}

export function sidePanelDeactivated() {
    //console.log("sidePanelDeactivated");
    return function (dispatch) {
        dispatch({
            type: instr.SIDE_PANEL_DEACTIVATED
        });
    };
}

/* KEM - doesn't seem to be used
export function openMisbehaviourExplorer(modelId, misbehaviourId, updateRootCausesModel) {
    console.log("openMisbehaviourExplorer: " + open + " (threatId = " + threatId + ")");
    return function (dispatch) {
        dispatch({
            type: instr.TOGGLE_THREAT_EDITOR,
            payload: {
                toggle: open,
                threatId: threatId
            }
        });
    };
}
*/

export function closeMisbehaviourExplorer() {
    //console.log("closeMisbehaviourExplorer");
    return function (dispatch) {
        dispatch({
            type: instr.CLOSE_MISBEHAVIOUR_EXPLORER
        });
    };
}

export function openComplianceExplorer() {
    //console.log("openComplianceExplorer");
    return function (dispatch) {
        dispatch({
            type: instr.OPEN_COMPLIANCE_EXPLORER
        });
    };
}

export function closeComplianceExplorer() {
    //console.log("closeComplianceExplorer");
    return function (dispatch) {
        dispatch({
            type: instr.CLOSE_COMPLIANCE_EXPLORER
        });
    };
}

export function openControlExplorer(control) {
    return function (dispatch) {
        dispatch({
            type: instr.OPEN_CONTROL_EXPLORER,
            payload: {
                selectedControl: control,
            }
        });
    };
}

export function closeControlExplorer() {
    return function (dispatch) {
        dispatch({
            type: instr.CLOSE_CONTROL_EXPLORER
        });
    };
}

export function openControlStrategyExplorer(csg, context) {
    return function (dispatch) {
        dispatch({
            type: instr.OPEN_CONTROL_STRATEGY_EXPLORER,
            payload: {
                selectedControlStrategy: csg,
                context: context
            }
        });
    };
}

export function closeControlStrategyExplorer() {
    return function (dispatch) {
        dispatch({
            type: instr.CLOSE_CONTROL_STRATEGY_EXPLORER
        });
    };
}

export function openRecommendationsExplorer(csg, context) {
    return function (dispatch) {
        dispatch({
            type: instr.OPEN_RECOMMENDATIONS_EXPLORER,
        });
    };
}

export function closeRecommendationsExplorer() {
    return function (dispatch) {
        dispatch({
            type: instr.CLOSE_RECOMMENDATIONS_EXPLORER
        });
    };
}

export function openReportDialog(reportType) {
    return function (dispatch) {
        dispatch({
            type: instr.OPEN_REPORT_DIALOG,
            payload: {reportType: reportType}
        });
    };
}

export function closeReportDialog() {
    return function (dispatch) {
        dispatch({
            type: instr.CLOSE_REPORT_DIALOG
        });
    };
}

export function updateControlOnAsset(modelId, assetId, updatedControl) {
    //build request body for updated CS
    let updatedCS = {
        uri: updatedControl.uri,
        proposed: updatedControl.proposed,
        workInProgress: updatedControl.workInProgress
    };

    return function (dispatch) {
        axiosInstance.put("/models/" + modelId + "/assets/" + assetId + "/control", updatedCS)
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_CONTROLS,
                    payload: {
                        controlsUpdate: response.data,
                        controlsReset: false
                    }
                })
            });
    };
}

export function updateControlCoverageOnAsset(modelId, assetId, updatedControl) {
    //build request body for updated CS
    let updatedCS = {
        uri: updatedControl.uri,
        proposed: updatedControl.proposed,
        workInProgress: updatedControl.workInProgress,
        coverageLevel: updatedControl.coverageLevel
    };

    //create controlsUpdateRequest for dispatch (only a single control in the array)
    let controlsUpdateRequest = {
        controls: [updatedControl.uri],
        proposed: updatedControl.proposed,
        workInProgress: updatedControl.workInProgress,
        coverageLevel: updatedControl.coverageLevel,
        coverageAsserted: true
    };
    
    return function (dispatch) {
        axiosInstance.put("/models/" + modelId + "/assets/" + assetId + "/control", updatedCS)
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_CONTROLS,
                    payload: {
                        controlsUpdate: controlsUpdateRequest,
                        controlsReset: false
                    }
                })
            });
    };
}

export function revertControlCoverageOnAsset(modelId, assetId, controlSet) {
    let cs = {
        uri: controlSet.uri
    };
    
    return function (dispatch) {
        axiosInstance.post("/models/" + modelId + "/assets/" + assetId + "/revert-control-coverage", cs)
            .then((response) => {
                let updatedControl = response.data;
                
                let controlsUpdate = {
                    controls: [updatedControl.uri],
                    proposed: updatedControl.proposed,
                    workInProgress: updatedControl.workInProgress,
                    coverageLevel: updatedControl.coverageLevel,
                    coverageAsserted: updatedControl.coverageAsserted
                };

                dispatch({
                    type: instr.UPDATE_CONTROLS,
                    payload: {
                        controlsUpdate: controlsUpdate,
                        controlsReset: false
                    }
                })
            });
    };
}

//Reset controls (i.e. "Reset All" button)
export function resetControls(modelId, controls) {
    return updateControls(modelId, controls, false, false, true);
}

//Update multiple controls with new proposed value
export function updateControls(modelId, controls, proposed, workInProgress, controlsReset) {
    console.log("updateControls: controlsReset = " + controlsReset);
    let controlsUpdateRequest = {
        controls: controls,
        proposed: proposed,
        workInProgress: workInProgress
    };
    
    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assets/controls", controlsUpdateRequest)
            .then((response) => {
                dispatch({
                    type: instr.UPDATE_CONTROLS,
                    payload: {
                        controlsUpdate: response.data,
                        controlsReset: controlsReset
                    }
                })
            });
    };
}

export function updateTwasOnAsset(modelId, assetId, updatedTwas) {
    //console.log("updateTwasOnAsset:", modelId, assetId, updatedTwas);
    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assets/" + assetId + "/twas", updatedTwas)
            .then((response) => {
                updatedTwas.twLevelAsserted = true;
                dispatch({
                    type: instr.UPDATE_TWAS,
                    payload: {
                        updatedTwas: updatedTwas
                    }
                })
            });
    };
}

export function revertAssertedTwasOnAsset(modelId, assetId, twas) {
    let twasUri = {
        uri: twas.uri
    };
    
    return function (dispatch) {
        axiosInstance.post("/models/" + modelId + "/assets/" + assetId + "/revert-twas", twasUri)
            .then((response) => {
                let updatedTwas = response.data;
                
                dispatch({
                    type: instr.UPDATE_TWAS,
                    payload: {
                        updatedTwas: updatedTwas
                    }
                })
            });
    };
}

export function updateMisbehaviourImpact(modelId, updatedMisbehaviour) {
    let misbehaviourId = updatedMisbehaviour.id;
    //console.log("updatedMisbehaviour: ", updatedMisbehaviour);

    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/misbehaviours/" + misbehaviourId + "/impact", updatedMisbehaviour)
            .then((response) => {
                updatedMisbehaviour.impactLevelAsserted = true;
                dispatch({
                    type: instr.UPDATE_MISBEHAVIOUR_IMPACT,
                    payload: {
                        misbehaviour: updatedMisbehaviour,
                    }
                })
            });
    };
}

export function revertMisbehaviourImpact(modelId, misbehaviour) {
    let misbehaviourId = misbehaviour.id;
    
    return function (dispatch) {
        axiosInstance.post("/models/" + modelId + "/misbehaviours/" + misbehaviourId + "/revert-impact", misbehaviour)
            .then((response) => {
                let updatedMisbehaviour = response.data;
                dispatch({
                    type: instr.UPDATE_MISBEHAVIOUR_IMPACT,
                    payload: {
                        misbehaviour: updatedMisbehaviour
                    }
                })
            });
    };
}

export function updateThreat(modelId, assetId, threatId, updatedThreat) {
    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/assets/" + assetId + "/threats/" + threatId, updatedThreat)
            .then((response) => {
                dispatch({
                    type: instr.GET_COMPILED_ASSET_DETAILS,
                    payload: {
                        threats: response.data["threats"],
                        controlSets: response.data["controlSets"]
                    }
                });
            });
    };
}

export function toggleAcceptThreat(modelId, threatId, threat) {
    console.log("toggleAcceptThreat id:" + threatId + ", model:" + modelId + ":\n" + threat.acceptanceJustification);
    let updatedThreat = {
        id: threat.id,
        uri: threat.uri,
        acceptanceJustification: threat.acceptanceJustification,
    };

    return function (dispatch) {
        axiosInstance
            .post("/models/" + modelId + "/threats/" + threatId + "/accept", updatedThreat)
            .then((response) => {
                console.log(response);
                dispatch({
                    type: instr.ACCEPT_THREAT,
                    payload: {
                        threat: response.data
                    }
                });
            });
    };
}

export function hideRelation(relationId) {
    return function (dispatch) {
        dispatch({
            type: instr.HIDE_RELATION,
            payload: {
                relationId: relationId,
            }
        });
    }
}

export function putRelationRedefine(modelId, relation) {
    let { assetIdFrom, assetIdTo, relType, relationId } = relation;
    console.log(`Updating relation ${relationId} from ${assetIdFrom} to ${assetIdTo}`);

    return function (dispatch) {
        axiosInstance
            .put("/models/" + modelId + "/relations/" + relationId, {
                "fromID": assetIdFrom,
                "toID": assetIdTo,
                "type": relType["type"],
                "label": relType["label"],
                "asserted": true,
                "visible": true
            })
            .then((response) => {
                dispatch({
                    type: instr.PATCH_RELATION,
                    payload: {
                        relationId: relationId,
                        relation: response.data
                    }
                });
            });
    };
}

export function uploadScreenshot(modelId) {
    return axiosInstance.post("/models/" + modelId + "/takescreenshot");
}

export function updateLayerSelection(selection) {
    return function (dispatch) {
        dispatch({
            type: instr.UPDATE_LAYER_SELECTION,
            payload: selection
        });
    };
}

//TODO: rename this to something like selectMisbehaviour
export function getRootCauses(modelId, misbehaviour) {
    return function (dispatch) {
        dispatch({
            type: instr.GET_ROOT_CAUSES,
            payload: {
                misbehaviour: misbehaviour
            }
        });
    };
}

export function getControlStrategiesForControlSet(uri, threats, controlStrategies) {
    var csgs = {};

    threats.forEach((threat) => {
        Object.keys(threat.controlStrategies).forEach((csgUri) => {
            let csg = controlStrategies[csgUri];
            csg.mandatoryControlSets.forEach((csUri) => {
                if (csUri === uri) {
                    csgs[csgUri] = csg;
                };
            });
            csg.optionalControlSets.forEach((csUri) => {
                if (csUri === uri) {
                    csgs[csgUri] = csg;
                }
            });
        })
    });

    return csgs;
}

export function toggleDeveloperMode() {
    return function(dispatch) {
        dispatch({
            type: instr.TOGGLE_DEVELOPER_MODE
        });
    };
}

export function getThreatGraph(modelId, riskMode, msUri) {
    return function(dispatch) {
        dispatch({
            type:instr.LOADING_ATTACK_PATH,
            payload: true
        });
        let shortUri = msUri.split("#")[1];
        let uri = '/models/' + modelId + "/threatgraph?riskMode=" + riskMode + "&allPath=false&normalOperations=false&targetURIs=system%23" + shortUri;
        axiosInstance.get(uri).then(response => {
            dispatch({
                type:instr.GET_ATTACK_PATH,
                payload: {"threats": response.data['graphs']["system#"+shortUri]['threats'],
                           "prefix": response.data['uriPrefix']
                         }
            })
        }).catch(error => {
            console.log("ERROR getting attack path:", error);
            dispatch({
                type:instr.LOADING_ATTACK_PATH,
                payload: false
            });
        });
    };
}

export function getShortestPathPlot(modelId, riskMode) {
    return function(dispatch) {
        axiosInstance.get("/models/" + modelId + "/authz").then(response => {
            console.log("DATA: ", response.data.readUrl);
            let readUrl = response.data.readUrl;
            let url = process.env.config.API_END_POINT + "/adaptor/api/v2/models/" + readUrl + "/path_plot?risk_mode=" + riskMode + "&retain_cs_changes=false&direct=true&export_format=svg";
            console.log("openning new window for URL: " + url);
            dispatch({
                type:instr.OPEN_GRAPH_WINDOW,
                payload: url
            })
        });
    };
}

export function getRecommendationsBlocking(modelId, riskMode) {
    return function(dispatch) {
        dispatch({
            type: instr.IS_CALCULATING_RECOMMENDATIONS
        });

        axiosInstance
            .get("/models/" + modelId + "/recommendations", {params: {riskMode: riskMode}})
            .then((response) => { 
                dispatch({
                    type: instr.IS_NOT_CALCULATING_RECOMMENDATIONS,
                });
                dispatch({
                    type: instr.RECOMMENDATIONS_RESULTS,
                    payload: response.data
                });
                dispatch({
                    type: instr.OPEN_WINDOW,
                    payload: "recommendationsExplorer"
                });
            })
            .catch((error) => {
                console.log("Error:", error);
                dispatch({
                    type: instr.IS_NOT_CALCULATING_RECOMMENDATIONS //fix
                });
            });
    };
}

export function getRecommendations(modelId, riskMode, acceptableRiskLevel, msUri, localSearch) {
    console.log("Called getRecommendations");

    let shortUri = msUri ? msUri.replace(Constants.URI_PREFIX, "") : null;

    console.log("riskMode = ", riskMode);
    console.log("acceptableRiskLevel = ", acceptableRiskLevel);
    console.log("msUri = ", msUri);
    console.log("shortUri = ", shortUri);
    console.log("localSearch = ", localSearch);

    return function(dispatch) {
        dispatch({
            type: instr.IS_CALCULATING_RECOMMENDATIONS
        });

        axiosInstance
            .get("/models/" + modelId + "/recommendations", {params: {riskMode: riskMode, 
                                                                acceptableRiskLevel: acceptableRiskLevel,
                                                                targetURIs: shortUri,
                                                                localSearch: localSearch,
                                                            }})
            .then((response) => {
                dispatch({
                    type: instr.RECOMMENDATIONS_JOB_STARTED,
                    payload: response.data
                });
            })
            .catch((error) => {
                console.log("Error:", error);
                dispatch({
                    type: instr.IS_NOT_CALCULATING_RECOMMENDATIONS //fix
                });
            });
    };
}

/*
 export function hoverThreat (show, threat) {
 return function (dispatch) {
 dispatch({
 type: instr.HOVER_THREAT,
 payload: {
 show: show,
 threat: threat
 }
 });
 };
 }
 */
