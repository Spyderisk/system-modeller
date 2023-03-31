import React from "react";
import PropTypes from 'prop-types';
import {getRootCauses, updateMisbehaviourImpact, revertMisbehaviourImpact} from "../../../../../actions/ModellerActions";
import {FormControl, OverlayTrigger, Tooltip} from 'react-bootstrap';
import {getLevelColour, getRenderedLevelText} from "../../../../util/Levels";
import * as Constants from "../../../../../../common/constants.js";
import {bringToFrontWindow} from "../../../../../actions/ViewActions";

class MisbehavioursPanel extends React.Component {

    constructor(props) {
        super(props);

        this.getUpdatedState = this.getUpdatedState.bind(this);
        this.renderMisbehaviours = this.renderMisbehaviours.bind(this);
        this.valueChanged = this.valueChanged.bind(this);
        this.onClickRevertImpactLevel = this.onClickRevertImpactLevel.bind(this);
        this.openMisbehaviourExplorer = this.openMisbehaviourExplorer.bind(this);

        this.state = this.getUpdatedState(props);;
    }
    
    getUpdatedState(props) {
        //console.log("getUpdatedState: misbehaviours:", props.misbehaviours);
        let levels = Object.values(props.levels).sort(function(a, b) {
            return b.value - a.value;
        });

        var misbehaviourLabels = Object.keys(props.misbehaviours);

        let impact = {};
        let likelihood = {};
        let risk = {};
        let updating = {};

        misbehaviourLabels.map((misbehaviourLabel, index) => {
            let misbehaviour = props.misbehaviours[misbehaviourLabel];
            impact[misbehaviourLabel] = misbehaviour.impactLevel;
            likelihood[misbehaviourLabel] = misbehaviour.likelihood;
            //risk[misbehaviourLabel] = this.calcRisk(impact[misbehaviourLabel], likelihood[misbehaviourLabel]); //For now (demo), we calculate locally
            risk[misbehaviourLabel] = misbehaviour.riskLevel;
            updating[misbehaviourLabel] = false;
        });

        let updatedState = {
            ...this.state,
            levels: levels,
            impact: impact,
            likelihood: likelihood,
            risk: risk,
            updating: updating,
        };
        
        //console.log("updatedState:", updatedState);
        return updatedState;
    }

    shouldComponentUpdate(nextProps) {
        //return this.props.misbehaviours !== nextProps.misbehaviours; //KEM: this doesn't pick up state change, e.g. when impact level is changed
        return true;
    }

