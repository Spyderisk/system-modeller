import React from "react";
import PropTypes from "prop-types";
import {OverlayTrigger, Panel, Tooltip, Button, ButtonToolbar} from "react-bootstrap";
import ThreatsPanel from "../../details/accordion/panels/ThreatsPanel";
import * as Constants from "../../../../../common/constants.js";
import {getShortestPathThreats} from "../../../../actions/ModellerActions";

class MisbehaviourAccordion extends React.Component {

    constructor(props) {
        super(props);

        this.renderHeader = this.renderHeader.bind(this);
        this.renderHeaderNumbers = this.renderHeaderNumbers.bind(this);
        this.getMisbehaviourThreats = this.getMisbehaviourThreats.bind(this);
        this.getIndirectThreats = this.getIndirectThreats.bind(this);
        this.getRootThreats = this.getRootThreats.bind(this);
        this.getTreatedThreats = this.getTreatedThreats.bind(this);
        this.getDirectEffectThreats = this.getDirectEffectThreats.bind(this);
        this.getThreatById = this.getThreatById.bind(this);
        this.getThreatByUri = this.getThreatByUri.bind(this);
        this.updateThreats = this.updateThreats.bind(this);
        this.getRootCausesDesc = this.getRootCausesDesc.bind(this);
        this.getDirectEffectsDesc = this.getDirectEffectsDesc.bind(this);

        this.state = {
            expanded: {
                threats: true,
                causes: true
            }
        }
    }

