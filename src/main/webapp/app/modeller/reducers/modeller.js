import * as instr from "../modellerConstants";
import setupJsPlumb from "../components/util/TileFactory";
import {setThreatTriggeredStatus} from "../components/util/ThreatUtils";

const modelState = {
    model: {
        id: "",
        name: "",
        palette: {
            assets: [],
            connections: {},
            layers: [],
            dataTypes: []
        },
        assets: [],
        relations: [],
        controlSets: [],
        controlStrategies: {},
        threats: [],
        complianceSets: [],
        complianceThreats: [],
        misbehaviourSets: {},
        valid: true,
        threatsUpdated: false,
        validating: false,
        //riskLevelsValid: true,  //don't set this initially (button will be coloured blue)
        saved: true,
        calculatingRisks: false,
        controlsReset: false,
        canBeEdited: true,
        canBeShared: true
    },
    // Rayna: TODO - when the backend for groups is implemented, put this array in the model above.
    groups: [],
    grouping: {
        group: "",
        asset: "",
        groupUpdated: false,
        locationUpdated: false,
        inProgress: false
    },
    movedAsset: false,
    selectedLayers: [
        "all"
    ],
    selectedAsset: {
        id: "",
        isThreatEditorVisible: false,
        isThreatEditorActive: false,
        controlSets: [],
        misbehaviours: [],
        selectedMisbehaviour: {},
        selectedControl: "",
        loadingControlsAndThreats: false,
        loadingCausesAndEffects: false,
        threats: [], // TODO: check if still required
    },
    selectedControlStrategy: [], //may be more than one
    csgExplorerContext: {}, //indicates if CSG(s) are related to a control set, etc
    selectedThreat: {
        id: ""
    },
    selectedMisbehaviour: {
        misbehaviour: {},
        loadingRootCauses: false
    },
    expanded: {
        assetDetails: {
            twas: false
        }
    },
    misbehaviourTwas: {},
    isMisbehaviourExplorerVisible: false,
    isMisbehaviourExplorerActive: false,
    isComplianceExplorerVisible: false,
    isComplianceExplorerActive: false,
    isControlExplorerVisible: false,
    isControlExplorerActive: false,
    isControlStrategyExplorerVisible: false,
    isControlStrategyExplorerActive: false,
    isReportDialogVisible: false,
    isReportDialogActive: false,
    isDroppingInferredGraph: false,
    threatFiltersActive: {
        "asset-threats": false,
        "direct-causes": false,
        "root-causes": false,
        "compliance-threats": false
    },
    isAcceptancePanelActive: false,
    suppressCanvasRefresh: false,
    redrawRelations: 0,
    selectedInferredAsset: "",
    developerMode: false,
    view: {
        showInferredRelations: false,
        showHiddenRelations: false,
        reCentreCanvas: false,
        reCenterModel: false,
        leftSidePanelWidth: 180,
        rightSidePanelWidth: 440
    },
    validationProgress: {
        status: "inactive",
        progress: 0.0,
        message: "Validating model",
        error: "",
        waitingForUpdate: false
    },
    loadingProgress: {
        progress: 0.0,
        message: "Loading model",
        waitingForUpdate: false
    },
    loading: {
        threats: false,
        details: false,
        model: false,
        asset: false,
        cardinality: false,
        newFact: []
    },
    canvas: {
        zoom: 1.0,
        //transformOrigin: [0.5, 0.5]
        transformOrigin: [5000, 5000]
    },
    sidePanelActivated: false,
    history: [""],
    historySize: 20,
    historyPointer: 0,
    backEnabled: false,
    forwardEnabled: false,
    reportType: "technicalReport",
    authz: {
        userEdit: false,
    }
};

jsPlumb.bind("ready", () => {
    console.log("--- jsPlumb is ready ---");
    setupJsPlumb();
   
    /*
    console.log("modeller jsPlumb.bind group:add");
    jsPlumb.bind("group:add", (group) => {
        console.log("--- jsPlumb group:add ---", group);
        alert("group:add");
    });

    console.log("modeller jsPlumb.bind group:addMember on group");
    jsPlumb.bind("group:addMember", (group, el, sourceGroup) => {
        console.log("--- jsPlumb group addMember ---", el);
        console.log("--- jsPlumb group: ", group);
        alert("group:addMember");
    });

    console.log("jsPlumb after bind: ", jsPlumb);
    */
});

