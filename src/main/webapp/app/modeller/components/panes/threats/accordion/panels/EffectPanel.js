import React from "react";
import PropTypes from 'prop-types';
import {FormControl, OverlayTrigger, Tooltip} from 'react-bootstrap';
import * as Constants from "../../../../../../common/constants.js";
import {updateMisbehaviourImpact} from "../../../../../actions/ModellerActions";
import {getLevelColour, getRenderedLevelText} from "../../../../util/Levels";

class EffectPanel extends React.Component {

    constructor(props) {
        super(props);

        this.renderMisbehaviours = this.renderMisbehaviours.bind(this);
        this.valueChanged = this.valueChanged.bind(this);
        this.getMisbehaviour = this.getMisbehaviour.bind(this);
        this.updateState = this.updateState.bind(this);

        this.state = {
            levels: [],
            impact: {},
            likelihood: {},
            risk: {},
            updating: {}
        };

        this.updateState(props, false);
    }

    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps: EffectPanel: effects:", nextProps.effects);
        this.updateState(nextProps, true);
    }

    updateState(nextProps, mounted) {
        //console.log("updateState: EffectPanel: effects:", nextProps.effects);

        let levels = Object.values(nextProps.levels).sort(function(a, b) {
            return b.value - a.value;
        });

        let impact = {};
        let likelihood = {};
        let risk = {};
        let updating = {};

        let misbehaviours = nextProps.effects;

        misbehaviours.map((misbehaviour, index) => {
            let misbehaviourId = misbehaviour["id"];
            impact[misbehaviourId] = misbehaviour.impactLevel;
            likelihood[misbehaviourId] = misbehaviour.likelihood;
            risk[misbehaviourId] = misbehaviour.riskLevel;
            updating[misbehaviourId] = false;
        });

        if (mounted) {
            this.setState({
                ...this.state,
                levels: levels,
                impact: impact,
                likelihood: likelihood,
                risk: risk,
                updating: updating,
            });
        }
        else {
            this.state = {
                levels: levels,
                impact: impact,
                likelihood: likelihood,
                risk: risk,
                updating: updating,
            };
        }
    }

    render() {
        if ( this.props.loadingCausesAndEffects ) {
            return (<div className="container-fluid"><div className="row"><span className="col-md-12">Loading...</span></div></div>);
        }

        var self = this;

        var misbehaviours = this.props.effects;
        if (! misbehaviours) {
            misbehaviours = [];
        }

        //only show if there are any
        if (misbehaviours.length>0) {
            return (
                <div className="container-fluid">
                    {this.renderMisbehaviours(misbehaviours)}
                </div>
            );
        } else {
            return (<div className="container-fluid"><div className="row"><span>none</span></div></div>);
        }
    }

    renderMisbehaviours(misbehaviours) {
        let self = this;

        let levels = this.state.levels;

        if ( jQuery.isEmptyObject(this.state.impact) ) {
            return;
        }

        return (
            <div>
                <p>
                    This threat directly causes the following consequences on other assets:
                </p>
                <div key={0} className='row head'>
                    <span className="col-xs-3">
                        Consequence
                    </span>
                    <span className="col-xs-3">
                        Asset
                    </span>
                    <span className="impact col-xs-1">
                        Direct Impact
                    </span>
                    <span className="likelihood col-xs-1">
                        Likelihood
                    </span>
                    <span className="risk col-xs-1">
                        Direct Risk
                    </span>
                </div>
                {misbehaviours.map((misbehaviour, index) => {
                    let misbehaviourId = misbehaviour["id"];
                    
                    //is misbehaviour selected?
                    let selected = this.props.selectedMisbehaviour.misbehaviour && this.props.selectedMisbehaviour.misbehaviour.id === misbehaviourId;

                    let impact = self.state.impact[misbehaviourId];
                    let likelihood = self.state.likelihood[misbehaviourId];
                    let risk = self.state.risk[misbehaviourId];
                    let updating = self.state.updating[misbehaviourId];
                    let active = (misbehaviour["active"] !== undefined) ? misbehaviour["active"] : true;

                    let likelihoodRender = getRenderedLevelText(levels, likelihood);
                    let riskRender = getRenderedLevelText(levels, risk);

                    return (
                        <div key={index + 1} className={
                            `row misbehaviour-item misbehaviour-${active ? "active" : "inactive"} ` +
                            `detail-info ${selected ? "selected-row" : "row-hover"}`
                        }>
                            <span className="misbehaviour col-xs-3">
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                    trigger={["hover"]}
                                    overlay={
                                        <Tooltip id={`effect-misbehaviour-${index + 1}-tooltip`}
                                            className={"tooltip-overlay"}>
                                            {misbehaviour.description ? misbehaviour.description :
                                                "View consequence" + (selected ? " (selected)" : "")}
                                        </Tooltip>
                                    }>
                                    <span className={"misbehaviour" + (selected ? "" : " clickable")}
                                        onClick={() => self.props.openMisbehaviourExplorer(misbehaviour)}>
                                        {misbehaviour["misbehaviourLabel"]}
                                    </span>
                                </OverlayTrigger>
                            </span>
                            <span className="col-xs-3">{misbehaviour["assetLabel"]}</span>
                            <span className="impact col-xs-1">
                                <FormControl
                                    disabled={!this.props.authz.userEdit}
                                    componentClass="select"
                                    className="impact-dropdown level"
                                    id={misbehaviourId}
                                    value={impact != null ? impact.uri : ""}
                                    style={{ backgroundColor: getLevelColour(levels, impact) }}
                                    onChange={this.valueChanged}
                                    ref="select-initial-tw">
                                    {levels.map((level, index) =>
                                        <option key={index + 1}
                                            value={level.uri}
                                            style={{ backgroundColor: getLevelColour(levels, level) }}>
                                            {level.label}
                                        </option>
                                    )};
                                </FormControl>
                                {updating ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw" /> : null}
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
        console.log("valueChanged");
        let misbehaviourId = e.target.id;
        let selectedLevelUri = e.target.value;
        console.log(misbehaviourId, selectedLevelUri);

        //console.log("this.props", this.props);
        //console.log("this.state", this.state);
        
        let selectedLevel = this.props.levels.find((level) => level["uri"] === selectedLevelUri);
        console.log(misbehaviourId, selectedLevel);

        let impact = this.state.impact;
        impact[misbehaviourId] = selectedLevel;

        let updating = this.state.updating;
        updating[misbehaviourId] = true;

        this.setState({
            ...this.state,
            impact: impact,
            updating: updating
        });

        let misbehaviour = this.getMisbehaviour(misbehaviourId);

        let updatedMisbehaviour = {
            id: misbehaviour["id"],
            uri: misbehaviour["uri"],
            impactLevel: {uri: selectedLevelUri}
        }

        this.props.dispatch(updateMisbehaviourImpact(this.props.modelId, updatedMisbehaviour));
    }

    getMisbehaviour(misbehaviourId) {
        let misbehaviour = this.props.effects.find((misbehaviour) => misbehaviour.id === misbehaviourId);
        return misbehaviour;
    }
}

EffectPanel.propTypes = {
    modelId: PropTypes.string,
    levels: PropTypes.array,
    effects: PropTypes.array,
    loadingCausesAndEffects: PropTypes.bool,
    selectedMisbehaviour: PropTypes.object,
    openMisbehaviourExplorer: PropTypes.func,
    dispatch: PropTypes.func
};

export default EffectPanel;
