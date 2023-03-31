import * as instr from "../modellerConstants";
import {addAsset} from "../actions/InterActions";

var _ = require('lodash');

let defaultState = {
    actions: [],
    clipboard: [],
};

export default function interaction(state=defaultState, action) {

    if (action.type === instr.INTERACTION_ADD_ASSET) {
        return {
            ...state,
            actions: [
                ...state.actions,
                {
                    ...action.payload,
                    'action': 'addAsset'
                }
            ]
        }
    }

    if (action.type === instr.INTERACTION_COPY_ASSET) {
        return {
            ...state,
            clipboard: [
                ...state.clipboard,
                {
                    ...action.payload,
                    'action': 'addAsset'
                }
            ]
        }
    }

    if (action.type === instr.INTERACTION_ADD_RELATION) {
        return {
            ...state,
            actions: [
                ...state.actions,
                {
                    ...action.payload,
                    'action': 'addRelation'
                }
            ]
        }
    }

    if (action.type === instr.INTERACTION_COPY_RELATION) {
        return {
            ...state,
            clipboard: [
                ...state.clipboard,
                {
                    ...action.payload,
                    'action': 'addRelation'
                }
            ]
        }
    }

    if (action.type === instr.INTERACTION_CLEAR_CLIPBOARD) {
        return {
            ...state,
            clipboard: []
        }
    }

    return state
}