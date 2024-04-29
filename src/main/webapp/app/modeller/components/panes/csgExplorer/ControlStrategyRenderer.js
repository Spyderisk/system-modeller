import React from "react";
import * as Constants from "../../../../common/constants.js";
import {OverlayTrigger, Tooltip, Well} from "react-bootstrap";
import ThreatsPanel from "../details/accordion/panels/ThreatsPanel";
import {
    openControlStrategyExplorer,
} from "../../../actions/ModellerActions";
import {bringToFrontWindow} from "../../../actions/ViewActions";

export function renderControlStrategy(csgName, controlStrategy, csgKey, threat, state, props, ths, context) {
    const csgDescription = controlStrategy.description ? controlStrategy.description : csgName;
    let blockingEffect = controlStrategy.blockingEffect;
    let controlStrategyControlSetUris = controlStrategy.mandatoryControlSets.concat(controlStrategy.optionalControlSets);
    let controlStrategyControlSets = controlStrategyControlSetUris.map(csUri => state.controlSets[csUri]);
    let csgType = threat ? controlStrategy.threatCsgTypes[threat.uri] : "";
    let assets = props.assets ? props.assets : props.model.assets;
    let showCsgLink = (context !== "csg-explorer"); //only show link to CSG explorer from other panels

    let csgThreats;

    if (!threat) {
        let threats = props.threats ? props.threats : props.model.threats;
        let threatUris = Object.keys(controlStrategy.threatCsgTypes);

        csgThreats = threatUris.map((threatUri) => {
            let threat = threats.find((threat) => threat["uri"] === threatUri);
            return threat;
        });
    }

    return (
        <div key={csgKey} style={{marginBottom: "7px"}}>
            <Well
                bsClass={"well well-sm strategy "
                + (controlStrategy.enabled ? csgType === "BLOCK"
                    ? "enabled blocked" : csgType === "MITIGATE"
                        ? "enabled mitigated" : "enabled" : "")}
                >
                {/*TODO: If MITIGATE is deprecated this conditional is unnecessary and can just be replaced with fa-star, or arguably no icon at all*/}
                <p>
                    {csgType === "BLOCK" ? <span className="fa fa-star"/>
                        : csgType === "MITIGATE" ? <span className="fa fa-star-half"/>
                            : csgType === "" ? null : <span className="fa fa-star-o"/>
                    }
                    <span className={"csg" + (showCsgLink ? "" : " no-link")}
                        onClick={() => showCsgLink ? openCsgExplorer([controlStrategy], {"selection": "csg", "asset": props.asset}, props) : undefined}
                    ><a>{csgName + " "}</a>
                    </span>
                    <span>
                        {blockingEffect ? "(" + blockingEffect.label + " effectiveness)" : ""}
                    </span>
                </p>
                <p>{csgDescription}</p>

                {controlStrategyControlSets.sort((a, b) => a["label"].localeCompare(b["label"])).map((control, index) => {
                    control.optional = controlStrategy.optionalControlSets.includes(control.uri);

                    let threatensAsset = false;
                    if (!threat || (threat && (_.find(assets, ['uri', threat.threatensAssets]))) ) {
                        threatensAsset = true;
                    }

                    let assetName = filterNodeName(control["assetId"], control["assetUri"], threat, assets);

                    return renderControlSet(control, index, controlStrategy, threatensAsset, assetName, props, state, ths);
                })}

                {(context === "csg-explorer") && <h5>Related Threats</h5>}

                {csgThreats && <ThreatsPanel dispatch={props.dispatch}
                                            name={"model-summary"}
                                            context={props.model.id}
                                            model={props.model}
                                            threats={csgThreats}
                                            selectedAsset={null}
                                            selectedThreat={props.selectedThreat}
                                            displayRootThreats={false}
                                            hoverThreat={props.hoverThreat}
                                            getRootThreats={null} // method not required here
                                            threatFiltersActive={null} // not required here
                            />}


            </Well>
        </div>
    )
}

