export const GET_USER = "GET_USER";
export const GET_MODELS = "GET_MODELS";
export const ADD_MODEL = "ADD_MODEL";
export const UPLOAD_MODEL = "UPLOAD_MODEL";
export const REGENERATE_KEY = "REGENERATE_KEY";
export const GET_REPORT = "GET_REPORT";
export const COMPILE_MODEL = "COMPILE_MODEL";
export const COPY_MODEL = "COPY_MODEL";
export const RENAME_MODEL = "RENAME_MODEL";
export const EDIT_MODEL = "EDIT_MODEL";
export const SHARE_MODEL = "SHARE_MODEL";
export const DELETE_MODEL = "DELETE_MODEL";
export const EXPORT_MODEL = "EXPORT_MODEL";
export const GET_ONTOLOGIES = "GET_ONTOLOGIES";
export const UPDATE_UPLOAD_PROGRESS = "UPDATE_UPLOAD_PROGRESS";
export const ADD_COPY_MODEL = "ADD_COPY_MODEL";

export default function dashboard(state = {
    user: {},
    ontologies: [],
    models: [],
    upload: {
        progress: 0.0,
        completed: false,
        status: {
            failed: false,
            reason: ""
        }
    },
    download: {
        progress: 0.0,
        completed: false
    },
    loading: {
        ontologies: false
    }
}, action) {
    switch (action.type) {
        case GET_USER:
            return {...state, user: action.payload};
        case GET_MODELS:
            return {...state, models: action.payload};
        case ADD_MODEL:
            return {...state, models: [...state.models, action.payload]};
        case ADD_COPY_MODEL:
            return {...state, models: [...state.models, action.payload]};
        case EDIT_MODEL:
            let updatedModel = action.payload;
            //console.log("EDIT_MODEL:", updatedModel);
            return {
                ...state,
                models: state.models.map(model => model.id === action.payload.id ?
                    {...model,
                        name: updatedModel.name,
                        description: updatedModel.description
                    } : model)
            };
        case DELETE_MODEL:
            return {
                ...state,
                models: state.models.filter((model) => model["id"] !== action.payload)
            };
        case GET_ONTOLOGIES:
            console.log("GET_ONTOLOGIES: ", action.payload);
            
            if (action.payload["loading"]) {
                console.log("ontologies requested");
                return {...state, loading: {...state.loading, ontologies: true}};
            }
            else if (jQuery.isEmptyObject(action.payload.ontologies)) {
                console.warn("WARNING: no knowledgebases are currently available");
            }
            else {
                console.log("ontologies received:", action.payload.ontologies);
            }
            
            let domainsMap = action.payload.ontologies;
            let domainUris = Object.keys(domainsMap);
            let ontologies = domainUris.map(uri => {
                let domain = domainsMap[uri];
                let ontology = {
                    "name": domain.label,
                    "graph": uri,
                    "version": domain.version
                }
                return ontology;
            });
            sortOntologies(ontologies);
            console.log("Setting ontologies:", ontologies);
            return {...state, ontologies: ontologies, loading: {...state.loading, ontologies: false}};
        case REGENERATE_KEY:
            return {
                ...state,
                models: state.models.map(model => model.id === action.payload.id ?
                    action.payload.isWriteKey ?
                        {...model, id: action.payload.newKey}
                        :
                        {...model, readUrl: action.payload.newKey}
                    :
                    model)
            };
        case UPLOAD_MODEL:
            return {
                ...state,
                models: action.payload,
                upload: {
                    progress: 100,
                    completed: true
                }
            };
        case UPDATE_UPLOAD_PROGRESS:
            return {
                ...state,
                upload: {
                    ...state.upload,
                    progress: action.payload.progress,
                    completed: false,
                    status: action.payload.status
                }
            };
    }

    return state;
}

function sortOntologies(ontologies) {
    //Sort for SHiELD (bring to top of list) 
    ontologies.sort((a, b) => (a.graph.includes("shield")) ? -1 : (b.graph.includes("shield")) ? 1 : 0);
}
