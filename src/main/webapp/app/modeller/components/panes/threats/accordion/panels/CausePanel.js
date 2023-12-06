import React from "react";
import PropTypes from 'prop-types';
import {FormControl, OverlayTrigger, Tooltip} from 'react-bootstrap';
import * as Constants from "../../../../../../common/constants.js";
import {updateMisbehaviourImpact, updateTwasOnAsset} from "../../../../../actions/ModellerActions";
import {getLevelColour, getRenderedLevelText} from "../../../../util/Levels";

class CausePanel extends React.Component {

    constructor(props) {
        super(props);

        this.renderMisbehaviours = this.renderMisbehaviours.bind(this);
        this.renderEntryPoints = this.renderEntryPoints.bind(this);
        this.twValueChanged = this.twValueChanged.bind(this);
        this.impactValueChanged = this.impactValueChanged.bind(this);
        this.getMisbehaviour = this.getMisbehaviour.bind(this);
        this.updateState = this.updateState.bind(this);
        this.openMisbehaviourExplorer = this.openMisbehaviourExplorer.bind(this);

        this.state = {
            tw_levels: [], // trustworthiness levels
            attributes: [],
            updating: {},
            twas: {},
            levels: [], // impact levels
            impact: {},
            likelihood: {},
            risk: {},
        };

        this.updateState(props, false);
    }

    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps: CausePanel: causes:", nextProps.causes);
        //console.log("componentWillReceiveProps:", this.props, nextProps);
        this.updateState(nextProps, true);
    }

    updateState(nextProps, mounted) {
        //console.log("updateState: CausePanel: causes:", nextProps.causes);
        //console.log("updateState: CausePanel: entryPoints:", nextProps.entryPoints);

        let tw_levels = Object.values(nextProps.levels["TrustworthinessLevel"]).sort(function(a, b) {
            return b.value - a.value;
        });
        
        let levels = Object.values(nextProps.levels["ImpactLevel"]).sort(function(a, b) {
            return b.value - a.value;
        });
        
        let twasArr = nextProps.entryPoints.sort(function (a, b) {
            return (a.attribute.label < b.attribute.label) ? -1 : (a.attribute.label > b.attribute.label) ? 1 : 0;
        });
        //console.log("twasArr", twasArr);

        let impact = {};
        let likelihood = {};
        let risk = {};
        let updating = {};
        
        let attributes = [];
        let twas = {};

        twasArr.map((twa, index) => {
            /*
            if (twa.asset !== asset.uri) {
                console.log("WARNING: twa not for this asset (ignoring)");
                return;
            };
            */
            let asset = this.props.getAssetByUri(twa.asset);
            //console.log("twa asset", asset);
            let assetLabel = asset ? asset.label : "unknown";

            //let attribute = twa.attribute;
            let label = twa.attribute.label + " at " + assetLabel;
            //console.log(twa);
            //attribute.label = label;
            
            let attribute = {...twa.attribute,
                label: label
            }
            
            if (! twa.assertedTWLevel) {
                console.log("WARNING: null assertedTWLevel on asset: " + label);
            }
            else {
                attributes.push(attribute);
                twas[label] = twa;
                updating[label] = false;
            }
        });

        let misbehaviours = nextProps.causes;

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
                tw_levels: tw_levels,
                attributes: attributes,
                updating: updating,
                twas: twas,
                levels: levels,
                impact: impact,
                likelihood: likelihood,
                risk: risk,
            });
        }
        else {
            this.state = {
                tw_levels: tw_levels,
                attributes: attributes,
                updating: updating,
                twas: twas,
                levels: levels,
                impact: impact,
                likelihood: likelihood,
                risk: risk,
            };
        }
    }

    render() {
        if ( this.props.loadingCausesAndEffects ) {
            return (<div className="container-fluid"><div className="row"><span className="col-md-12">Loading...</span></div></div>);
        }

        let secondary = this.props.secondaryThreat;
        let primary = !secondary;

        let causes = this.props.causes;
        let entryPoints = Object.values(this.props.entryPoints);

        if (! causes) {
            causes = [];
        }

        if (primary) {
            return (
                <div className="container-fluid">
                    {entryPoints.length>0 && <p>This primary threat's likelihood depends on the highest calculated value of these attributes:</p>}
                    {entryPoints.length>0 && this.renderEntryPoints()}
                    {causes.length>0 && <hr/>}
                    {causes.length>0 && <p>This threat's likelihood also depends on the following asset behaviours:</p>}
                    {causes.length>0 && this.renderMisbehaviours(causes)}
                </div>
            );
        }
        else if (secondary) {
            return (
                <div className="container-fluid">
                    {causes.length>0 && <p>This secondary threat's likelihood depends on the least likely of these consequences (which are caused by other threats):</p>}
                    {causes.length>0 && this.renderMisbehaviours(causes)}
                    {entryPoints.length>0 && <hr/>}
                    {entryPoints.length>0 && <p>This threat's likelihood also depends on the following asset attributes:</p>}
                    {entryPoints.length>0 && this.renderEntryPoints()}
                </div>
            );
        }
    }
    
    renderEntryPoints() {
        let self = this;
        
        let attributes = this.state.attributes;
        if (attributes === undefined)
            attributes = [];
        
        let tw_levels = this.state.tw_levels;
        
        //flag to show TWAS where visible = false
        //here, we set it to true, as we still want to display TWAS in the Cause panel
        let showInvisibleTwas = true;
        
        return this.props.renderTrustworthinessAttributes(attributes, tw_levels, self, showInvisibleTwas);
    }

    renderMisbehaviours(misbehaviours) {
        let self = this;

        let levels = this.state.levels;

        if ( jQuery.isEmptyObject(this.state.impact) ) {
            return;
        }

        return (
            <div>
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
                    
                    //console.log("render cause: selected = " + selected);

                    return (
                        <div key={index + 1} className={
                            `row misbehaviour-${active ? "active" : "inactive"} ` +
                            `detail-info ${selected ? "selected-row" : "row-hover"}`
                        }>
                            <span className="misbehaviour col-xs-3">
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                    trigger={["hover"]}
                                    overlay={
                                        <Tooltip id={`misbehaviour-${index + 1}-tooltip`}
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
                                    onChange={this.impactValueChanged}
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
                    )
                })}
            </div>
        )
    }

    twValueChanged(e) {
        //console.log("twValueChanged");
        let field = e.target.id;
        let selectedLevelUri = e.target.value;

        let selectedLevel = this.state.tw_levels.find((level) => level["uri"] === selectedLevelUri);

        let twas = this.state.twas;
        let updating = this.state.updating;

        //create new copy of twas object, with updated assertedTWLevel
        let twasForField = {...twas[field],
            assertedTWLevel: selectedLevel
        };

        let updatedTwas = {...twas};
        updatedTwas[field] = twasForField;

        //set updating flag for this field
        let updatedUpdating = {...updating};
        updatedUpdating[field] = true;

        this.setState({
            ...this.state,
            twas: updatedTwas,
            updating: updatedUpdating
        });
        
        let asset = this.props.getAssetByUri(twasForField.asset);
        //console.log("Calling updateTwasOnAsset:", twasForField, asset);

        this.props.dispatch(updateTwasOnAsset(this.props.modelId, asset.id, twasForField));
    }
    
    impactValueChanged(e) {
        //console.log("impactValueChanged");
        let misbehaviourId = e.target.id;
        let selectedLevelUri = e.target.value;
        //console.log(misbehaviourId, selectedLevelUri);

        //console.log("this.props", this.props);
        //console.log("this.state", this.state);
        
		//TODO: check which is correct below
        let selectedLevel = this.state.levels.find((level) => level["uri"] === selectedLevelUri);
        //let selectedLevel = this.props.levels.find((level) => level["uri"] === selectedLevelUri);
        //console.log(misbehaviourId, selectedLevel);

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
        let misbehaviour = this.props.causes.find((misbehaviour) => misbehaviour.id === misbehaviourId);
        return misbehaviour;
    }
    
    openMisbehaviourExplorer(misbehaviour) {
        this.props.openMisbehaviourExplorer(misbehaviour);
    }
}

CausePanel.propTypes = {
    modelId: PropTypes.string,
    levels: PropTypes.object,
    secondaryThreat: PropTypes.bool,
    causes: PropTypes.array,
    entryPoints: PropTypes.array,
    loadingCausesAndEffects: PropTypes.bool,
    selectedMisbehaviour: PropTypes.object,
    openMisbehaviourExplorer: PropTypes.func,
    renderTrustworthinessAttributes: PropTypes.func,
    getAssetByUri: PropTypes.func,
    dispatch: PropTypes.func
};

export default CausePanel;