//Provides a way of updating the store for the modeller, based on a set of instructions defined in dashboardConstants.js
export default function modeller(state = modelState, action) {

    if ( action.type !== instr.SIDE_PANEL_ACTIVATED &&
         action.type !== instr.SIDE_PANEL_DEACTIVATED &&
         action.type !== instr.SIDE_PANEL_WIDTHS
       )
    {
        console.log(action.type);
    }
    
    if (action.type === instr.NEW_FACT) {
        //console.log("NEW_FACT: ", action.payload);
       
        return {
            ...state,
            loading: {
                ...state.loading,
                newFact: (action.payload === "" ? state.loading.newFact.slice(1, state.loading.newFact.length) : state.loading.newFact.concat([action.payload]))
            }
        }
    }

    if (action.type === instr.GET_MODEL) {

        console.log("modellerReducer: get model");

        var valid = action.payload["valid"];
        var selectedAssetId = state.selectedAsset["id"];

        //printThreats(action.payload.threats);

        let model = action.payload;
        model.saved = true; //must be true if reloaded

        let groups = model.groups;

        //sort the levels arrays
        sortLevels(model.levels);

        let palette = state.model.palette;

        if (model.palette === undefined) {
            //re-save the palette into the model
            model.palette = palette;
        }

        //Create map of misbehaviour to related twas
        let misbehaviourTwas = {};
        Object.values(model.twas).forEach(twas => {
            let msUri = twas.causingMisbehaviourSet;
            if (msUri) {
                misbehaviourTwas[msUri] = twas.uri;
            }
        });

        let misbehaviour = state.selectedMisbehaviour.misbehaviour;

        //update the selected misbehaviour, if defined
        if (misbehaviour.uri) {
            misbehaviour = model.misbehaviourSets[misbehaviour.uri];
        }

        //set trigger flags on all threats
        if (model.threats) {
            model.threats.forEach(threat => setThreatTriggeredStatus(threat, model.controlStrategies));
        }

        return {
            ...state,
            model: model,
            groups: groups,
            selectedAsset: {
                ...state.selectedAsset,
                id: selectedAssetId,
            },
            selectedMisbehaviour: {
                ...state.selectedMisbehaviour,
                misbehaviour: misbehaviour,
            },
            misbehaviourTwas: misbehaviourTwas
        };
    }

    if (action.type === instr.GET_AUTHZ) {
        let authz = action.payload;
        console.log("GET_MODEL_AUTHZ: " + authz);
        let editUsers = action.payload.data.writeUsernames;
        let ownerUsers = action.payload.data.ownerUsernames;

        if (
            !ownerUsers.indexOf(this.props.auth.user.username) > -1 ||
            !editUsers.indexOf(auth.user.username) > -1
        ) {
            return {
                ...state,
                authz: {
                    ...state.authz,
                    userEdit: false,
                },
            };
        }
    }

    if (action.type === instr.UPDATE_EDIT) {
        let edit = action.payload;
            return {
                ...state,
                authz: {
                    ...state.authz,
                    userEdit: edit,
                },
            };
        
    }



    if (action.type === instr.GET_MODEL_LOADING_ID) {
        let loadingId = action.payload;
        console.log("GET_MODEL_LOADING_ID: " + loadingId);
        return {
            ...state,
            model: {
                ...state.model,
                loadingId: loadingId
            },
        };
    }

    if (action.type === instr.GET_PALETTE) {
        let palette = action.payload;
        console.log("GET_PALETTE:");
        console.log(palette);
        return {
            ...state,
            model: {
                ...state.model,
                palette: palette
            },
        };
    }

    if (action.type === instr.EDIT_MODEL) {
        let updatedModel = action.payload;
        //console.log("EDIT_MODEL:", updatedModel);
        return {
            ...state,
            model: {
                ...state.model,
                name: updatedModel.name,
                description: updatedModel.description
            }
        };
    }

    if (action.type === instr.UPDATE_VALIDATION_PROGRESS) {
        if (action.payload.waitingForUpdate) {
            //console.log("poll: UPDATE_VALIDATION_PROGRESS: (waiting for progress)");
            return {
                ...state, validationProgress: {
                    ...state.validationProgress,
                    waitingForUpdate: action.payload.waitingForUpdate
                }
            };
        }

        let status = "running";

        if (action.payload.status) {
            status = action.payload.status;
        }
        else if (action.payload.message.indexOf("failed") != -1) {
            console.log("Validation failed (detected from message)");
            status = "failed";
        }
        else if (action.payload.message.indexOf("complete") != -1) {
            console.log("Validation completed (detected from message)");
            status = "completed";
        }

        let error = action.payload.error != null ? action.payload.error : "";

        return {
            ...state, validationProgress: {
                status: status,
                progress: action.payload.progress,
                message: action.payload.message,
                error: error,
                waitingForUpdate: action.payload.waitingForUpdate
            }
        };
    }

    if (action.type === instr.UPDATE_RISK_CALC_PROGRESS) {
        //console.log("UPDATE_RISK_CALC_PROGRESS", action.payload);
        if (action.payload.waitingForUpdate) {
            //console.log("poll: UPDATE_RISK_CALC_PROGRESS: (waiting for progress)");
            return {
                ...state, validationProgress: {
                    ...state.validationProgress,
                    waitingForUpdate: action.payload.waitingForUpdate
                }
            };
        }

        let status = "running";

        if (action.payload.status) {
            status = action.payload.status;
        }
        else if (action.payload.message.indexOf("failed") != -1) {
            console.log("Risk calc failed (detected from message)");
            status = "failed";
        }
        else if (action.payload.message.indexOf("complete") != -1) {
            console.log("Risk calc completed (detected from message)");
            status = "completed";
        }

        let error = action.payload.error != null ? action.payload.error : "";

        return {
            ...state, validationProgress: {
                status: status,
                progress: action.payload.progress,
                message: action.payload.message,
                error: error,
                waitingForUpdate: action.payload.waitingForUpdate
            }
        };
    }

    if (action.type === instr.UPDATE_LOADING_PROGRESS) {
        //console.log("UPDATE_LOADING_PROGRESS:", action.payload);
        if (action.payload.waitingForUpdate) {
            //console.log("poll: UPDATE_LOADING_PROGRESS: (waiting for progress)");
            return {
                ...state, loadingProgress: {
                    ...state.loadingProgress,
                    waitingForUpdate: action.payload.waitingForUpdate
                }
            };
        }
        else {
            //console.log("poll: UPDATE_LOADING_PROGRESS: " + action.payload.progress);
            //KEM: LoadingOverlay now requires a response, or next poll request will not happen
            //if (state.loadingProgress.progress !== action.payload.progress) {
                //console.log("poll: updating progress: " + action.payload.progress);
                return {
                    ...state, loadingProgress: {
                        progress: action.payload.progress,
                        status: action.payload.status,
                        error: action.payload.error,
                        message: action.payload.message,
                        waitingForUpdate: action.payload.waitingForUpdate
                    }
                };
            //} else {
            //    return state;
            //}
        }
    }

    if (action.type === instr.POST_MODEL) {
        return state;
    }

    if (action.type === instr.GET_ASSET) {
        return state;
    }

    if (action.type === instr.POST_ASSET) {

        console.log("modellerReducer: added asset");

        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets, action.payload["asset"]],
                valid: action.payload["valid"]
            },
            suppressCanvasRefresh: false
        };
    }

    if (action.type === instr.GET_RELATION) {
        return state;
    }

    if (action.type === instr.POST_RELATION) {

        //console.log("modellerReducer: added relation");
        let rel = action.payload["relation"];

        return {
            ...state,
            model: {
                ...state.model,
                relations: [...state.model.relations.filter(r => {
                    return !(r.fromID === rel.fromID && r.toID === rel.toID && r.type === rel.type);
                }), action.payload["relation"]],
                valid: action.payload.model["valid"],
            }
        };
    }

    if (action.type === instr.PUT_ASSET_NAME) {

        console.log("modellerReducer: renamed asset");
        let updatedAsset = action.payload;

        let misBehavSets = {};
        _.map(state.model.misbehaviourSets, (ms, uri) => {
            let assetUri = ms["asset"];
            if (assetUri === updatedAsset.uri) {
                ms["assetLabel"] = updatedAsset.label
            }
            misBehavSets[uri] = ms;
        });

        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets.map((asset) => {
                    if (asset["id"] === updatedAsset["id"]) {
                        asset["label"] = updatedAsset["label"];
                    }
                    return asset;
                })],
                misbehaviourSets: misBehavSets
                //valid: action.payload["valid"]
            }
        };
    }

    if (action.type === instr.PUT_ASSET_TYPE) {

        console.log("modellerReducer: changed type of asset");
        let updatedAsset = action.payload["asset"];
        let deletedRelations = action.payload["relations"];

        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets.map((asset) => {
                    if (asset["id"] === updatedAsset["id"]) {
                        asset["type"] = updatedAsset["type"];
                    }
                    return asset;
                })],
                relations: state.model.relations.filter((relation) => {
                    return deletedRelations.indexOf(relation["id"]) < 0;
                }),
                valid: action.payload["valid"]
            }
        };
    }

    if (action.type === instr.MOVING_ASSET) {
        return {
            ...state,
            movedAsset: false
        };
    }
    
    if (action.type === instr.GET_METADATA) {

        // console.log("Additional Properties: Got asset's metadata");
        let updatedAsset = action.payload["asset"];
        let meta = action.payload["metaData"];

        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets.map((asset) => {
                    if (asset["id"] === updatedAsset["id"]) {
                        asset["metaData"] = meta;
                    }
                    return asset;
                })]
                }
            };
    }

    if (action.type === instr.PUT_METADATA) {

        // console.log("Additional Properties: Put asset's metadata", action.payload);
        let updatedAsset = action.payload["asset"];
        let meta = action.payload["metaData"];

        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets.map((asset) => {
                    if (asset["id"] === updatedAsset["id"]) {
                        asset["metaData"] = meta;
                    }
                    return asset;
                })]
            }
        };

    }

    if (action.type === instr.PUT_ASSET_LOC) {

        //console.log("modellerReducer: moved asset", action.payload);
        //let updatedAsset = action.payload["asset"];
        let updatedAsset = action.payload;

        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets.map((asset) => {
                    if (asset["id"] === updatedAsset["id"]) {
                        asset["iconX"] = updatedAsset["iconX"];
                        asset["iconY"] = updatedAsset["iconY"];
                    }
                    return asset;
                })]
                //currently there is a bug in the controller that sets valid to false
                //however there is no real need to change valid state here, as it should not change anyway!
                //valid: action.payload["valid"] //no need to change valid state here, as it should not change
            },
            movedAsset: true,
            grouping: {...state.grouping,
                locationUpdated: state.grouping.inProgress ? true : state.grouping.locationUpdated,
                inProgress: state.grouping.inProgress ? !state.grouping.groupUpdated : state.grouping.inProgress//if group already updated, task is done, so inProgress set to false
            }
        };
    }

    if (action.type === instr.UPDATE_ASSET_LOCS) {
        let updatedAssets = action.payload;
        
        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets.map((asset) => {
                    updatedAssets.map((updatedAsset) => {
                        if (asset.uri === updatedAsset.uri) {
                            asset.iconX = updatedAsset.iconX;
                            asset.iconY = updatedAsset.iconY;
                            return true;
                        }
                    });
                    return asset;
                })]
            },
            //movedAsset: true,
            grouping: {...state.grouping,
                locationUpdated: state.grouping.inProgress ? true : state.grouping.locationUpdated,
                inProgress: state.grouping.inProgress ? !state.grouping.groupUpdated : state.grouping.inProgress//if group already updated, task is done, so inProgress set to false
            }
        };
    }

    if (action.type === instr.PUT_ASSET_CARD) {

        let updatedAsset = action.payload["asset"];
        //console.log("modellerReducer: updated asset cardinality: ", updatedAsset);
        if (updatedAsset["minCardinality"] === undefined || updatedAsset["maxCardinality"] === undefined) {
            alert("ERROR: could not update cardinality");
        }

        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets.map((asset) => {
                    if (asset["id"] === updatedAsset["id"]) {
                        asset["minCardinality"] = updatedAsset["minCardinality"] !== undefined ? updatedAsset["minCardinality"] : asset["minCardinality"];
                        asset["maxCardinality"] = updatedAsset["maxCardinality"] !== undefined ? updatedAsset["maxCardinality"] : asset["maxCardinality"];
                    }
                    return asset;
                })],
                valid: action.payload["valid"]
            }
        };
    }

    if (action.type === instr.PUT_ASSET_POPULATION) {

        let updatedAsset = action.payload["updatedAsset"];

        return {
            ...state,
            model: {
                ...state.model,
                assets: [...state.model.assets.map((asset) => {
                    if (asset["id"] === updatedAsset["id"]) {
                        asset["population"] = updatedAsset["population"];
                    }
                    return asset;
                })],
                valid: false
            }
        };
    }

    if (action.type === instr.DELETE_ASSET) {

        //console.log("modellerReducer: deleted asset: " + action.payload["assets"]);
        // console.log("modellerReducer: deleted asset payload: ", action.payload);

        let deletedAssets = action.payload["assets"];
        var pair = deleteAssetsFromHistory(deletedAssets, state.history, state.historyPointer);
        var pointer = pair.point;
        var hist = pair.hist;
        
        let groupId = action.payload["assetGroup"];
        // console.log("deleted asset from group: groupId = ", groupId);
        
        //flag to indicate if selected asset has been deleted
        //(user may delete an asset that is not currently selected)
        //if so, we will need to clear the selectedAsset
        let sad = (deletedAssets.includes(state.selectedAsset.id));
        // console.log("Selected asset deleted: ", sad);

        return {
            ...state,
            groups: [...state.groups.map((group) => {
                if (group["id"] === groupId) {
                    group.assetIds = group.assetIds.filter((assetId) => {
                        return !action.payload["assets"].includes(assetId);
                    });
                }
                return group;
            })],
            model: {
                ...state.model,
                assets: state.model.assets.filter((asset) => {
                    return action.payload["assets"].indexOf(asset["id"]) < 0;
                }),
                relations: state.model.relations.filter((relation) => {
                    return action.payload["relations"].indexOf(relation["id"]) < 0;
                }),
                valid: action.payload["valid"],
            },
            selectedAsset: {
                ...state.selectedAsset,
                id: sad ? "" : state.selectedAsset.id,
                controlSets: sad ? [] : state.selectedAsset.controlSets,
                threats: sad ? [] : state.selectedAsset.threats,
            },
            historyPointer: pointer,
            history: hist,
            backEnabled: (pointer>0 ? true : false),
            forwardEnabled: (state.history[pointer+1] != null ? true : false)
        };
    }

    if (action.type === instr.CHANGE_SELECTED_ASSET) {
        //N.B. payload may contain "id" or "uri"
        let selectedAsset;

        // only change asset if it exists
        if (action.payload["uri"] && action.payload["uri"] !== "") {
            selectedAsset = _.find(state.model.assets, ["uri", action.payload["uri"]]);
            if (!selectedAsset) {
                console.warn("Cannot locate asset: ", action.payload["uri"]);
                return state;
            }
        }
        else if (action.payload["id"] && action.payload["id"] !== "") {
            selectedAsset = _.find(state.model.assets, ["id", action.payload["id"]]);
            if (!selectedAsset) {
                console.warn("Cannot locate asset: ", action.payload["id"]);
                return state;
            }
        }

        let selectedAssetId = selectedAsset ? selectedAsset["id"] : "";

        var pointer = state.historyPointer + 1;

        if (pointer>19) {
            pointer = 19;
        }

        var hist = updateAssetHistory(selectedAssetId, state.history, pointer);

        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                id: selectedAssetId,
                controlSets: [],
                threats: [],
                //isThreatEditorVisible: false //keep ThreatEditor open now
            },
            historyPointer: pointer,
            history: hist,
            backEnabled: (pointer>0 ? true : false),
            forwardEnabled: (state.history[pointer+1] != null ? true : false)
        };
    }

    if (action.type === instr.CHANGE_SELECTED_INFERRED_ASSET) {
        var pointer = state.historyPointer + 1;
        if(pointer>modelState.historySize-1){
          pointer = modelState.historySize-1;
        }
        var hist = updateAssetHistory(action.payload["id"], state.history, pointer);

        console.log(newPointer);
        console.log(assetID);
        console.log(state.history);

        return {
            ...state,
            selectedInferredAsset: action.payload["id"],
            historyPointer: pointer,
            history: hist,
            backEnabled: (pointer>0 ? true : false),
            forwardEnabled: (state.history[pointer+1] != null ? true : false)
        };
    }

    if (action.type === instr.ASSET_HISTORY_FORWARD) {

        var assetID;
        var newPointer;
        if(state.historyPointer<modelState.historySize-1){
            newPointer = state.historyPointer+1;
        } else {
            newPointer = state.historyPointer;
        }
        assetID = state.history[newPointer];

        return {
          ...state,
          selectedAsset: {
              ...state.selectedAsset,
              id: assetID,
              controlSets: [],
              threats: [],
          },
          historyPointer: newPointer,
          history: state.history,
          backEnabled: (newPointer>0 ? true : false),
          forwardEnabled: (state.history[newPointer+1] != null ? true : false)
        };
    }

    if (action.type === instr.ASSET_HISTORY_BACK) {

        var assetID;
        var newPointer;
        //console.log(state.historyPointer);
        if(state.historyPointer>0){
            newPointer = state.historyPointer-1;
            //console.log(newPointer);
        } else {
            newPointer = state.historyPointer;
            //console.log(newPointer);
        }
        assetID = state.history[newPointer];

        return {
          ...state,
          selectedAsset: {
              ...state.selectedAsset,
              id: assetID,
              controlSets: [],
              threats: [],
              //isThreatEditorVisible: false //keep ThreatEditor open now
          },
          historyPointer: newPointer,
          history: state.history,
          backEnabled: (newPointer==0 ? false : true),
          forwardEnabled: (state.history[newPointer+1] != null ? true : false)
        };
    }


    if (action.type === instr.IS_VALIDATING) {

        console.log("modellerReducer: model is validating");

        return {
            ...state,
            model: {
                ...state.model,
                //validationStatus: "started", //KEM - seems to be unused
                validating: true
            },
            validationProgress: {
                status: "starting",
                progress: 0.0,
                message: "Validation starting",
                error: "",
                waitingForUpdate: false
            },
        };
    }

    if (action.type === instr.IS_NOT_VALIDATING) {

        console.log("modellerReducer: model is not validating");

        return {
            ...state,
            model: {
                ...state.model,
                validating: false
            },
            validationProgress: {
                status: "inactive",
                progress: 0.0,
                message: "Validating...",
                error: "",
                waitingForUpdate: false
            },
        };
    }

    if (action.type === instr.VALIDATION_FAILED) {

        console.log("modellerReducer: validation failed");

        return {
            ...state,
            model: {
                ...state.model,
                validating: false
            },
        };
    }

    if (action.type === instr.RISK_CALC_FAILED) {

        console.log("modellerReducer: risk calc failed");

        return {
            ...state,
            model: {
                ...state.model,
                calculatingRisks: false
            },
        };
    }

    if (action.type === instr.IS_CALCULATING_RISKS) {

        console.log("modellerReducer: calculating risks for model");

        return {
            ...state,
            model: {
                ...state.model,
                calculatingRisks: true
            },
            validationProgress: { //TODO: change to riskCalcProgress?
                status: "starting",
                progress: 0.0,
                message: "Risk calculation starting",
                error: "",
                waitingForUpdate: false
            },
        };
    }

    if (action.type === instr.IS_NOT_CALCULATING_RISKS) {

        console.log("modellerReducer: not calculating risks for model");

        return {
            ...state,
            model: {
                ...state.model,
                calculatingRisks: false
            },
            validationProgress: { //TODO: change to riskCalcProgress?
                status: "inactive",
                progress: 0.0,
                message: "",
                error: "",
                waitingForUpdate: false
            },
        };
    }

    if (action.type === instr.RISK_CALC_RESULTS) {
        let results = action.payload["results"];
        let saved = action.payload["saved"];
        console.log("Received risk calc results: ", results);
        console.log("Saved = ", saved);

        let prefix = "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/";

        let likelihoodLevelsArray = state.model.levels["Likelihood"];
        let riskLevelsArray = state.model.levels["RiskLevel"];
        let twLevelsArray = state.model.levels["TrustworthinessLevel"];

        var likelihoodLevels = likelihoodLevelsArray.reduce(function(map, obj) {
            map[obj.uri] = obj;
            return map;
        }, {});

        var riskLevels = riskLevelsArray.reduce(function(map, obj) {
            map[obj.uri] = obj;
            return map;
        }, {});

        var twLevels = twLevelsArray.reduce(function(map, obj) {
            map[obj.uri] = obj;
            return map;
        }, {});

        let csMap = results["cs"]; //updated control sets map from risk calc

        //loop through all control sets and update coverageLevel, which may have been corrected by the risk calc (consistency checks)
        let updatedControlSets = [...state.model.controlSets.map((cs) => {
            let csKey = cs["uri"].replace(prefix, "");
            if (csKey in csMap) {
                let rcControlSet = csMap[csKey];
                let coverageLevel = prefix + rcControlSet.coverageLevel;
    
                return {
                    ...cs,
                    coverageLevel: coverageLevel
                };
            }
            else {
                console.warn("could not locate control set: " + csKey + " in risk results");
                return cs;
            }
        })];

        //need to build lists of indirectCauses for each MS, as these are not currently available in the returned risk calc MS objects
        console.log("Building indirect causes lists for misbehaviours..");
        let msUris = Object.keys(results["misbehaviourSets"]).map((muri) => {return prefix + muri});

        let msIndirectCauses = {};

        msUris.map((muri) => {
            msIndirectCauses[muri] = new Set(); //initialise set
        });

        let threatsMap = results["threats"];

        Object.values(threatsMap).map((rcThreat) => {
            let threatUri = prefix + rcThreat.uri;
            let indirectMisbehaviours = rcThreat.indirectMisbehaviours.map((muri) => {return prefix + muri});
            indirectMisbehaviours.map((muri) => {
                let indCausesSet = msIndirectCauses[muri];
                indCausesSet.add(threatUri); //add threat to the ms causes set
            });
        });

        //console.log("msIndirectCauses (complete):", msIndirectCauses);
        //end of build lists of indirectCauses

        let updatedMisbehaviourSets = {...state.model.misbehaviourSets};
        let misbehaviourSets = Object.values(results["misbehaviourSets"]);

        misbehaviourSets.map((ms) => {
            let msuri = prefix + ms.uri;
            let likelihoodUri = prefix + ms.prior;
            let riskUri = prefix + ms.risk;

            let likelihood = likelihoodLevels[likelihoodUri];
            let riskLevel = riskLevels[riskUri];

            let updatedMs = updatedMisbehaviourSets[msuri];

            if (updatedMs) {
                updatedMs.likelihood = likelihood;
                updatedMs.riskLevel = riskLevel;
                updatedMs.indirectCauses = Array.from(msIndirectCauses[msuri]); //get indirect causes from msIndirectCauses map
                updatedMs.directEffects = ms.causedThreats.map((muri) => {return prefix + muri});
            }
            else {
                console.warn("Could not locate MS: " + msuri);
            }
        });

        let updatedTwass = {...state.model.twas};
        let twass = Object.values(results["twas"]);

        //update TWAS based on risk results
        //N.B. risk calc may adjust asserted (assumed) values as well as inferred (calculated)
        twass.map((twas) => {
            let twasuri = prefix + twas.uri;

            let assertedTWLevelUri = prefix + twas.assertedLevel; //assumed
            let inferredTWLevelUri = prefix + twas.inferredLevel; //calculated

            let updatedTwas = updatedTwass[twasuri];

            if (updatedTwas) {
                updatedTwas.assertedTWLevel = twLevels[assertedTWLevelUri];
                updatedTwas.inferredTWLevel = twLevels[inferredTWLevelUri];
            }
            else {
                console.warn("Could not locate TWAS: " + twasuri);
            }
        });

        let model = results["model"];
        let modelRiskUri = prefix + model.risk;
        let modelRisk = riskLevels[modelRiskUri];

        return {
            ...state,
            model: {
                ...state.model,
                riskLevelsValid: true,
                risk: modelRisk,
                saved: saved,
                controlSets: updatedControlSets,
                misbehaviourSets: updatedMisbehaviourSets,
                threats: [
                    ...state.model.threats.map((threat) => {
                        let threatKey = threat["uri"].replace(prefix, "");
                        if (threatKey in threatsMap) {
                            //console.log("updating threat: " + threat["uri"]);
                            let rcThreat = threatsMap[threatKey];

                            let likelihoodUri = prefix + rcThreat.prior;
                            let riskUri = prefix + rcThreat.risk;
                
                            let likelihood = likelihoodLevels[likelihoodUri];
                            let riskLevel = riskLevels[riskUri];

                            let indirectEffects = rcThreat.indirectMisbehaviours.map((muri) => {return prefix + muri});
                            let rootCause = rcThreat.rootCause != null ? rcThreat.rootCause : false;
                
                            return {
                                ...threat,
                                likelihood: likelihood,
                                riskLevel: riskLevel,
                                indirectEffects: indirectEffects,
                                rootCause: rootCause
                            };
                        }
                        else {
                            console.warn("could not locate threat: " + threatKey + " in risk results");
                            return threat;
                        }
                    })],
                threatsUpdated: true
            }
            //Not sure if the following is required, as the selectedMisbehaviour seems to update anyway
            //selectedMisbehaviour: {
            //    ...state.selectedMisbehaviour,
            //    misbehaviour: updatedSelectedMisbehaviour
            //}
        };

    }

    if (action.type === instr.IS_DROPPING_INFERRED_GRAPH) {

        console.log("modellerReducer: dropping inferred graph for model");

        return {
            ...state,
            isDroppingInferredGraph: true
        };
    }

    if (action.type === instr.IS_NOT_DROPPING_INFERRED_GRAPH) {

        console.log("modellerReducer: dropped inferred graph for model");

        return {
            ...state,
            isDroppingInferredGraph: false
        };
    }

    /* KEM - seems to be unused
    if (action.type === instr.RESET_VALIDATION_STATUS) {

        console.log("modellerReducer: reset validation status");

        return {
            ...state,
            model: {
                ...state.model,
                validationStatus: "",
                validating: false
            }
        };
    }
    */

    if (action.type === instr.DELETE_RELATION) {
        if (action.payload["started"]) {
            let relationId = action.payload["relationId"];
            console.log("modellerReducer: deleting relation " + relationId);
            return {
                ...state,
                model: {
                    ...state.model,
                    relations: [...state.model.relations.map((relation) => {
                        if (relation["id"] === relationId) {
                            console.log("marking relation as deleting: " + relation["id"]);
                            return {...relation,
                                deleting: true //set deleting flag (used by spinner)
                            }
                        }
                        else {
                            //console.log("(ignoring relation " + relation["id"] + ")");
                            return relation;
                        };
                    })]
                }
            };
        }
        else {
            console.log("modellerReducer: deleted relation(s) " + action.payload["relations"]); //may be more than one
            return {
                ...state,
                model: {
                    ...state.model,
                    relations: state.model.relations.filter((relation) => {
                        return action.payload["relations"].indexOf(relation["id"]) < 0;
                    }),
                    valid: action.payload["valid"]
                }
            };
        }
    }

    if (action.type === instr.PATCH_RELATION) {
        let relationId = action.payload["relationId"];
        let updatedRelation = action.payload["relation"];
        let newRelationId = updatedRelation["id"];

        //console.log("modellerReducer: patched relation " + action.payload["id"]);
        console.log(`modellerReducer: patched relation ${relationId} (new relation id: ${newRelationId})`);

        return {
            ...state,
            model: {
                ...state.model,
                relations: [...state.model.relations.map((relation) => {
                    if (relation["id"] === relationId) {
                        return updatedRelation;
                    }
                    return relation;
                })],
                valid: false
            }
        };
    }

    if (action.type === instr.HIDE_RELATION) {
        let relationId = action.payload["relationId"];

        return {
            ...state,
            model: {
                ...state.model,
                relations: [...state.model.relations.map((relation) => {
                    if (relation["id"] === relationId) {
                        relation.hidden = !relation.hidden;
                    }
                    return relation;
                })]
            }
        }
    }

    if (action.type === instr.PATCH_UPDATED_CONTROL) {
        return state;
    }

    if (action.type === instr.GET_CONTROLS) {
        return state;
    }

    if (action.type === instr.PATCH_UPDATED_THREAT) {
        return state;
    }

    if (action.type === instr.GET_UPDATED_THREATS) {
        return state;
    }

    if (action.type === instr.ACCEPT_THREAT) {
        return {
            ...state,
            model: {
                ...state.model,
                threats: [
                    ...state.model.threats.map((threat) => {
                        if (threat["uri"] === action.payload.threat["uri"]) {
                            //return action.payload.threat;
                            return {
                                ...threat,
                                acceptanceJustification: action.payload.threat.acceptanceJustification,
                                resolved: action.payload.threat.resolved
                            };
                        }
                        return threat;
                    })],
                threatsUpdated: true
            },
            selectedAsset: {
                ...state.selectedAsset,
                threats: [
                    ...state.selectedAsset.threats.map((threat) => {
                        if (threat["uri"] === action.payload.threat["uri"]) {
                            return {
                                ...threat,
                                acceptanceJustification: action.payload.threat.acceptanceJustification,
                                resolved: action.payload.threat.resolved
                            };
                        }
                        return threat;
                    })]
            }
        };
    }

    if (action.type === instr.GET_COMPILED_ASSET_DETAILS) {
        let newThreats = action.payload["threats"];
        //printThreats(newThreats);
        if (action.payload["controlSets"] === undefined) {
            return {
                ...state,
                selectedAsset: {
                    ...state.selectedAsset,
                    threats: action.payload["threats"],
                    misbehaviours: getMisbehaviours(newThreats),
                    loadingControlsAndThreats: action.payload.loadingControlsAndThreats
                },
                model: {
                    ...state.model,
                    threats: state.model.threats.map((threat) => {
                        if (newThreats.find((a) => a.uri === threat.uri) !== undefined) {
                            return newThreats.find((a) => a.uri === threat.uri);
                        }
                        return threat;
                    })
                }
            };
        } else {
            return {
                ...state,
                model: {
                    ...state.model,
                    controlSets: action.payload["allcontrols"],
                    threats: state.model.threats.map((threat) => {
                        if (newThreats.find((a) => a.uri === threat.uri) !== undefined) {
                            return newThreats.find((a) => a.uri === threat.uri);
                        }
                        return threat;
                    })
                },
                selectedAsset: {
                    ...state.selectedAsset,
                    controlSets: action.payload["controlSets"],
                    threats: action.payload["threats"],
                    misbehaviours: getMisbehaviours(newThreats),
                    loadingControlsAndThreats: action.payload.loadingControlsAndThreats
                }
            };
        }
    }

    if (action.type === instr.UPDATE_CONTROLS) {
        let controlsReset = action.payload.controlsReset ? action.payload.controlsReset : false;
        let controlsUpdate = action.payload.controlsUpdate;
        
        let threats = [...state.model.threats];
        let controlStrategies = {...state.model.controlStrategies};
        let complianceSets = [...state.model.complianceSets];

        let controlUris = controlsUpdate.controls;
        let controlSets = state.model.controlSets.map((controlSet) => {
            if (controlUris.includes(controlSet.uri)) {
                controlSet.proposed = controlsUpdate.proposed;
                controlSet.workInProgress = controlsUpdate.workInProgress;
                if (controlsUpdate.coverageLevel) controlSet.coverageLevel = controlsUpdate.coverageLevel;
                if (controlsUpdate.coverageAsserted !== undefined) controlSet.coverageAsserted = controlsUpdate.coverageAsserted;
                return controlSet;
            } else {
                return controlSet;
            }
        });

        updateControlStrategies(threats, controlStrategies, controlSets); //update CSGs with updated controls
        updateComplianceSets(complianceSets, state.model.complianceThreats, controlStrategies, controlSets); //also update compliance threats with updated controls

        return {
            ...state,
            model: {
                ...state.model,
                riskLevelsValid: false,
                controlSets: controlSets,
                threats: threats,
                complianceSets: complianceSets,
                threatsUpdated: true,
                controlsReset: controlsReset
            },
            selectedAsset: {
                ...state.selectedAsset,
            }
        };
    }

    if (action.type === instr.UPDATE_TWAS) {
        let updatedTwas = action.payload["updatedTwas"];
        let twasUri = updatedTwas["uri"];

        let twas = {...state.model.twas};
        twas[twasUri] = updatedTwas;

        return {
            ...state,
            model: {
                ...state.model,
                twas: twas,
                riskLevelsValid: false
            }
        };
    }

    if (action.type === instr.UPDATE_MISBEHAVIOUR_IMPACT) {
        let misbehaviour = action.payload["misbehaviour"];
        let misbehaviourUri = misbehaviour.uri;

        let impactLevelsArray = state.model.levels["ImpactLevel"];
        let impactLevel = impactLevelsArray.find((level) => level["uri"] === misbehaviour.impactLevel.uri);
        let updatedImpact = impactLevel;
        let impactLevelAsserted = misbehaviour["impactLevelAsserted"];

        let updatedSelectedMisbehaviour = state.selectedMisbehaviour.misbehaviour;
        if (updatedSelectedMisbehaviour["uri"] === misbehaviourUri) {
            updatedSelectedMisbehaviour["impactLevel"] = updatedImpact;
            updatedSelectedMisbehaviour["impactLevelAsserted"] = impactLevelAsserted;
        }

        let updatedMisbehaviourSets = {...state.model.misbehaviourSets};
        updatedMisbehaviourSets[misbehaviourUri]["impactLevel"] = updatedImpact;
        updatedMisbehaviourSets[misbehaviourUri]["impactLevelAsserted"] = impactLevelAsserted;

        return {
            ...state,
            model: {
                ...state.model,
                riskLevelsValid: false,
                misbehaviourSets: updatedMisbehaviourSets
            },
            selectedMisbehaviour: {
                ...state.selectedMisbehaviour,
                misbehaviour: updatedSelectedMisbehaviour
            }
        };
    }

    if (action.type === instr.GET_SECONDARY_EFFECTS) {
        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                //secondaryEffects: getSecondaryEffectsMockData() //KEM: can be used to mock the data, if useful
                secondaryEffects: action.payload
            }
        };
    }

    /*
    if (action.type === instr.GET_CAUSE_EFFECT) {
        if (action.payload.secondaryEffects !== undefined) {
            return {
                ...state,
                model: {
                    ...state.model,
                    threatsUpdated: false
                },
                selectedAsset: { //TODO: change to selectedThreat
                    ...state.selectedAsset,
                    cause: action.payload.cause, //TODO: this needs to be moved to the threat
                    effect: action.payload.effect, //TODO: this needs to be moved to the threat
                    secondaryEffects: action.payload.secondaryEffects, //TODO: ditto
                    loadingCausesAndEffects: action.payload.loadingCausesAndEffects,
                    //updateRootCausesModel: action.payload.updateRootCausesModel
                }
            };
        }
        else {
            return {
                ...state,
                model: {
                    ...state.model,
                    threatsUpdated: false
                },
                selectedAsset: { //TODO: change to selectedThreat
                    ...state.selectedAsset,
                    loadingCausesAndEffects: action.payload.loadingCausesAndEffects,
                    //updateRootCausesModel: action.payload.updateRootCausesModel
                }
            };
        }
    }
    */

    //TODO: change to EXPLORE_MISBEHAVIOUR?
    if (action.type === instr.GET_ROOT_CAUSES) {
        let misbehaviour = action.payload.misbehaviour;

        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                isThreatEditorActive: false
            },
            selectedMisbehaviour: {
                ...state.selectedMisbehaviour,
                misbehaviour: misbehaviour
            },
            isMisbehaviourExplorerVisible: true,
            isMisbehaviourExplorerActive: true
        };
    }

    if (action.type === instr.CHANGE_SELECTED_TWAS) {
        let twas = action.payload.twas;
        let misbehaviour = state.model.misbehaviourSets[twas.causingMisbehaviourSet];

        if (!misbehaviour)
            return state;

        //Select the TWAS by selecting its corresponding misbehaviour
        //Also expand the TWAS panel in the asset details
        return {
            ...state,
            selectedMisbehaviour: {
                ...state.selectedMisbehaviour,
                misbehaviour: misbehaviour
            },
            expanded: {
                ...state.expanded,
                assetDetails: {
                    ...state.expanded.assetDetails,
                    twas: true
                }
            }
        };
    }

    if (action.type === instr.TOGGLE_PANEL) {
        let panel = action.payload.panel;
        let expanded = action.payload.expanded;

        //TODO (if/when required): add support for toggling other collapsible panels
        if (panel !== "twas") {
            console.log("Panel expand not supported for panel:", panel);
            return state;
        }

        return {
            ...state,
            expanded: {
                ...state.expanded,
                assetDetails: {
                    ...state.expanded.assetDetails,
                    twas: expanded
                }
            }
        };
    }

    if (action.type === instr.CLOSE_MISBEHAVIOUR_EXPLORER) {
        return {
            ...state,
            selectedMisbehaviour: {
                misbehaviour: {},
                //directCause: [],
                //rootCause: [],
                loadingRootCauses: false
            },
            isMisbehaviourExplorerVisible: false,
            isMisbehaviourExplorerActive: false,
        };
    }

    if (action.type === instr.OPEN_COMPLIANCE_EXPLORER) {
        return {
            ...state,
            isComplianceExplorerVisible: true,
            isComplianceExplorerActive: true,
        };
    }


    if (action.type === instr.CLOSE_COMPLIANCE_EXPLORER) {
        return {
            ...state,
            isComplianceExplorerVisible: false,
            isComplianceExplorerActive: false,
        };
    }

    if (action.type === instr.OPEN_CONTROL_EXPLORER) {
        //console.log(action.payload["selectedControl"]);
        //console.log(state);
        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                selectedControl: action.payload["selectedControl"],
            },
            isControlExplorerVisible: true,
            isControlExplorerActive: true,
        };
    }


    if (action.type === instr.CLOSE_CONTROL_EXPLORER) {
        return {
            ...state,
            isControlExplorerVisible: false,
            isControlExplorerActive: false,
        };
    }

    if (action.type === instr.OPEN_CONTROL_STRATEGY_EXPLORER) {
        let csgs = [...action.payload["selectedControlStrategy"]];
        let context = action.payload["context"];

        return {
            ...state,            
            selectedControlStrategy: csgs,
            csgExplorerContext: context,
            isControlStrategyExplorerVisible: true,
            isControlStrategyExplorerActive: true,
        };
    }


    if (action.type === instr.CLOSE_CONTROL_STRATEGY_EXPLORER) {
        return {
            ...state,
            isControlStrategyExplorerVisible: false,
            isControlStrategyExplorerActive: false,
        };
    }

    if (action.type === instr.OPEN_REPORT_DIALOG) {
        console.log("OPEN_REPORT_DIALOG");
        return {
            ...state,
            reportType: action.payload["reportType"],
            isReportDialogVisible: true,
            isReportDialogActive: true,
        };
    }

    if (action.type === instr.CLOSE_REPORT_DIALOG) {
        console.log("CLOSE_REPORT_DIALOG");
        return {
            ...state,
            isReportDialogVisible: false,
            isReportDialogActive: false,
        };
    }

    if (action.type === instr.TOGGLE_THREAT_EDITOR) {
        let isThreatEditorVisible = action.payload["toggle"];
        //console.log(state.selectedAsset);
        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                isThreatEditorVisible: isThreatEditorVisible,
                isThreatEditorActive: isThreatEditorVisible,
            },
            selectedThreat: {id: action.payload["threatId"]},
            isMisbehaviourExplorerActive: !isThreatEditorVisible,
            isComplianceExplorerActive: !isThreatEditorVisible,
            isControlExplorerActive: !isThreatEditorVisible,
            suppressCanvasRefresh: action.payload["toggle"]
        };
    }

    if (action.type === instr.ACTIVATE_THREAT_EDITOR) {
        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                isThreatEditorActive: true
            },
            isMisbehaviourExplorerActive: false,
            isComplianceExplorerActive: false,
            isControlExplorerActive: false,
        };
    }

    if (action.type === instr.ACTIVATE_MISBEHAVIOUR_EXPLORER) {
        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                isThreatEditorActive: false
            },
            isMisbehaviourExplorerActive: true,
            isComplianceExplorerActive: false,
            isControlExplorerActive: false,
            threatFiltersActive: {
                "asset-threats": false,
                "direct-causes": false,
                "root-causes": false,
                "compliance-threats": false
            },
        };
    }

    if (action.type === instr.ACTIVATE_COMPLIANCE_EXPLORER) {
        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                isThreatEditorActive: false
            },
            isMisbehaviourExplorerActive: false,
            isComplianceExplorerActive: true,
            isControlExplorerActive: false,
            threatFiltersActive: {
                "asset-threats": false,
                "direct-causes": false,
                "root-causes": false,
                "compliance-threats": false
            },
        };
    }

    if (action.type === instr.ACTIVATE_CONTROL_EXPLORER) {
        return {
            ...state,
            selectedAsset: {
                ...state.selectedAsset,
                isThreatEditorActive: false
            },
            isMisbehaviourExplorerActive: false,
            isComplianceExplorerActive: false,
            isControlExplorerActive: true,
            threatFiltersActive: {
                "asset-threats": false,
                "direct-causes": false,
                "root-causes": false,
                "compliance-threats": false
            },
        };
    }

    if (action.type === instr.ACTIVATE_THREAT_FILTER) {
        let id = action.payload["id"];
        let value = action.payload["active"];
        //console.log("ACTIVATE_THREAT_FILTER: " + id + ": " + value);

        let assetThreatsActive = (id === "threats-filter-asset-threats") ? value : state.threatFiltersActive["asset-threats"];
        let directCausesActive = (id === "threats-filter-direct-causes") ? value : state.threatFiltersActive["direct-causes"];
        let rootCausesActive = (id === "threats-filter-root-causes") ? value : state.threatFiltersActive["root-causes"];
        let complianceThreatsActive = false;

        if (id.includes("compliance-threats")) {
            if (value !== false) {
                complianceThreatsActive = id;
            }
        }

        return {
            ...state,
            threatFiltersActive: {
                ...state.threatFiltersActive,
                "asset-threats": assetThreatsActive,
                "direct-causes": directCausesActive,
                "root-causes": rootCausesActive,
                "compliance-threats": complianceThreatsActive
            },
        };
    }

    if (action.type === instr.ACTIVATE_ACCEPTANCE_PANEL) {
        let value = action.payload["active"];
        //console.log("ACTIVATE_ACCEPTANCE_PANEL: " + value);
        return {
            ...state,
            isAcceptancePanelActive: value
        };
    }

    if (action.type === instr.SUPPRESS_CANVAS_REFRESH) {
        return {
            ...state,
            suppressCanvasRefresh: action.payload["value"]
        };
    }

    if (action.type === instr.REDRAW_RELATIONS) {
        console.log("REDRAW_RELATIONS");
        return {
            ...state,
            redrawRelations: (state.redrawRelations + 1)
        };
    }

    if (action.type === instr.UPLOAD_SCREENSHOT) {
        return state;
    }

    if (action.type === instr.UPDATE_LAYER_SELECTION) {
        return {
            ...state,
            selectedLayers: action.payload,
            suppressCanvasRefresh: false
        };
    }

    if (action.type === instr.UPDATE_THREAT_LOADING) {
        return {
            ...state,
            loading: {
                ...state.loading,
                threats: action.payload
            }
        };
    }

    if (action.type === instr.UPDATE_DETAILS_LOADING) {
        //alert("Loading details: " + action.payload);
        return {
            ...state,
            loading: {
                ...state.loading,
                details: action.payload
            }
        };
    }

    if (action.type === instr.UPDATE_MODEL_LOADING) {
        return {
            ...state,
            loading: {
                ...state.loading,
                model: action.payload
            },
            loadingProgress: action.payload ? {
                progress: 0.0,
                message: "Loading model",
                waitingForUpdate: false
            } : {...state.loadingProgress}
        };
    }

    if (action.type === instr.UPDATE_CARD_LOADING) {
        return {
            ...state,
            loading: {
                ...state.loading,
                cardinality: action.payload
            }
        };
    }

    if (action.type === instr.UPDATE_ASSET_LOADING) {
        return {
            ...state,
            loading: {
                ...state.loading,
                asset: action.payload
            }
        };
    }

    if (action.type === instr.SET_ZOOM) {
        let transformOrigin = action.payload.transformOrigin !== null ? action.payload.transformOrigin : state.canvas.transformOrigin;

        return {
            ...state,
            canvas: {
                ...state.canvas,
                zoom: action.payload.zoom
            }
        };
    }

    if (action.type === instr.SET_TRANSFORM_ORIGIN) {
        return {
            ...state,
            canvas: {
                ...state.canvas,
                transformOrigin: action.payload.transformOrigin
            }
        };
    }

    // TODO: this action does not exist: what did it do?!
    // if(action.type === instr.SET_SCROLL_LEFT) {
    //     return {
    //         ...state,
    //         canvas: {
    //             ...state.canvas,
    //             scrollLeft: action.payload.scrollLeft
    //         }
    //     };
    // }

    // TODO: this action does not exist: what did it do?!
    // if(action.type === instr.SET_SCROLL_TOP) {
    //     return {
    //         ...state,
    //         canvas: {
    //             ...state.canvas,
    //             scrollTop: action.payload.scrollTop
    //         }
    //     };
    // }

    if (action.type === instr.SHOW_INFERRED_RELATIONS) {
        return {
            ...state,
            view: {
                ...state.view,
                showInferredRelations: action.payload.showInferredRelations
            }
        };
    }

    if (action.type === instr.SHOW_HIDDEN_RELATIONS) {
        return {
            ...state,
            view: {
                ...state.view,
                showHiddenRelations: action.payload.showHiddenRelations
            }
        };
    }

    if (action.type === instr.RECENTRE_CANVAS) {
        return {
            ...state,
            view: {
                ...state.view,
                reCentreCanvas: action.payload.newState
            }
        }
    }

    if (action.type === instr.RECENTRE_MODEL) {
        return {
            ...state,
            view: {
                ...state.view,
                reCentreModel: action.payload.newState
            }
        }
    }

    if (action.type === instr.SIDE_PANEL_WIDTHS) {
        let widthsObj = action.payload;
        if (!widthsObj.hasOwnProperty("left")) widthsObj.left = state.view.leftSidePanelWidth;
        else if (!widthsObj.hasOwnProperty("right")) widthsObj.right = state.view.rightSidePanelWidth;
        return {
            ...state,
            view: {
                ...state.view,
                leftSidePanelWidth: widthsObj.left,
                rightSidePanelWidth: widthsObj.right
            }
        }
    }

    if (action.type === instr.SIDE_PANEL_ACTIVATED) {
        //console.log("SIDE_PANEL_ACTIVATED");
        return {
            ...state,
            sidePanelActivated: true
        }
    }
    
    if (action.type === instr.SIDE_PANEL_DEACTIVATED) {
        //console.log("SIDE_PANEL_DEACTIVATED");
        return {
            ...state,
            sidePanelActivated: false
        }
    }

    if (action.type === instr.POST_GROUP) {
        console.log("modellerReducer: added group");

        return {
            ...state,
            groups: [...state.groups, action.payload ],
            suppressCanvasRefresh: false
        };
    }

    if (action.type === instr.DELETE_GROUP) {
        let groupId = action.payload["groupId"];
        let deleteAssets = action.payload["deleteAssets"];
        let response = action.payload["response"];
        
        console.log("modellerReducer: deleted group: " + groupId);
        console.log("modellerReducer: deleteAssets: " + deleteAssets);
        console.log("modellerReducer: delete group response: ", response);
        
        let deletedAssets = response.assets;
        let deletedRelations = response.relations;
        let valid = response.valid;
        
        var pair = deleteAssetsFromHistory(deletedAssets, state.history, state.historyPointer);
        var pointer = pair.point;
        var hist = pair.hist;
       
        //flag to indicate if selected asset has been deleted
        //(user may delete an asset that is not currently selected)
        //if so, we will need to clear the selectedAsset
        let sad = (deletedAssets.includes(state.selectedAsset.id));
        console.log("Selected asset deleted: ", sad);
        
        let groupUpdated = state.grouping.inProgress ? true : state.grouping.groupUpdated;
        let locationUpdated = state.grouping.inProgress ? state.grouping.locationUpdated : false;
        let inProgress = state.grouping.inProgress ? !(groupUpdated && locationUpdated) : false;

        return {
            ...state,
            groups: state.groups.filter((group) => {
                return group["id"] !== groupId;
            }),
            suppressCanvasRefresh: false,
            model: {
                ...state.model,
                assets: state.model.assets.filter((asset) => {
                    return deletedAssets.indexOf(asset["id"]) < 0;
                }),
                relations: state.model.relations.filter((relation) => {
                    return deletedRelations.indexOf(relation["id"]) < 0;
                }),
                valid: valid,
            },
            grouping: {...state.grouping,
                groupUpdated: groupUpdated,
                locationUpdated: locationUpdated,
                inProgress: inProgress
            },
            selectedAsset: {
                ...state.selectedAsset,
                id: sad ? "" : state.selectedAsset.id,
                controlSets: sad ? [] : state.selectedAsset.controlSets,
                threats: sad ? [] : state.selectedAsset.threats,
            },
            historyPointer: pointer,
            history: hist,
            backEnabled: (pointer>0 ? true : false),
            forwardEnabled: (state.history[pointer+1] != null ? true : false)
        };
    }

    if (action.type === instr.GROUPING) {
        console.log("modellerReducer: grouping event: ", action.payload);

        return {
            ...state,
            grouping: {...state.grouping,
                group: action.payload.group ? action.payload.group : state.grouping.group,
                asset: action.payload.asset ? action.payload.asset : state.grouping.asset,
                groupUpdated: action.payload.groupUpdated !== undefined ? action.payload.groupUpdated : state.grouping.groupUpdated,
                locationUpdated: action.payload.locationUpdated !== undefined ? action.payload.locationUpdated : state.grouping.locationUpdated,
                //assetRelocated: action.payload.assetRelocated ? action.payload.assetRelocated : state.grouping.assetRelocated,
                inProgress: action.payload.inProgress !== undefined ? action.payload.inProgress : state.grouping.inProgress
            }
        };
    }
    
    if (action.type === instr.PRINT_MODEL_STATE) {
        console.log(state.model);
        return state;
    }

    if (action.type === instr.PUT_GROUP_ADD_ASSET) {
        console.log("modellerReducer: added asset to group", action.payload);
        let updatedGroup = action.payload.group;
        let updatedAsset = action.payload.asset;
        return {
            ...state,
                model: {
                    ...state.model,
                    assets: [...state.model.assets.map((asset) => {
                        if (asset["id"] === updatedAsset["id"]) {
                            asset["iconX"] = updatedAsset["iconX"];
                            asset["iconY"] = updatedAsset["iconY"];
                        }
                        return asset;
                    })]
                },
                groups: [...state.groups.map((group) => {
                    if (group["id"] === updatedGroup["id"]) {
                        group.assetIds = [ ...updatedGroup.assetIds ];
                    }
                    return group;
                })],
            grouping: {...state.grouping,
                groupUpdated: true,
                locationUpdated: true,
                //inProgress: !state.grouping.locationUpdated //if location already updated, task is done, so inProgress set to false
                inProgress: false //now we set the relocated asset position at the same time as adding to group
            }
        }
    };
    
    if (action.type === instr.PUT_GROUP_REMOVE_ASSET) {
        console.log("modellerReducer: removed asset from group", action.payload);
        let updatedGroup = action.payload.group;
        let updatedAsset = action.payload.asset;
        
        return {
            ...state,
                model: {
                    ...state.model,
                    assets: [...state.model.assets.map((asset) => {
                        if (asset["id"] === updatedAsset["id"]) {
                            asset["iconX"] = updatedAsset["iconX"];
                            asset["iconY"] = updatedAsset["iconY"];
                            asset["grouped"] = false;
                        }
                        return asset;
                    })]
                },
                groups: [...state.groups.map((group) => {
                    if (group["id"] === updatedGroup["id"]) {
                        group.assetIds = [ ...updatedGroup.assetIds ];
                    }
                    return group;
                })],
        }
    };
    
    if (action.type === instr.MOVE_ASSET_TO_GROUP) {
        console.log("modellerReducer: moved asset group", action.payload);
        let sourceGroup = action.payload.sourceGroup;
        let targetGroup = action.payload.targetGroup;
        let updatedAsset = action.payload.asset;
        
        return {
            ...state,
                model: {
                    ...state.model,
                    assets: [...state.model.assets.map((asset) => {
                        if (asset["id"] === updatedAsset["id"]) {
                            asset["iconX"] = updatedAsset["iconX"];
                            asset["iconY"] = updatedAsset["iconY"];
                            asset["grouped"] = true;
                        }
                        return asset;
                    })]
                },
                groups: [...state.groups.map((group) => {
                    if (group["id"] === sourceGroup["id"]) {
                        group.assetIds = [ ...sourceGroup.assetIds ];
                    }
                    else if (group["id"] === targetGroup["id"]) {
                        group.assetIds = [ ...targetGroup.assetIds ];
                    }
                    return group;
                })],
        }
    };
    
    if (action.type === instr.PUT_GROUP_LOC) {
        console.log("modellerReducer: updated group location: ", action.payload);
        let updatedGroup = action.payload;

        return {
            ...state,
            groups: [...state.groups.map((group) => {
                if (group["id"] === updatedGroup["id"]) {
                    group["left"] = updatedGroup["left"];
                    group["top"] = updatedGroup["top"];
                }
                return group;
            })]
            //movedGroup: true //TODO: may need this
        };
    }

    if (action.type === instr.PUT_GROUP_RESIZE) {
        console.log("modellerReducer: updated group size: ", action.payload);
        let updatedGroup = action.payload;

        return {
            ...state,
            groups: [...state.groups.map((group) => {
                if (group["id"] === updatedGroup["id"]) {
                    group["width"] = updatedGroup["width"];
                    group["height"] = updatedGroup["height"];
                    group["resizable"] = false;
                }
                return group;
            })]
        };
    }

    if (action.type === instr.PUT_GROUP_RENAME) {
        console.log("modellerReducer: updated group label: ", action.payload);
        let updatedGroup = action.payload;

        return {
            ...state,
            groups: [...state.groups.map((group) => {
                if (group["id"] === updatedGroup["id"]) {
                    group["label"] = updatedGroup["label"];
                }
                return group;
            })]
        };
    }

    if (action.type === instr.PUT_GROUP_EXPANDED) {
        console.log("modellerReducer: updated group expanded: ", action.payload);
        let updatedGroup = action.payload;
        let expanded = updatedGroup["expanded"];

        return {
            ...state,
            groups: [...state.groups.map((group) => {
                if (group["id"] === updatedGroup["id"]) {
                    group["expanded"] = expanded;
                    group["resizable"] = false; //reset this
                }
                return group;
            })]
        };
    }

    if (action.type === instr.SET_GROUP_RESIZABLE) {
        console.log("modellerReducer: set group resizable: ", action.payload);
        let groupId = action.payload.groupId;
        let resizable = action.payload.resizable;

        return {
            ...state,
            groups: [...state.groups.map((group) => {
                if (group["id"] === groupId) {
                    group["resizable"] = resizable;
                }
                return group;
            })]
        };
    }

    if (action.type === instr.TOGGLE_DEVELOPER_MODE) {
        return {
            ...state,
            developerMode: !state.developerMode
        }
    }

    if (action.type === instr.OPEN_GRAPH_WINDOW) {
        console.log("modellerReducer: open graph window: ", action.payload);
        window.open(action.payload);
    }

    return state;
}