export function renderControlSet(control, index, controlStrategy, threatensAsset, assetName, props, state, ths) {
    let spinnerActive = state.updatingControlSets[control.uri] === true;
    let optionalControl = control.optional;
    let errorFlag = control["error"];
    let errorMsg = "Request failed. Please try again";

    let errorFlagOverlay = "";
    if (errorFlag) {
        let errorFlagOverlayProps = {
            delayShow: Constants.TOOLTIP_DELAY, placement: "right",
            overlay: <Tooltip id={"control-set-" + control.id + "-error-tooltip"}
                              className={"tooltip-overlay"}>
                {errorMsg}
            </Tooltip>
        };
        errorFlagOverlay = <OverlayTrigger {...errorFlagOverlayProps}>
            <div>
                &nbsp;
                <span>
                    {/* show icon according to error / busy etc */}
                    <i className="fa fa-exclamation-triangle" style={{color : "#bf0500"}}/>
                </span>
            </div>
        </OverlayTrigger>;
    } else if (spinnerActive) {
        let errorFlagOverlayProps = {
            delayShow: Constants.TOOLTIP_DELAY, placement: "right",
            overlay: <Tooltip id={"control-set-" + control.id + "-error-tooltip"}
                              className={"tooltip-overlay"}>
                <span>
                    <i className="fa fa-spinner fa-pulse fa-lg fa-fw"/>Processing
                </span>
            </Tooltip>
        };
        errorFlagOverlay = <OverlayTrigger {...errorFlagOverlayProps}>
            <div>
                &nbsp;
                <span><i className="fa fa-spinner fa-pulse fa-lg fa-fw"/></span>
            </div>
        </OverlayTrigger>;
    }

    let cursor = (!threatensAsset || !control["assertable"]) ? 'not-allowed' : 'pointer';

    if (props.authz.userEdit == false) {
        cursor = "default"
    }
    
    return (
        <div key={index} className="row detail-info control-info">
            <Well
                bsClass={"well well-sm control" + (optionalControl ?  " optional" : "")}
                disabled={!threatensAsset || !control["assertable"]}
            >
                <span 
                    className={"inline-flex inverted " + (props.authz.userEdit ? "traffic-lights" : "traffic-lights-view-only")} 
                    style={{cursor: cursor}}
                    disabled={!threatensAsset || !control["assertable"]}
                >
                    <OverlayTrigger key={"red" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                        <Tooltip id={"ctrl-red-tooltip-" + index}
                                 className={"tooltip-overlay"}>
                            {"Control not implemented"}
                        </Tooltip>
                    }>
                        <span className="fa-stack" onClick={() => props.authz.userEdit ? toggleControlProposedState(control, controlStrategy, ths, false) : undefined}>
                            <i className="bg fa fa-circle fa-stack-1x"/>
                            <i className={!control.proposed ? "red fa fa-stop-circle fa-stack-1x" : "red fa fa-circle fa-stack-1x"}/>
                        </span>
                    </OverlayTrigger>
                    <OverlayTrigger key={"amber" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                        <Tooltip id={"ctrl-amber-tooltip-" + index}
                                 className={"tooltip-overlay"}>
                            {"Control to be implemented"}
                        </Tooltip>
                    }>
                        <span className="fa-stack" onClick={() => props.authz.userEdit ? toggleControlProposedState(control, controlStrategy, ths, true, true) : undefined}>
                            <i className="bg fa fa-circle fa-stack-1x"/>
                            <i className={control.proposed && control.workInProgress ?  "amber fa fa-stop-circle fa-stack-1x" : "amber fa fa-circle fa-stack-1x"}/>
                        </span>
                    </OverlayTrigger>
                    <OverlayTrigger key={"green" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                        <Tooltip id={"ctrl-green-tooltip-" + index}
                                 className={"tooltip-overlay"}>
                            {"Control is implemented"}
                        </Tooltip>
                    }>
                        <span className="fa-stack" onClick={() => props.authz.userEdit ? toggleControlProposedState(control, controlStrategy, ths, true) : undefined}>
                            <i className="bg fa fa-circle fa-stack-1x"/>
                            <i className={control.proposed && !control.workInProgress ? "green fa fa-stop-circle fa-stack-1x" : "green fa fa-circle fa-stack-1x"}/>
                        </span>
                    </OverlayTrigger>
                </span>
                <OverlayTrigger key={index} delayShow={Constants.TOOLTIP_DELAY}
                                placement="left" overlay={
                    <Tooltip id={"ctrl-strat-desc-tooltip-" + index}
                             className={"tooltip-overlay"}>
                        {control.description !== null
                            ? control["description"]
                            : "No description available"}
                    </Tooltip>
                }>
                    <span className="control-description">
                        {control["label"]}
                        {' at "'}
                        {assetName}
                        {'" '}
                        <i className="fa fa-info-circle"></i>
                        {optionalControl ? ' (optional) ' : ''}
                    </span>
                </OverlayTrigger>
            </Well>
        {errorFlagOverlay}
        </div>
    )
}

function openCsgExplorer(csgs, context, props) {
    if (csgs.length > 0) {
        props.dispatch(openControlStrategyExplorer(csgs, context));
        props.dispatch(bringToFrontWindow("controlStrategyExplorer"));
    }
}

function toggleControlProposedState(control, controlStrategy, ths, proposed = true, workInProgress = false) {
    let updateThreat = ths.props.updateThreat ? ths.props.updateThreat : ths.updateThreat;

    updateThreat({
        control: {
            ...control,
            proposed: proposed,
            workInProgress: workInProgress,
            controlStrategy: controlStrategy ? controlStrategy["id"] : null
        },
    });

    let updatingControlSets = {...ths.state.updatingControlSets};
    updatingControlSets[control.uri] = true;

    let controlSets = {...ths.state.controlSets};
    let updatedCs = {...controlSets[control.uri],
        proposed: proposed,
        workInProgress: workInProgress
    };
    controlSets[control.uri] = updatedCs;

    ths.setState({
        updatingControlSets: updatingControlSets,
        controlSets: controlSets
    });
}

function getAssetByUri(assetUri, assets) {
    let asset = assets.find((asset) => {
        return (asset.uri === assetUri);
    });
    return asset;
}

export function filterNodeName(id, uri, threat, assets) {
    let pattern = threat ? threat.pattern : null;
    if (!pattern) {
        let asset = getAssetByUri(uri, assets);
        if (asset) {
            return asset.label;
        }
        else {
            return id;
        }
    }
    let threatensAsset = pattern.nodes.find(a => uri === a.asset);
    if (threatensAsset) {
        return threatensAsset["assetLabel"];
    }
    var node = assets.find((asset) => {
        return asset["id"] === id;
    });

    if (node === undefined) {
        return id;
    } else {
        return node["label"];
    }
}

