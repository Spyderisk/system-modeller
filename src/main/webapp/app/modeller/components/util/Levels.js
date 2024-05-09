import React from "react";
import {OverlayTrigger, Tooltip, FormControl} from "react-bootstrap";
import * as Constants from "../../../common/constants.js";

export function getRenderedLevelText(levels, level, reverseColours, emptyLevelTooltip) {

    if (levels === null || level === undefined || level === null){
        return renderEmptyLevel(emptyLevelTooltip);
    } else {
        
        let colour = getLevelColour(levels, level, reverseColours);

        return <span 
                style={{backgroundColor: colour}}
                className="level"
            >
            {level.label}
        </span>
    }

}

function renderEmptyLevel(tooltip) {
    let tooltipText = tooltip ? tooltip : "Not yet available - please run Risk Calculation";
    return (
        <OverlayTrigger 
                        delayShow={Constants.TOOLTIP_DELAY}
                        placement="bottom"
                        trigger={["hover"]}
                        overlay={<Tooltip id="empty-levels-tooltip" className={"tooltip-overlay"}>
                            {tooltipText}</Tooltip>}>
            <strong>N/A</strong>
        </OverlayTrigger>
    );
}

//reverseColours flag reverses the color map, e.g. highest level is green (not red)
export function getLevelColour(levels, level, reverseColours) {
    
    if (levels == null || level == null){
        return 'white'; //TODO: check this
    }
    else {
        /* Sorting no longer required, as all levels are now initially sorted
        console.log("sorting levels");
        let sortedLevels = Object.values(levels).sort(function(a, b) {
            return b.value - a.value;
        });
        let maxVal = sortedLevels[0].value;
        */
       
        //TODO: following code calculates colours on-the-fly
        //These could be pre-calculated after loading model (i.e. after levels are sorted)

        let maxVal = levels[0].value;
        
        let levelValue;
        
        if (reverseColours) {
            levelValue = maxVal - level.value; //reverse level value
        }
        else {
            levelValue = level.value;
        }

        let r = "d8";
        let g = (230 - (100 * levelValue/maxVal)).toString(16);
        let b = (230 - (100 * levelValue/maxVal)).toString(16);

        let colour = '#' + r + g + b;
        
        return colour;
    }
}

export function getThreatColor(threat, controlStrategies, levels, returnBE) {
    let csgUris = Object.keys(threat["controlStrategies"]);
    let maxBlockingEffect = {value: -1};

    csgUris.map(csgUri => {
        let csg = controlStrategies[csgUri];
        if (csg.blockingEffect === null) {
            console.warn("Null blockingEffect for CSG: ", csg.uri);
        }
        else {
            if (csg.enabled && csg.blockingEffect.value > maxBlockingEffect.value)  {
                maxBlockingEffect = csg.blockingEffect;
            }
        }
    });

    let color = (maxBlockingEffect.value > -1) ? getLevelColour(levels, maxBlockingEffect, true) : undefined;
    
    if (returnBE) {
        return {be: maxBlockingEffect, color: color};
    }
    else {
        return color;
    }
}

export function getLevelValue(levelLabel) {
    if (levelLabel) levelLabel = levelLabel.label;
    let level = 0;
    switch (levelLabel) {
        case "Very Low": level = 1; break;
        case "Low": level = 2; break;
        case "Medium": level = 3; break;
        case "High": level = 4; break;
        case "Very High": level = 5; break;
    }
    return level;
}

export function renderCoverageLevel(id, coverageLevel, levels, userEdit, updating, onChangeCallback) {
    //console.log("renderCoverageLevel: ", id, coverageLevel);

    if (coverageLevel == null)
        return null;

    return (
        <span className="impact">
            <FormControl
                disabled={!userEdit}
                componentClass="select"
                className="impact-dropdown level"
                id={id}
                value={coverageLevel.uri}
                style={{ backgroundColor: getLevelColour(levels, coverageLevel, true) }}
                onChange={onChangeCallback}
                ref="select-coverage">
                {levels.map((level, index) =>
                    <option key={index + 1}
                        value={level.uri}
                        style={{ backgroundColor: getLevelColour(levels, level, true) }}>
                        {level.label}
                    </option>
                )};
            </FormControl>
            {updating ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw" /> : null}
        </span>
    );
}

export function renderPopulationLevel(asset, populationLevel, levels, userEdit, updating, onChangeCallback) {
    //console.log("renderPopulationLevel: ", asset.label, asset.id, populationLevel);

    if (populationLevel == null)
        return null;

    return (
        <span className="population">
            <FormControl
                disabled={!userEdit}
                componentClass="select"
                className="population-dropdown level"
                id={asset.id}
                value={populationLevel.uri}
                style={{ backgroundColor: getLevelColour(levels, populationLevel, false) }}
                onChange={onChangeCallback}
                ref="select-population-level">
                {levels.map((level, index) =>
                    <option key={index + 1}
                        value={level.uri}
                        style={{ backgroundColor: getLevelColour(levels, level, false) }}>
                        {level.label}
                    </option>
                )};
            </FormControl>
            {updating ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw" /> : null}
        </span>
    );
}