function updateControlStrategies(threats, controlStrategies, controlSets) {
    let controlSetsMap = {};
    controlSets.forEach(cs => {
        controlSetsMap[cs.uri] = cs;
    });

    threats.forEach((threat) => {
        threat.resolved = threat.acceptanceJustification !== null;
        let triggered = false; //set updated value initially to false, then set to true below, if any triggers are active

        let csgTypes = threat["controlStrategies"];

        let csgsAsArray = Object.keys(csgTypes).map(csgUri => {
            return controlStrategies[csgUri];
        });

        csgsAsArray.forEach((csg) => {
            let mandatoryControlSetUris = csg.mandatoryControlSets;
            let mandatoryControlSets = mandatoryControlSetUris.map(csUri => {
                let cs = controlSetsMap[csUri];
                return cs;
            });

            //CSG is enabled if there are no mandatory controls, or if one of them is not proposed
            csg.enabled = (mandatoryControlSets.length) = 0 ? true : mandatoryControlSets.find((control) => !control["proposed"]) === undefined;

            if (csg.type !== "TRIGGER" && csg.enabled) {
                threat.resolved = true; //if any non-triggering CSGs are enabled, the threat is resolved
            }
            else if (csg.type === "TRIGGER" && csg.enabled) {
                triggered = true; //if any triggering CSGs are enabled, the threat is triggered
            }
        });

        // Finally, if triggered state has changed, update the threat
        if (threat.triggered !== triggered) {
            //console.log("threat " + (triggered ? "triggered" : "untriggered") + ": " + threat.label);
            threat.triggered = triggered;
        }
    });
}