    //N.B. this method is deprecated, so code will need to be refactored to use preferred methods!
    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps: ", nextProps);
        let updatedState = this.getUpdatedState(nextProps);
        this.setState(updatedState);
    }
    

    render() {
        //console.log("MisbehavioursPanel render");
        if (this.props.selectedAsset["loadingControlsAndThreats"]) {
            return (<div className="container-fluid"><div className="row"><span className="col-md-12">Loading...</span></div></div>);
        }

        //console.log("MisbehavioursPanel: misbehaviours:", this.props.misbehaviours);
        var misbehaviourLabels = Object.keys(this.props.misbehaviours).sort();
        //console.log("misbehaviourLabels:", misbehaviourLabels);
        
        return (
            <div className="misbehaviours detail-list">
                <div className="container-fluid">
                    {misbehaviourLabels.length > 0 ? this.renderMisbehaviours(misbehaviourLabels)
                    : <span>No consequences found</span>}
                </div>
            </div>
        );
    }

    renderMisbehaviours(misbehaviourLabels) {
        //console.log("renderMisbehaviours");
        let self = this;

        let levels = this.state.levels;
        if ( jQuery.isEmptyObject(this.state.impact) ) {
            return;
        }
        
        //console.log("selected misbehaviour:", this.props.selectedMisbehaviour);
        
        return (
            <div>
                <div key={0} className={'row head'}>
                    <span className="col-xs-4 misbehaviour">
                        Consequence
                    </span>
                    <span className="col-xs-2 impact">
                        Direct Impact
                    </span>
                    <span className="col-xs-1 likelihood">
                        Likelihood
                    </span>
                    <span className="col-xs-1 risk">
                        Direct Risk
                    </span>
                </div>
                {misbehaviourLabels.map((misbehaviourLabel, index) => {
                    let misbehavioursGroup = this.props.misbehaviours[misbehaviourLabel];
                    //console.log("misbehavioursGroup:", misbehavioursGroup);
                    
                    //is misbehaviour visible?
                    let visible = misbehavioursGroup["visible"];

                    if (visible !== undefined && !visible) {
                        //console.log("Hiding misbehaviour: ", misbehavioursGroup["misbehaviourLabel"]);
                        return;
                    }
                        
                    let misbehaviourId = misbehavioursGroup["id"];

                    let selectedMisbehav = this.props.selectedMisbehaviour.misbehaviour;
                    //is misbehaviour selected?
                    let selected = selectedMisbehav && selectedMisbehav.id === misbehaviourId;

                    let impact = self.state.impact[misbehaviourLabel];
                    let likelihood = self.state.likelihood[misbehaviourLabel];
                    let risk = self.state.risk[misbehaviourLabel];
                    let updating = self.state.updating[misbehaviourLabel];

                    let likelihoodRender = getRenderedLevelText(levels, likelihood);
                    let riskRender = getRenderedLevelText(levels, risk);

                    //show revert impact level button only if the level has been asserted by the user
                    let showRevertButton = misbehavioursGroup.impactLevelAsserted;

                    return (
                        <div key={index + 1} className={
                            `row detail-info ${
                                selected === true ? "selected-row" : "row-hover"
                            }`
                        }>
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                                trigger={["hover"]}
                                                overlay={
                                                    <Tooltip id={`misbehaviour-${index + 1}-tooltip`}
                                                             className={"tooltip-overlay"}>
                                                        { misbehavioursGroup.description ? misbehavioursGroup.description :
                                                            "View consequences" + (selected ? " (selected)" : "") }
                                                    </Tooltip>
                                                }>
                                <span className={"col-xs-4 misbehaviour" + (selected ? "" : " clickable")}>
                                    <span
                                          onClick={() => {
                                              this.openMisbehaviourExplorer(misbehavioursGroup);
                                              this.props.dispatch(bringToFrontWindow("misbehaviourExplorer"));
                                          }}>
                                            {misbehaviourLabel}
                                    </span>
                                </span>
                                </OverlayTrigger>
                                <span className="col-xs-2 impact">
                                    <FormControl 
                                        disabled={!this.props.authz.userEdit}
                                        componentClass="select"
                                        className="impact-dropdown level"
                                        id={misbehaviourLabel}
                                        value={impact != null ? impact.uri : ""}
                                        style={{backgroundColor: getLevelColour(levels, impact)}}
                                        onChange={this.valueChanged}
                                        ref="select-initial-tw">
                                        {levels.map((level, index) =>
                                            <option key={index+1}
                                                value={level.uri}
                                                style={{backgroundColor: getLevelColour(levels, level)}}>
                                                {level.label}
                                            </option>
                                        )};
                                    </FormControl>
                                    {updating ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw" /> : null}
                                    &nbsp;
                                    <span style={{cursor: "pointer", display: showRevertButton ? "inline-block" : "none"}} className="fa fa-undo undo-button" 
                                        onClick={((e) => {
                                            this.onClickRevertImpactLevel(misbehaviourLabel);
                                        })}
                                    />
                                </span>
                                <span className="likelihood col-xs-1">
                                    {likelihoodRender}
                                </span>
                                <span className="risk col-xs-1">
                                    {riskRender}
                                </span>
                        </div>
                    );
                })}
            </div>
        )
    }

    valueChanged(e) {
        let misbehaviourId = e.target.id;
        let selectedLevelUri = e.target.value;

        let selectedLevel = this.props.levels.find((level) => level["uri"] === selectedLevelUri);

        let impact = this.state.impact;
        impact[misbehaviourId] = selectedLevel;

        let updating = this.state.updating;
        updating[misbehaviourId] = true;

        this.setState({
            ...this.state,
            impact: impact,
            updating: updating
        });

        let misbehaviour = this.props.misbehaviours[misbehaviourId];

        let updatedMisbehaviour = {
            id: misbehaviour["id"],
            uri: misbehaviour["uri"],
            impactLevel: {uri: selectedLevelUri}
        }

        this.props.dispatch(updateMisbehaviourImpact(this.props.model["id"], updatedMisbehaviour));
    }

    onClickRevertImpactLevel(misbehaviourId) {
        //console.log("onClickRevertImpactLevel:", misbehaviourId);
        if (misbehaviourId) {
            //set updating flag for this impact label
            let updatedUpdating = {...this.state.updating};
            updatedUpdating[misbehaviourId] = true;

            this.setState({
                ...this.state,
                updating: updatedUpdating
            });

            let misbehaviour = this.props.misbehaviours[misbehaviourId];

            let ms = {
                id: misbehaviour["id"],
                uri: misbehaviour["uri"]
            }

            this.props.dispatch(revertMisbehaviourImpact(this.props.model["id"], ms));
        }
    }

    openMisbehaviourExplorer(misbehaviour) {
        //console.log("Displaying root causes for misbehaviour: ");
        //console.log(misbehaviour);

        //this.props.dispatch(toggleThreatEditor(false, ""));

        /*
        this.setState({
            ...this.state,
            rootCausesModal: {
                misbehaviour: misbehaviour,
                show: true
            }
        });
        */

        // Now we get root causes for a specified misbehaviour (rather than a threat)
        //console.log("misbehaviourUri:");
        //console.log(misbehaviour.misbehaviours[0].id);
        let misbehaviourUri = misbehaviour.uri; //in theory all misbehaviours in group have the same id
        //console.log(misbehaviourUri);
        let updateRootCausesModel = true;
        //console.log("dispatch getRootCauses...");
        //this.props.dispatch(getRootCauses(this.props.model["id"], misbehaviourId, updateRootCausesModel));
        //this.props.dispatch(getRootCauses(this.props.model["id"], misbehaviour.misbehaviours[0], updateRootCausesModel));
        this.props.dispatch(getRootCauses(this.props.model["id"], misbehaviour));
    }

    /*
    closeRootCausesDisplay() {
        this.setState({
            ...this.state,
            rootCausesModal: {
                show: false
            }
        });
    }
    */

}

MisbehavioursPanel.propTypes = {
    model: PropTypes.object,
    levels: PropTypes.array,
    selectedAsset: PropTypes.object,
    misbehaviours: PropTypes.object,
    selectedMisbehaviour: PropTypes.object,
    loading: PropTypes.object,
    dispatch: PropTypes.func
};

export default MisbehavioursPanel;