    // Uncomment/modify if props or state needs to be checked
    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;
        return shouldComponentUpdate;
    }

    getRootCausesDesc(nThreats) {
        if (nThreats > 0) {
            return <p>These are the highest likelihood root causes of this consequence:</p>
        }
        else {
            return (
                <div>
                    <p>There are no root causes because none of the threats that could directly cause this consqeuence have high enough likelihood.</p>
                    {(this.props.twas && this.props.twas.assertedTWLevel.label !== "Safe") &&
                        <p>However, threats may still be caused by low {this.props.renderTwasLink(this.props.twas)} at {this.props.renderAssetLink(this.props.asset)}, based on its assumed TW level.</p>
                    }
                </div>
            )
        }
    }

    getDirectEffectsDesc() {
        let misbehaviour = this.props.selectedMisbehaviour.misbehaviour
        let misbLabel = misbehaviour.misbehaviourLabel;
        return <p>
            Any secondary threats listed below are caused directly by {misbLabel} at {this.props.renderAssetLink(this.props.asset)}. 
            {this.props.twas ?
                <span> Any primary threats are caused by low {this.props.renderTwasLink(this.props.twas)} at {this.props.renderAssetLink(this.props.asset)}, based on its calculated trustworthiness level which 
            may be reduced below the assumed trustworthiness level by {misbLabel} at {this.props.renderAssetLink(this.props.asset)}.</span> : null}
        </p>
    }

    render() {
        let {expanded} = this.state;
        let misbehaviourThreats = this.getMisbehaviourThreats();
        let treatedMisbehaviourThreats = this.getTreatedThreats(misbehaviourThreats);
        let indirectThreats = this.getIndirectThreats();
        let treatedIndirectThreats = this.getTreatedThreats(indirectThreats);
        let directEffectThreats = this.getDirectEffectThreats();
        let treatedDirectEffectThreats = this.getTreatedThreats(directEffectThreats);

        let selectedAsset = {
            loadingCausesAndEffects: false,
            loadingControlsAndThreats: false
        }

        //TODO: move the style to a stylesheet
        let allCausesIcon = <span style={{float:"right"}}>∣<i className="fa fa-arrow-right"/><i className="fa fa-arrow-right"/> <i className="fa fa-crosshairs"/></span>;
        let directCausesIcon = <span style={{float:"right"}}><i className="fa fa-arrow-right"/> <i className="fa fa-crosshairs"/></span>;
        let directlyCausesIcon = <span style={{float:"right"}}><i className="fa fa-crosshairs"/> <i className="fa fa-arrow-right"/></span>;

        // populate attack path threats
        let attackPathThreats = this.props.selectedMisbehaviour.attackPathThreats.map((threatUri) => {
            let threat = this.props.model.threats.find((threat) => threat["uri"] === threatUri);
            return threat;
        });

        let loadingAttackPath = this.props.selectedMisbehaviour.loadingAttackPath;

        return (
            <div className="panel-group accordion">
                <Panel bsStyle="primary" defaultExpanded>
                   <Panel.Heading>
                       <Panel.Title toggle>
                             {this.renderHeaderNumbers("Root causes", allCausesIcon, "Most likely root causes of this consequence.", "all-causes", treatedIndirectThreats.length, indirectThreats.length)}
                       </Panel.Title>
                   </Panel.Heading>
                   <Panel.Collapse>
                       <Panel.Body>
                            {this.props.selectedMisbehaviour.loadingRootCauses ?
                                <div className="container-fluid"><div className="row"><span className="col-md-12">Loading...</span></div></div>
                                    :
                                <ThreatsPanel dispatch={this.props.dispatch}
                                        name={"root-causes"}
                                        context={this.props.selectedMisbehaviour.misbehaviour.uri}
                                        getDescription={this.getRootCausesDesc}
                                        model={this.props.model}
                                        selectedAsset={null}
                                        selectedThreat={this.props.selectedThreat}
                                        displayRootThreats={true}
                                        hoverThreat={this.props.hoverThreat}
                                        getIndirectThreats={this.getIndirectThreats}
                                        threatFiltersActive={this.props.threatFiltersActive}
                                        loading={this.props.loading}/>
                            }
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel bsStyle="primary" defaultExpanded>
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderHeaderNumbers("Direct causes", directCausesIcon, "All the threats that can directly cause this consequence, immediately preceeding it on an attack path.", "direct-causes", treatedMisbehaviourThreats.length, misbehaviourThreats.length)}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <p>This consequence could potentially be caused by any of the following threats. The consequence's likelihood matches the highest likelihood threat(s), which are considered to be its actual causes.</p>
                            {this.props.selectedMisbehaviour.loadingRootCauses ?
                                <div className="container-fluid"><div className="row"><span className="col-md-12">Loading...</span></div></div>
                                    :

                                <ThreatsPanel dispatch={this.props.dispatch}
                                              name={"direct-causes"}
                                              context={this.props.selectedMisbehaviour.misbehaviour.uri}
                                              model={this.props.model}
                                              selectedAsset={null}
                                              selectedThreat={this.props.selectedThreat}
                                              displayRootThreats={true}
                                              hoverThreat={this.props.hoverThreat}
                                              getDirectThreats={this.getMisbehaviourThreats}
                                              threatFiltersActive={this.props.threatFiltersActive}
                                              loading={this.props.loading}/>
                            }
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel bsStyle="primary" defaultExpanded>
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderHeaderNumbers("Secondary threats caused or primary threats enabled", directlyCausesIcon, "Threats which are all directly caused or enabled by this consequence, next on the attack path.", "directly-causes", treatedDirectEffectThreats.length, directEffectThreats.length)}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            {this.getDirectEffectsDesc()}
                            {this.props.selectedMisbehaviour.loadingRootCauses ?
                                <div className="container-fluid"><div className="row"><span className="col-md-12">Loading...</span></div></div>
                                    :

                                <ThreatsPanel dispatch={this.props.dispatch}
                                              name={"direct-effects"}
                                              context={this.props.selectedMisbehaviour.misbehaviour.uri}
                                              model={this.props.model}
                                              selectedAsset={null}
                                              selectedThreat={this.props.selectedThreat}
                                              displayRootThreats={true}
                                              hoverThreat={this.props.hoverThreat}
                                              getDirectThreats={this.getDirectEffectThreats}
                                              threatFiltersActive={this.props.threatFiltersActive}
                                              loading={this.props.loading}/>
                            }
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderHeader("Attack path threats", null, "Threats identified in the attack path of this consequence.", "attack-path-threats")}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <ButtonToolbar>
                                <Button className="btn btn-primary btn-xs"
                                        onClick={() => {this.props.dispatch(
                                                               getShortestPathThreats(this.props.model.id, this.props.selectedMisbehaviour.misbehaviour.uri));}}>Calculate Attack Path</Button>
                                {loadingAttackPath ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw"/> : null}
                            </ButtonToolbar>
                            {!loadingAttackPath && <ThreatsPanel dispatch={this.props.dispatch}
                                          name={"attack-path-threats"}
                                          context={this.props.selectedMisbehaviour.misbehaviour.uri}
                                          model={this.props.model}
                                          threats={attackPathThreats}
                                          selectedAsset={null}
                                          selectedThreat={null}
                                          displayRootThreats={false}
                                          hoverThreat={this.props.hoverThreat}
                                          getDirectThreats={null}
                                          threatFiltersActive={null}
                            />}
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
            </div>
        );
    }

    renderHeaderNumbers(title, icon="", title_tt, tt_id, n1, n2) {
        let numbers = "(" + n1 + "/" + n2 + ")";
        let numbers_tt = n1 + " out of " + n2 + " addressed";
        return this.renderHeader(title, icon, title_tt, tt_id, numbers, numbers_tt);
    }

    renderHeader(title, icon, title_tt, tt_id, numbers=undefined, numbers_tt=undefined) {
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

    getMisbehaviourThreats() {
        /*
        let misbehavioursGroup = this.props.misbehaviour;
        console.log("getMisbehaviourThreats: misbehavioursGroup:");
        console.log(misbehavioursGroup);
        let misbehaviours = misbehavioursGroup.misbehaviours;
        console.log("getMisbehaviourThreats: misbehaviours:");
        console.log(misbehaviours);

        if (misbehaviours === undefined) {
            return [];
        }

        //console.log(misbehavioursGroup.misbehaviourLabel);
        //console.log(misbehavioursGroup.assetLabel);
        console.log("Misbehaviour threats:");

        let misbehaviourThreats = misbehaviours.map((misbehaviour) => {
            return misbehaviour.threat;
        });

        console.log(misbehaviourThreats);
        //return misbehaviourThreats;
        return this.updateThreats(misbehaviourThreats);
        */
        //let misbehaviourThreats = this.props.selectedAsset["directCauses"];
        //let misbehaviourThreats = this.props.directCauses;
        let misbehaviourThreats = this.props.selectedMisbehaviour.misbehaviour.directCauses;
        //console.log("MisbehaviourAccordion: getMisbehaviourThreats: misbehaviourThreats = ", misbehaviourThreats);
        return this.updateThreats(misbehaviourThreats); //ensure that resolved state, etc is updated
    }

    getDirectEffectThreats() {
        let directEffects = this.props.selectedMisbehaviour.misbehaviour.directEffects;
        let misbehaviourThreats = misbehaviourThreats = (directEffects !== undefined) ? directEffects : [];
        return this.updateThreats(misbehaviourThreats); //ensure that resolved state, etc is updated
    }

    getIndirectThreats() {
        let misbehaviourThreats = this.props.selectedMisbehaviour.misbehaviour.indirectCauses;
        return this.updateThreats(misbehaviourThreats); //ensure that resolved state, etc is updated
    }

    getRootThreats() {
        let rootCauses = this.props.selectedMisbehaviour.misbehaviour.rootCauses;
        return this.updateThreats(rootCauses);
    }

    updateThreats(threatUris) {
        if (threatUris === undefined) return [];

        return threatUris.map((threatUri) => {
            let modelThreat;

            if (threatUri) {
                modelThreat = this.getThreatByUri(threatUri);
                if (modelThreat === undefined) {
                    console.log("WARNING: threat does not exist: " + threatUri);
                    //return threat;
                }

                return modelThreat;
            }
        });
    }

    getTreatedThreats(threats) {
        //get all resolved threats
        var treatedThreats = [];

        if (threats != undefined) {
            treatedThreats = threats.filter(function (t) {
                if (t && (t.resolved == true || t.acceptanceJustification !== null)) {
                    return t;
                } else {
                    return;
                }
            });
        }

        return treatedThreats;
    }

    getThreatById(threatId) {
        //console.log("getThreatById: " + threatId);
        let threat = this.props.model.threats.find((threat) => {
            return (threat.id === threatId);
        });
        //console.log("threat: ");
        //console.log(threat);
        return threat;
    }

    getThreatByUri(threatUri) {
        //console.log("getThreatByUri: " + threatUri);
        let threat = this.props.model.threats.find((threat) => {
            return (threat.uri === threatUri);
        });
        //console.log("threat: ");
        //console.log(threat);
        return threat;
    }
}

MisbehaviourAccordion.propTypes = {
    model: PropTypes.object,
    selectedMisbehaviour: PropTypes.object,
    selectedThreat: PropTypes.object,
    twas: PropTypes.object,
    asset: PropTypes.object,
    //selectedAsset: PropTypes.object,
    //directCauses: PropTypes.array,
    //rootCauses: PropTypes.array,
    loading: PropTypes.object,
    threatFiltersActive: PropTypes.object,
    hoverThreat: PropTypes.func,
    renderTwasLink: PropTypes.func,
    renderAssetLink: PropTypes.func,
    dispatch: PropTypes.func
};

export default MisbehaviourAccordion;
