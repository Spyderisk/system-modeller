import * as instr from "../modellerConstants";
import {polyfill} from "es6-promise";

polyfill();


export function addAsset(asset, clipboard=false) {
    // console.log("addAsset: ", asset);
    return function (dispatch) {
        dispatch({
            type: clipboard ?
                instr.INTERACTION_COPY_ASSET :
                instr.INTERACTION_ADD_ASSET,
            payload: {
                'uri': asset.uri,
                'name': asset.label,
                'id': asset.id,
                'iconX': asset.iconX,
                'iconY': asset.iconY,
                'type': asset.type,
            }
        })
    };
}

export function addRelation(relation, clipboard=false) {
    return function (dispatch) {
        dispatch({
            type: clipboard ?
                instr.INTERACTION_COPY_RELATION :
                instr.INTERACTION_ADD_RELATION,
            payload: {
                'uri': relation.uri,
                'label': relation.label,
                'id': relation.id,
                'fromID': relation.fromID,
                'toID': relation.toID,
                'type': relation.type,
            }
        })
    };
}

export function clearClipboard() {
    return function (dispatch) {
        dispatch({
            type: instr.INTERACTION_CLEAR_CLIPBOARD,
            payload: null
        })
    }
}
