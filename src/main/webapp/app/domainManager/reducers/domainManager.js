export const GET_ONTOLOGIES = "GET_ONTOLOGIES";
export const GET_PALETTE = "GET_PALETTE";
export const GET_USERS = "GET_USERS";
export const GET_USERS_ALL = "GET_USERS_ALL";
export const UPDATE_UPLOAD_PROGRESS = "UPDATE_UPLOAD_PROGRESS";
export const UPLOAD_DOMAIN = "UPLOAD_DOMAIN";
export const TOGGLE_UPLOAD_MODAL = "TOGGLE_UPLOAD_MODAL";
export const EXPORT_DOMAIN = "EXPORT_DOMAIN";

//Provides a way of updating the store for the dashboard, based on a set of instructions defined in dashboardConstants.js
export const domainManager = (state = {
    ontologies: {},
    upload: {
        isOpen: false,
        domain: undefined,
        domainUri: undefined,
        newDomain: false,
        progress: 0.0,
        completed: false,
        status: {
            failed: false,
            reason: ""
        }
    },
    activePalette: {},
    userList: [],
    activeList: [],
    download: {}
}, action) => {
    //console.log(action.payload);
    switch(action.type){
        case GET_ONTOLOGIES:
            return {
                ...state,
                ontologies: action.payload
            };
        case GET_PALETTE:
            return {
                ...state,
                activePalette: action.payload
            };
        case GET_USERS:
            //console.log("GET_USERS", action.payload);
            return {
                ...state,
                activeList: action.payload
            };
        case GET_USERS_ALL:
            //console.log("GET_USERS_ALL", action.payload);
            return {
                ...state,
                userList: action.payload
            };
        case UPLOAD_DOMAIN:
            if (action.payload.error) {
                return {
                    ...state,
                    upload: {
                        ...state.upload,
                        completed: false,
                        status: {
                            failed: true,
                            reason: action.payload.message ? action.payload.message : action.payload.error
                        }
                    }
                };
            }
            else {
                return {
                    ...state,
                    ontologies: action.payload,
                    upload: {
                        ...state.upload,
                        progress: 100,
                        completed: true
                    }
                };
            }
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
        case TOGGLE_UPLOAD_MODAL:
            let {isOpen, domain, domainUri, newDomain} = action.payload;
            return {
                ...state,
                upload: {
                    ...state.upload,
                    isOpen: isOpen,
                    domain: domain,
                    domainUri: domainUri,
                    newDomain: newDomain,
                    progress: 0.0,
                    completed: false,
                    status: {
                        failed: false,
                        reason: ""
                    }
                }
            };
    }
    return state;
};
