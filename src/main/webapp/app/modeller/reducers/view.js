import * as instr from "../modellerConstants";

var _ = require('lodash');

let defaultState = {
    windowOrder: [
        { 'name': 'controlExplorer', order: 1065 },
        { 'name': 'controlStrategyExplorer', order: 1065 },
        { 'name': 'misbehaviourExplorer', order: 1065 },
        { 'name': 'complianceExplorer', order: 1065 },
        { 'name': 'reportDialog', order: 1065 },
        { 'name': 'threatEditor', order: 1065 }
    ],
    'threatEditor': 1065,
    'reportDialog': 1065,
    'complianceExplorer': 1065,
    'misbehaviourExplorer': 1065,
    'controlExplorer': 1065,
    'controlStrategyExplorer': 1065,
};

var highestWindowOrder = 1074;
var hiddenWindowOrder = 1065;


export default function view(state=defaultState, action) {
    //console.log("view:", state, action);
    if (action.type === instr.OPEN_WINDOW) {
        let newWindowOrder = [];
        let newWindowObjects = {};
        // set default highest window order
        let windowName = action.payload;

        let order = highestWindowOrder - 1;
        let newOrder;

        // iterate through windows reduce all by one and set the highest whilst
        // avoiding any hidden ones
        _.map(_.orderBy(state.windowOrder, ['order'], ['desc']), (window, idx) => {
            if (window.name === windowName) newOrder = highestWindowOrder;
            else if (window.order === hiddenWindowOrder) newOrder = hiddenWindowOrder;
            else {
                newOrder = order;
                order -= 1;
            }

            newWindowOrder.push({'name': window.name, 'order': newOrder});
            newWindowObjects[window.name] = newOrder;
        });

        return {
            ...newWindowObjects,
            windowOrder: newWindowOrder
        };
    }

    closeWindow:
    if (action.type === instr.CLOSE_WINDOW) {
        let windowName = action.payload;
        let newWindowOrder = [];
        let newWindowObjects = {};
        let oldWindowIdx = _.findIndex(state.windowOrder, ['name', windowName]);

        if (oldWindowIdx === -1 &&  state.windowOrder[oldWindowIdx].order ===
                hiddenWindowOrder) break closeWindow;

        let order = highestWindowOrder + 1;
        let newOrder;

        _.map(_.orderBy(state.windowOrder, ['order'], ['desc']), (window, idx) => {

            // increase the order of each window below this window
            if (window.name === windowName || window.order === hiddenWindowOrder) {
                newOrder = hiddenWindowOrder;
            }
            else {
                newOrder = order;
                order -= 1;
            }

            newWindowOrder.push({ 'name': window.name, 'order': newOrder });
            newWindowObjects[window.name] = newOrder;
        });

        return {
            ...newWindowObjects,
            windowOrder: newWindowOrder
        };
    }

    return state
}