function updateComplianceSets(complianceSets, complianceThreats, controlStrategies, controlSets) {
    complianceSets.forEach((complianceSet) => {
        let systemThreats = complianceSet.systemThreats.map((threatUri) => {
            return complianceThreats.find((threat) => threat.uri === threatUri);
        });
        updateControlStrategies(systemThreats, controlStrategies, controlSets);
        if (systemThreats.length > 0) {
            complianceSet.compliant = true;
            systemThreats.forEach((threat) => {
                if (!threat.resolved) {
                    complianceSet.compliant = false; //if any are unresolved, compliant is false
                }
            });
        }
    });
}

function getMisbehaviours(assetThreats) {
    //This is a set of misbehaviours, grouped into those with the same label
    var misbehavioursSet = {};

    assetThreats.map((threat) => {
        var threatMisbehaviours = threat.misbehaviours;

        threatMisbehaviours = threatMisbehaviours.map((misbehaviour) => {
            var misbehaviourWithThreat = misbehaviour;

            var threatSummary = {
                "id": threat.id,
                "label": threat.label,
                "pattern": threat.pattern
            }

            misbehaviourWithThreat["threat"] = threatSummary;

            //console.log(misbehaviour.misbehaviourLabel);

            var misbehavioursGroup;

            if (misbehavioursSet.hasOwnProperty(misbehaviour.misbehaviourLabel)) {
                misbehavioursGroup = misbehavioursSet[misbehaviour.misbehaviourLabel]
            }
            else {
                misbehavioursGroup = {"misbehaviourLabel": misbehaviour.misbehaviourLabel, "assetLabel": misbehaviour.assetLabel, "misbehaviours": []}
                misbehavioursSet[misbehaviour.misbehaviourLabel] = misbehavioursGroup;
            }

            misbehavioursGroup.misbehaviours.push(misbehaviourWithThreat);

            return misbehaviourWithThreat;
        });

        return threat;
    });

    console.log("misbehavioursSet");
    console.log(misbehavioursSet);

    return misbehavioursSet;
}

