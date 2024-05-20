import React, {Fragment} from "react";
import PropTypes from "prop-types";
import {OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../common/constants.js";
import ThreatAccordion from "./accordion/ThreatAccordion";
import {Rnd} from "react-rnd";
//import {getCauseEffect} from "../../../../modeller/actions/ModellerActions";
import {activateThreatEditor} from "../../../../modeller/actions/ModellerActions";
import {changeSelectedAsset} from "../../../actions/ModellerActions";
import {getRenderedLevelText} from "../../util/Levels";
import {bringToFrontWindow, closeWindow} from "../../../actions/ViewActions";
import {connect} from "react-redux";
import {openDocumentation} from "../../../../common/documentation/documentation"
import {getThreatStatus} from "../../util/ThreatUtils.js";

class ThreatEditor extends React.Component {

    constructor(props) {
        super(props);

        this.getAssetByUri = this.getAssetByUri.bind(this);

        this.rnd = null;
    }

    /**
     * React Lifecycle Methods
     */

    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;
        /* This doesn't work, if other props have changed
        //If visible flag has changed, then update
        let shouldComponentUpdate = this.props.isVisible != nextProps.isVisible;
        */

        //If it not visible and is still not visible, it should not update
        //console.log("this.props.isVisible: " + this.props.isVisible);
        //console.log("nextProps.isVisible: " + nextProps.isVisible);
        if ((!this.props.isVisible) && (!nextProps.isVisible)) {
            //console.log("ThreatEditor.shouldComponentUpdate: false: (not visible)");
            shouldComponentUpdate = false;
        }
        else if (this.props.loadingRootCauses != nextProps.loadingRootCauses) {
            //console.log("ThreatEditor.shouldComponentUpdate: false: (loadingRootCauses changed)");
            shouldComponentUpdate = false;
        }
        else {
            //console.log("ThreatEditor.shouldComponentUpdate: " + shouldComponentUpdate);
        }

        //TODO: This could be improved to find other conditions when component should not be updated
        return shouldComponentUpdate;
    }

    render() {
        let threat = this.props.threat;

        let asset;
        let assetLabel = "";
        if (threat) {
            asset = this.getAssetByUri(threat.threatensAssets);
            if (asset) assetLabel = asset["label"];
            else {
                asset = threat.pattern.nodes.find(a => threat["threatensAssets"] === a.asset);
                if (asset) {
                    assetLabel = asset["assetLabel"];
                }
            }
        } else {
            return null;
        }

        let clazzStyle = {display: "none"};
        let root_cause = threat.rootCause;
        let secondary = threat.secondaryThreat;
        let primary = !secondary;

        let isComplianceThreat = threat && threat.isComplianceThreat;
        let threatType = "unknown";
        
        if (primary) {
            threatType = root_cause ? "Primary (Root Cause)" : "Primary";
        }
        else if (secondary) {
            threatType = "Secondary";
        }
        else if (threat.isModellingError) {
            threatType = "Modelling";
        }
        else if (isComplianceThreat) {
            threatType = "Compliance";
        }

        let impact = threat !== undefined ? threat["impactLevel"] : null;
        let likelihood = threat !== undefined ? threat["likelihood"] : null;
        let risk = threat !== undefined ? threat["riskLevel"] : null;

        let impactLevel = impact != null ? impact.label : "";

        let likelihoodRender = <strong>Error</strong>;
        let riskRender = <strong>Error</strong>;

        let statusString = getThreatStatus(threat, this.props.model.controlStrategies);
        let status;
        let triggeredStatus = "";

        if (statusString.includes("/")) {
            let arr = statusString.split("/");
            status = arr[0];
            triggeredStatus = arr[1];
        }
        else {
            status = statusString;
        }

        let emptyLevelTooltip; //tooltip to display when likelihood or risk is not available ("N/A")
        let triggerableText; //text to display for a triggerable threat

        if (triggeredStatus === "UNTRIGGERED") {
            triggerableText = "N.B. This threat is not currently active. It is triggered when a particular control strategy is enabled.";
            emptyLevelTooltip = "This threat poses no risk as it has not been enabled by a control strategy";
        }
        else if (triggeredStatus === "TRIGGERED") {
            triggerableText = "N.B. This threat has been triggered by a particular control strategy which is currently enabled.";
        }
        
        if (this.props.model.levels != null){
            likelihoodRender = getRenderedLevelText(this.props.model.levels.Likelihood, likelihood, false, emptyLevelTooltip);
            riskRender = getRenderedLevelText(this.props.model.levels.RiskLevel, risk, false, emptyLevelTooltip);
        }

        let assetLabelHeading = <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
                    trigger={["hover"]}
                    overlay={
                        <Tooltip id="threaten-asset-tooltip"
                                className={"tooltip-overlay"}>
                            {"Select asset"}
                        </Tooltip>
                    }>
                    <span className="highlighted"
                        style={{cursor: "pointer", fontSize: "inherit"}}
                        onClick={() => this.props.dispatch(
                            changeSelectedAsset(asset ? asset.id : ""))
                        }>
                        {assetLabel}
                    </span>
                </OverlayTrigger>

        let windowTitle, windowHelpLink;
        if (threat.isModellingError) {
            windowTitle = "Modelling Error Explorer";
            windowHelpLink = "modelling-error-explorer"
        }
        else if (isComplianceThreat) {
            windowTitle = "Compliance Threat Explorer";
            windowHelpLink = "compliance-threat-explorer"
        } else {
            windowTitle = "Threat Explorer";
            windowHelpLink = "threat-explorer"
        }

        return (
            <Rnd ref={c => { this.rnd = c; }}
                bounds='#view-boundary'
                default={{
                    x: window.outerWidth * 0.25,
                    y: (100 / window.innerHeight) * window.devicePixelRatio,
                    width: 560,
                    height: 660,
                }}
                style={{ zIndex: this.props.windowOrder }}
                minWidth={150}
                minHeight={200}
                onResize={(e) => {
                    if (e.stopPropagation) e.stopPropagation();
                    if (e.preventDefault) e.preventDefault();
                    e.cancelBubble = true;
                    e.returnValue = false;
                }}
                disableDragging={this.props.isAcceptancePanelActive} //disable window dragging if accept panel has focus
                cancel={".content, .text-primary, strong, span"} //disable dragging on threat label (hence allowing text selection)
                onDragStart={(e) => {
                    let elName = $(e.target)[0].localName;
                    //console.log("onDragStart: element: " + elName);

                    if (elName === "input" || elName === "textarea" || elName === "button") {
                        return false;
                    }

                    if (this.props.isActive) {
                        //console.log("onDragStart: (threat editor already active)");
                        return;
                    }


                    //console.log("ThreatEditor: dispatch activateThreatEditor")
                    this.props.dispatch(activateThreatEditor());
                }}
                onDrag={(e) => {
                    if (e.stopPropagation) e.stopPropagation();
                    if (e.preventDefault) e.preventDefault();
                    e.cancelBubble = true;
                    e.returnValue = false;
                }}
                className={!this.props.isVisible ? "hidden" : null}>
                <div ref="threat-editor"
                    classID="threatEditor"
                    className="threat-editor"
                    style={!this.props.isVisible ? clazzStyle : null}>
                    <div className="header" onMouseDown={() => {
                        this.props.dispatch(bringToFrontWindow("threatEditor"))
                    }}>

                        <h1>
                            <div className="doc-help-explorer">
                                <div>
                                    {windowTitle}
                                </div>
                            </div>
                        </h1>
                        <span classID="rel-delete"
                            className="menu-close fa fa-times"
                            onClick={() => {
                                this.props.dispatch(closeWindow("threatEditor"));
                                this.props.closeMenu();
                            }}>
                        </span>
                        <span className="menu-close fa fa-question" onClick={e => openDocumentation(e, "redirect/" + windowHelpLink)} />
                    </div>
                    <div className="content">
                        <div className="desc">
                            <div className="descriptor">
                                <h4>
                                    {threatType}
                                    {threat.isModellingError ? " Error at " : " Threat to "}
                                    {assetLabelHeading}
                                </h4>
                            </div>
                            {this.props.developerMode && <p>{threat.uri}</p>}
                            {threat.normalOperation && <p>This "threat" is expected to occur in normal operation.</p>}
                            <p>
                                {threat !== undefined ? threat["description"] : ""}
                            </p>
                            {triggerableText && <p>
                                <b>{triggerableText}</b>
                            </p>}
                            {!isComplianceThreat && <Fragment>
                                <div key={0} className='row head'>
                                    <span className="col-xs-2 likelihood">
                                        Likelihood
                                    </span>
                                    <span className="col-xs-2 risk">
                                        System Risk
                                    </span>
                                    <span className="col-xs-8"></span>
                                </div>
                                <div className='row detail-info'>
                                    <span className="col-xs-2 likelihood">
                                        {likelihoodRender}
                                    </span>
                                    <span className="col-xs-2 risk">
                                        {riskRender}
                                    </span>
                                    <span className="col-xs-8"></span>
                                </div>
                            </Fragment>}
                        </div>

                        <ThreatAccordion dispatch={this.props.dispatch}
                            levels={this.props.model["levels"]}
                            asset={asset}
                            assets={this.props.model["assets"]}
                            relations={this.props.model["relations"]}
                            controlStrategies={this.props.model["controlStrategies"]}
                            controlSets={this.props.model["controlSets"]}
                            modelId={this.props.model["id"]}
                            threat={threat}
                            threatStatus={status}
                            triggeredStatus={triggeredStatus}
                            threats={this.props.model["threats"]}
                            twas={this.props.model["twas"]}
                            selectedMisbehaviour={this.props.selectedMisbehaviour}
                            loadingCausesAndEffects={this.props.loadingCausesAndEffects}
                            getAssetByUri={this.getAssetByUri}
                            isRelationDeletable={this.props.isRelationDeletable}
                            renderTrustworthinessAttributes={this.props.renderTrustworthinessAttributes}
                            authz={this.props.authz}
                            developerMode={this.props.developerMode}
                            />

                        {(this.props.loading !== undefined) && this.props.loading.threats &&
                            <div className="loading-overlay visible"><span className="fa fa-refresh fa-spin fa-3x fa-fw" /><h1>
                                Loading threat editor...</h1></div>}
                    </div>
                </div>
            </Rnd>
        );
    }

    getAssetByUri(uri) {
        let asset = this.props.model.assets.find((asset) => {
            return asset["uri"] === uri;
        });

        return asset;
    }
}

ThreatEditor.propTypes = {
    isVisible: PropTypes.bool,
    isActive: PropTypes.bool, // is in front of other panels
    isAcceptancePanelActive: PropTypes.bool,
    closeMenu: PropTypes.func,
    loading: PropTypes.object,
    threat: PropTypes.object,
    selectedMisbehaviour: PropTypes.object,
    loadingCausesAndEffects: PropTypes.bool,
    loadingRootCauses: PropTypes.bool,
    model: PropTypes.object,
    isRelationDeletable: PropTypes.func,
    renderTrustworthinessAttributes: PropTypes.func,
    authz: PropTypes.object,
    developerMode: PropTypes.bool,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["threatEditor"]
    }
};

export default connect(mapStateToProps)(ThreatEditor);
