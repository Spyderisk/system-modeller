import React from "react";
import PropTypes from "prop-types";
import {OverlayTrigger, Panel, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";
import {
    activateAcceptancePanel,
    getRootCauses,
    toggleAcceptThreat,
    updateControlOnAsset,
    updateThreat
} from "../../../../../modeller/actions/ModellerActions";
import PatternPanel from "./panels/PatternPanel";
import EffectPanel from "./panels/EffectPanel";
import CausePanel from "./panels/CausePanel";
import ModelMisBehavPanel from "../../details/accordion/panels/ModelMisBehavPanel";
import ControlStrategiesPanel from "./panels/ControlStrategiesPanel";
import {getThreatStatus} from "../../../util/ThreatUtils";
import {bringToFrontWindow} from "../../../../actions/ViewActions";

class ThreatAccordion extends React.Component {

    constructor(props) {
        super(props);

        this.updateThreat = this.updateThreat.bind(this);
        this.activateAcceptancePanel = this.activateAcceptancePanel.bind(this);
        this.toggleAcceptThreat = this.toggleAcceptThreat.bind(this);
        this.getNodes = this.getNodes.bind(this);
        this.getControl = this.getControl.bind(this);
        this.openMisbehaviourExplorer = this.openMisbehaviourExplorer.bind(this);
        this.getEntryPoints = this.getEntryPoints.bind(this);
        this.renderHeader = this.renderHeader.bind(this);
        this.renderHeaderNumbers = this.renderHeaderNumbers.bind(this);
        this.renderCsgPanels = this.renderCsgPanels.bind(this);
        this.renderCsgsPanel = this.renderCsgsPanel.bind(this);
        this.getNonTriggerCsgs = this.getNonTriggerCsgs.bind(this);
        this.getTriggerCsgs = this.getTriggerCsgs.bind(this);

        this.state = {
            expanded: {
                pattern: false,
                cause: true,
                effect: true,
                secondaryEffects: false,
                controlStrategies: true,
            }
        }
    }

    componentWillReceiveProps(nextProps) {
        //console.log("ThreatAccordion: componentWillReceiveProps:", this.props, nextProps);
        let isComplianceThreat = nextProps.threat && nextProps.threat.isComplianceThreat;
        if (isComplianceThreat) {
            this.setState({
                ...this.state,
                expanded: {...this.state.expanded, pattern: true}
            })
        }
    }
    
    render() {
        if(this.props.threat === undefined){
            return null;
        }

        let isComplianceThreat = this.props.threat.isComplianceThreat;        

        //N.B. The causes, effects, secondaryEffects are set in populateThreatMisbehaviours()
        let causes = this.props.threat.secondaryEffectConditions;
        let effects = this.props.threat.misbehaviours;
        let secondaryEffects = this.props.threat.effects;

        //let entryPoints = this.props.threat.entryPoints;
        // get full TWAS objects array for this threat (not for compliance threats)
        let entryPoints = !isComplianceThreat ? this.getEntryPoints(this.props.threat) : [];
        
        //TODO: move the style to a stylesheet
        let directCauseIcon = <span style={{float:"right"}}><i className="fa fa-arrow-right"/> <i className="fa fa-crosshairs"/></span>;
        let directEffectsIcon = <span style={{float:"right"}}><i className="fa fa-crosshairs"/> <i className="fa fa-arrow-right"/></span>;
        let secondaryEffectsIcon = <span style={{float:"right"}}><i className="fa fa-crosshairs"/> <i className="fa fa-arrow-right"/><i className="fa fa-arrow-right"/>∣</span>;

        return (
            <div className="panel-group accordion">
                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div>Detail</div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <PatternPanel threat={this.props.threat}
                                          modelId={this.props.modelId}
                                          asset={this.props.asset}
                                          assets={this.props.assets}
                                          relations={this.props.relations}
                                          getNodes={this.getNodes}
                                          dispatch={this.props.dispatch}
                                          isRelationDeletable={this.props.isRelationDeletable}
                                          authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

                {!isComplianceThreat && <Panel defaultExpanded bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderHeader("Direct cause", directCauseIcon, "The direct cause of a primary threat is loss of trustworthiness in some asset(s); for a secondary threat it is the consequence(s) of other threats.", "threat-direct-cause")}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <CausePanel
                                        modelId={this.props.modelId}
                                        secondaryThreat = {this.props.threat.secondaryThreat}
                                        causes={causes}
                                        entryPoints={entryPoints}
                                        renderTrustworthinessAttributes={this.props.renderTrustworthinessAttributes}
                                        getAssetByUri={this.props.getAssetByUri}
                                        levels={this.props.levels}
                                        selectedMisbehaviour={this.props.selectedMisbehaviour}
                                        loadingCausesAndEffects={this.props.loadingCausesAndEffects}
                                        openMisbehaviourExplorer={this.openMisbehaviourExplorer}
                                        dispatch={this.props.dispatch}
                                        authz={this.props.authz}
                                        />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>}

                {!isComplianceThreat && <Panel defaultExpanded bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderHeader("Direct consequences", directEffectsIcon, "A threat has a direct consequence on a number of other assets with a particular likelihood. The 'Direct Impact' of the consequence combined with this likelihood gives rise to the consequence's 'Direct Risk'.", "threat-direct-effect")}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <EffectPanel modelId={this.props.modelId}
                                         effects={effects}
                                         levels={this.props.levels["ImpactLevel"]}
                                         selectedMisbehaviour={this.props.selectedMisbehaviour}
                                         loadingCausesAndEffects={this.props.loadingCausesAndEffects}
                                         openMisbehaviourExplorer={this.openMisbehaviourExplorer}
                                         dispatch={this.props.dispatch}
                                         authz={this.props.authz}
                                         />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>}

                {!isComplianceThreat && <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderHeader("Secondary consequences", secondaryEffectsIcon, "A threat's direct consequences can go on to cause other problems listed here.", "threat-secondary-effects")}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <p>The direct consequences of this threat go on to cause the following consequences:</p>
                            <ModelMisBehavPanel panelType="secondary-effects"
                                                selectedMisbehaviours={secondaryEffects}
                                                selectedThreat={this.props.threat.id}
                                                adjustAssetNameSizes={false}
                                                authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>}

                {this.renderCsgPanels()}

            </div>
        );
    }

    renderHeaderNumbers(title, icon, title_tt, tt_id, n1, n2) {
        let numbers = "(" + n1 + "/" + n2 + ")";
        let numbers_tt = n1 + " out of " + n2 + " active";
        return this.renderHeader(title, icon, title_tt, tt_id, numbers, numbers_tt);
    }

    renderHeader(title, icon, title_tt, tt_id, numbers, numbers_tt) {
        let tooltip1Props = {
            delayShow: Constants.TOOLTIP_DELAY, placement: "top", trigger: ["hover"],
            overlay: <Tooltip id={tt_id + "-tooltip1"} className={"tooltip-overlay"}>
                {title_tt}
            </Tooltip>
        };
        let overlay1 = <OverlayTrigger {...tooltip1Props}>
            <span>{title}</span>
        </OverlayTrigger>;

        let overlay2 = "";
        if (numbers !== undefined) {
            let tooltip2Props = {
                delayShow: Constants.TOOLTIP_DELAY, placement: "top", trigger: ["hover"],
                overlay: <Tooltip id={tt_id + "-tooltip2"} className={"tooltip-overlay"}>
                    {numbers_tt}
                </Tooltip>
            };
            overlay2 = <OverlayTrigger {...tooltip2Props}>
                <span>{numbers}</span>
            </OverlayTrigger>
        }

        return (
            <React.Fragment>
                {overlay1}{" "}{overlay2}{icon}
            </React.Fragment>
        );
    }

    renderCsgPanels() {
        //Create map of csuri -> cs
        let propControlSets = {};
        this.props.controlSets.forEach(cs => propControlSets[cs.uri] = cs);

        //TODO: only render triggering CSGs panel if there are any
        return (
            [this.renderCsgsPanel(propControlSets, this.getNonTriggerCsgs(), false),
            this.renderCsgsPanel(propControlSets, this.getTriggerCsgs(), true)]
        );
    }

    // Get standard CSGs (non-triggering)
    getNonTriggerCsgs() {
        //get map of control strategy types for this threat
        let csgTypes = this.props.threat["controlStrategies"];

        //filter out any CSGs that are of type "TRIGGER"
        let csgsAsArray = Object.keys(csgTypes).map(csgUri => {
            let csgType = csgTypes[csgUri];
            if (csgType === "TRIGGER")
                return undefined;
            else {
                let csg = this.props.controlStrategies[csgUri];
                return csg;
            }
        }).filter(csg => csg !== undefined);

        return csgsAsArray;
    }

    // Get CSGs that are triggering
    getTriggerCsgs() {
        //get map of control strategy types for this threat
        let csgTypes = this.props.threat["controlStrategies"];

        //filter out any CSGs that are NOT of type "TRIGGER"
        let csgsAsArray = Object.keys(csgTypes).map(csgUri => {
            let csgType = csgTypes[csgUri];
            if (csgType !== "TRIGGER")
                return undefined;
            else {
                let csg = this.props.controlStrategies[csgUri];
                return csg;
            }
        }).filter(csg => csg !== undefined);

        return csgsAsArray;
    }

    renderCsgsPanel(propControlSets, csgsAsArray, triggering) {
        let panelTitle = triggering ? "Triggering Control Strategies" : "Control Strategies";
        let panelTooltip = triggering ? "Control strategies that will trigger this threat" :
                                        "Control strategies that will address this threat";

        // We don't want to display or count CSGs where one or more CSs cannot be asserted
        csgsAsArray = csgsAsArray.filter(csg => {
            let controlSetUris = csg.mandatoryControlSets.concat(csg.optionalControlSets);
            let controlSets = controlSetUris.map(csUri => {
                let cs = propControlSets[csUri];
                return cs;
            });
            let strategyNotAssertable = controlSets.map(cs => !(cs.assertable)).reduce(
                (previousValue, currentValue) => previousValue || currentValue,
                false
            )
            return !strategyNotAssertable;
        })

        // TODO: the correctly filtered list of CSGs that we now have is only used here for the count in the panel
        // We should be passing the filtered list into the ControlStrategiesPanel for display. As it is, similar filtering code is also implemented in there.
        // The necessary Object can be created using Object.fromEntries(csgsAsArray)

        //filter only those that are resolved (enabled) 
        let csgResolved = csgsAsArray.filter(csg => csg.enabled);
        
        let nCsgs = csgsAsArray.length; //total number of CGSs
        let nCsgResolved = csgResolved.length; //number of CGSs resolved

        // Don't display triggering CSGs panel if there aren't any
        if (triggering && nCsgs === 0) {
            return null;
        }
        
        let csgStyle = "danger"; //no CGSs are active
        let status = this.props.threatStatus;
        
        if ((status === "BLOCKED") || (status === "MITIGATED") || triggering) {
            if (nCsgResolved === nCsgs) {
                csgStyle = "success"; //all CGSs are active
            }
            else if (nCsgResolved > 0) {
                csgStyle = "warning"; //at least one CGS is active
            }
        }
        else if ((status === "ACCEPTED") && !triggering) {
            csgStyle = "warning";
        }
        
        // 7/2/2019: decided that colouring would simply be green here, if one or more CSGs are enabled (see #628)
        //let csgsPanelColor = getThreatColor(this.props.threat, this.props.levels["TrustworthinessLevel"]);
        let csgsPanelColor; //this just means the style is not used, so we use csgStyle instead below

        let key = triggering ? "triggering-csgs" : "csgs";
        
        return (
            <Panel defaultExpanded key={key} bsStyle={csgStyle} className="csg" style={{borderColor: csgsPanelColor}}>
                <Panel.Heading style={{backgroundColor: csgsPanelColor}}>
                    <Panel.Title toggle style={{color: csgsPanelColor ? "black" : "white"}}>
                        {this.renderHeaderNumbers(panelTitle, null, panelTooltip, "threat-ctrl-strat", nCsgResolved, nCsgs)}
                    </Panel.Title>
                </Panel.Heading>
                <Panel.Collapse>
                    <Panel.Body>
                        <ControlStrategiesPanel threat={this.props.threat}
                                                asset={this.props.asset}
                                                triggering={triggering}
                                                filteredCsgs={csgsAsArray}
                                                controlStrategies={this.props.controlStrategies}
                                                controlSets={propControlSets}
                                                levels={this.props.levels["TrustworthinessLevel"]}
                                                getControl={this.getControl}
                                                updateThreat={this.updateThreat}
                                                activateAcceptancePanel={this.activateAcceptancePanel}
                                                toggleAcceptThreat={this.toggleAcceptThreat}
                                                authz={this.props.authz}
                                                developerMode={this.props.developerMode}
                                                />
                    </Panel.Body>
                </Panel.Collapse>
            </Panel>
        )
    }

    activateAcceptancePanel(arg) {
        this.props.dispatch(activateAcceptancePanel(arg));
    }

    toggleAcceptThreat(arg) {
        var updatedThreat = this.props.threat;

        if (arg.hasOwnProperty("acceptThreat") && arg.hasOwnProperty("reason")) {
            updatedThreat = {
                ...updatedThreat,
                acceptanceJustification: arg.acceptThreat ? (arg.reason !== "" ? arg.reason : null) : null
            };
        }

        this.props.dispatch(toggleAcceptThreat(this.props.modelId, this.props.threat["id"], updatedThreat));
    }

    updateThreat(arg) {
        var updatedThreat = this.props.threat;

        //this is to enable an entire control strategy. I think 're not currently doing this
        if (arg.hasOwnProperty("controlStrategy")) {
            var updatedControlStrategiesA = updatedThreat["controlStrategies"]
                .filter((controlStrategy) => controlStrategy["id"] !== arg.controlStrategy["id"]);
            updatedControlStrategiesA.push({
                ...arg.controlStrategy,
                enabled: !arg.controlStrategy["enabled"]
            });
            updatedThreat = {
                ...updatedThreat,
                controlStrategies: updatedControlStrategiesA
            };
        }

        //this is to enable a single control in a control strategy
        if (arg.hasOwnProperty("control")) {
            //updating control strategy that this control belongs to
            if (!arg.control["controlStrategy"]) {
                console.warn("WARNING: cannot update control strategy: arg.control.controlStrategy is null");
                return;
            }

            //Here we still want to keep the currently selected asset, not change to the asset referred to in the updatedControl
            this.props.dispatch(updateControlOnAsset(this.props.modelId, this.props.asset["id"], arg.control));

            return;
        }

        if (arg.hasOwnProperty("threatLevel")) {
            updatedThreat = {
                ...updatedThreat,
                threatLevel: {
                    priorLikelihood: arg.threatLevel["priorLikelihood"],
                    potentialImpact: arg.threatLevel["potentialImpact"]
                }
            };
        }

        this.props.dispatch(updateThreat(this.props.modelId, this.props.asset["id"], updatedThreat["id"], updatedThreat));
    }

    getNodes() {
        //console.log("getNodes");
        let nodes = [];

        //console.log("this.props.threat: ", this.props.threat);
        if (this.props.threat) {
            let pattern = this.props.threat.pattern;
            if (! pattern) {
                alert("ERROR: threat has no pattern!");
            }
            else {
                nodes = this.props.threat.pattern["nodes"];
                nodes = nodes.map((node, index) => {
                    //console.log("node: ", node);
                    let asset = this.props.getAssetByUri(node["asset"]);
                    //console.log(node["roleLabel"]);
                    //console.log("asset:", asset);
                    //let role = node["roleLabel"];

                    let assetType = node["roleLabel"];
                    if (asset) assetType = asset["type"];

                    //console.log("role: ", role);
                    //console.log("assetType: ", assetType);
                    node["visible"] = ((!(assetType.endsWith("#LogicalPath") || assetType.endsWith("#LogicalSegment"))));
                    //console.log('node["visible"]: ', node["visible"]);
                    return node;
                })
            }
        }
        else {
            console.log("WARNING: this.props.threat not defined");
        }

        //console.log("Nodes:", nodes);

        return nodes;
    }

    getControl(id) {
        var control;
        if (this.props.controlSets) {
            control = this.props.controlSets.find((control) => control["id"] === id);
        }
        else {
            console.log("WARNING: no controlSets defined");
        }
        //console.log(control);

        if (control !== undefined) {
            return control;
        } else {
            return {
                label: "unknown",
                assetid: "",
                proposed: false
            };
        }
    }

    /*
    // N.B. Following is only required due to issue 291 - problem locating a secondary effect.
    // Once this is fixed, we can revert to using openMisbehaviourExplorer below
    openMisbehaviourExplorerForSecondaryEffect(secondaryEffect) {
        console.log("Displaying root causes for secondary effect: ");
        console.log(secondaryEffect);

        let misbehaviour;

        this.props.threats.map( (threat) => {
            let misbehaviours = threat["misbehaviours"];
            misbehaviours.map( (m) => {
                if ( (misbehaviour === undefined) && (m.misbehaviourLabel === secondaryEffect.misbehaviourLabel) && (m.assetLabel === secondaryEffect.assetLabel) ) {
                    misbehaviour = m;
                    console.log("Located misbehaviour:");
                    console.log(misbehaviour);
                }
            });
        });

        if (misbehaviour === undefined) {
            let msg = "Could not locate secondary effect: " + secondaryEffect.misbehaviourLabel + " at " + secondaryEffect.assetLabel
            console.log(msg);
            alert(msg);
            return;
        }

        this.openMisbehaviourExplorer(misbehaviour);
    }
    */

    openMisbehaviourExplorer(misbehaviour) {
        console.log("Displaying root causes for misbehaviour: ");
        console.log(misbehaviour);
        let m;
        if (misbehaviour.m !== undefined) {
            m = misbehaviour.m;
        }
        else {
            m = misbehaviour;
        }

        let updateRootCausesModel = true;
        this.props.dispatch(bringToFrontWindow("misbehaviourExplorer"));
        this.props.dispatch(getRootCauses(this.props.modelId, m, updateRootCausesModel));
    }

    getEntryPoints(threat) {
        //console.log("getEntryPoints for threat:", threat);

        let entryPointURIs = threat.entryPoints;
        //console.log("entryPointURIs:", entryPointURIs);

        if (entryPointURIs === undefined) {
            return [];
        }

        //console.log("this.props.twas:", this.props.twas);

        let twas = entryPointURIs.map((uri) => {
            return this.props.twas[uri];
        });

        //console.log("twas:", twas);
        return twas;
    }
}

ThreatAccordion.propTypes = {
    modelId: PropTypes.string,
    levels: PropTypes.object,
    asset: PropTypes.object,
    assets: PropTypes.array,
    relations: PropTypes.array,
    controlStrategies: PropTypes.object,
    controlSets: PropTypes.array, //all controlSets
    threat: PropTypes.object,
    threatStatus: PropTypes.string,
    triggeredStatus: PropTypes.string,
    threats: PropTypes.array,
    twas: PropTypes.object,
    selectedMisbehaviour: PropTypes.object,
    loadingCausesAndEffects: PropTypes.bool,
    getAssetByUri: PropTypes.func,
    isRelationDeletable: PropTypes.func,
    renderTrustworthinessAttributes: PropTypes.func,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
    developerMode: PropTypes.bool,
};

export default ThreatAccordion;