function printThreats(threats) {
    console.log("All threats (" + threats.length + "):");

    // Sort by id
    let sortedThreats = threats.sort(function (a, b) {
        return (a.id < b.id) ? -1 : (a.id > b.id) ? 1 : 0;
    });

    sortedThreats.map((threat) => {
        console.log(threat.id);
    });
}

function updateAssetHistory(asset, history, pointer) {
    //console.log("Update Asset History with " + asset);
    //console.log("History " + history);
    //console.log("History Pointer " + pointer);

    if(pointer>=modelState.historySize-1 && history[pointer]!=null){
        pointer = modelState.historySize-1;
        var i;
        for(i = 0; i<modelState.historySize-1; i++){
            history[i] = history[i+1];
        }
        history[pointer] = asset;
    } else {
         history[pointer] = asset;
         var j;
         for(j = pointer+1; j<=modelState.historySize-1; j++){
            history[j] = null;
         }
    }

    //console.log("New History" + history);
    return history;
}

function deleteAssetsFromHistory(assets, history, pointer) {

    //console.log("deleteAssetsFromHistory: ", assets, history, pointer);

    var updatedPointer = pointer;

    //parse 1: remove all instances of the asset(s)
    var i;
    for (i = 0; i <= modelState.historySize-1; i++){

        if (assets.includes(history[i])){
            //console.log("Deleting: " + history[i]);
            history[i] = null;
            if (i <= pointer)
                updatedPointer--;
        }
    }

    //parse 2: squish it so there are no nulls
    history = removeNullsInHistory(history);
    
    var updatedPointer2 = updatedPointer;
    
    //parse 3: delete adjacent identical Assets
    for (i = 0; i <= modelState.historySize-2; i++){
        var j;
        if (history[i] !== null){
            //console.log("Looking at:" + history[i]);
            for (j = i+1; j<=modelState.historySize-1; j++){
                if (history[i] === history[j]){
                    console.log(history[j] === "" ? "Deleting Blank" : "Deleting: " + history[j]);
                    history[j] = null;
                    if (i <= updatedPointer)
                        updatedPointer2--;
                } else {
                    break;
                }
            }
        }
    }

    history = removeNullsInHistory(history);
    
    return {hist: history, point: updatedPointer2};

}

function removeNullsInHistory(history) {
    var i;
    //console.log("Remove Null from : " + history);
    for (i = 0; i<=modelState.historySize-1; i++){
        if (history[i] === null){
            var j;
            for (j = i+1; j <= modelState.historySize-1; j++){
                if (history[j] !== null){
                    //console.log("Moving " + history[j] + " into " + history[i]);
                    history[i] = history[j];
                    history[j] = null;
                    break;
                }
            }
        }
    }
    //console.log("Done: " + history);
    return history;
}

//sort all levels arrays
function sortLevels(levelsMap) {
    //console.log("sortLevels: unsorted: ", levelsMap);

    Object.keys(levelsMap).map((levelKey) => {
        let levels = levelsMap[levelKey];
        //console.log(levelKey, levels);
        levels.sort(function(a, b) {
            return b.value - a.value;
        });
    });

    //console.log("sortLevels: sorted: ", levelsMap);
}

/* Uncomment if required for testing sorting, etc
function getSecondaryEffectsMockData() {
    var secondaryEffects = {
        misbehaviours: [
            {misbehaviourLabel: "MisbehaviourB", assetLabel: "assetA"},
            {misbehaviourLabel: "MisbehaviourA", assetLabel: "assetB"},
            {misbehaviourLabel: "MisbehaviourC", assetLabel: "assetB"},
            {misbehaviourLabel: "MisbehaviourC", assetLabel: "assetA"},
            {misbehaviourLabel: "MisbehaviourA", assetLabel: "assetC"},
            {misbehaviourLabel: "MisbehaviourB", assetLabel: "assetC"},
            {misbehaviourLabel: "MisbehaviourC", assetLabel: "assetC"},
            {misbehaviourLabel: "MisbehaviourA", assetLabel: "assetA"},
            {misbehaviourLabel: "MisbehaviourB", assetLabel: "assetB"}
        ],
        threats: [

        ]
    }
    console.log("getSecondaryEffectsMockData:");
    console.log(secondaryEffects);
    return secondaryEffects;
}
*/
