import React from "react";
import PropTypes from 'prop-types';
import {updateMisbehaviourImpact, changeSelectedAsset, changeSelectedTwas} from "../../../../actions/ModellerActions";
import {bringToFrontWindow, closeWindow} from "../../../../actions/ViewActions";
import {getLevelColour, getRenderedLevelText} from "../../../util/Levels";
import {FormControl, OverlayTrigger, Tooltip} from "react-bootstrap";
import MisbehaviourAccordion from "../../misbehaviours/accordion/MisbehaviourAccordion";
import {Rnd} from "react-rnd";
import {connect} from "react-redux";
import * as Constants from "../../../../../common/constants.js";
import {openDocumentation} from "../../../../../common/documentation/documentation";

var _ = require('lodash');

class RootCausesEditor extends React.Component {

    constructor(props) {
        super(props);

        this.getAssetByUri = this.getAssetByUri.bind(this);
        this.valueChanged = this.valueChanged.bind(this);
        this.renderTwasLink = this.renderTwasLink.bind(this);
        this.renderAssetLink = this.renderAssetLink.bind(this);

        this.rnd = null;

        this.state = {
            impact: {},
            updating: false
        };
    }

    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;

        if ((!this.props.show) && (!nextProps.show)) {
            //console.log("RootCausesEditor.shouldComponentUpdate: false: (not visible)");
            return false;
        }

        if (nextProps.loading.model) {
            //console.log("RootCausesEditor.shouldComponentUpdate: false: (model loading)");
            return false;
        }

        if (this.props.isActive != nextProps.isActive) {
            // console.log("RootCausesEditor.shouldComponentUpdate: true: (isActive changed)");
            return true;
        }

        if (this.props.loadingCausesAndEffects != nextProps.loadingCausesAndEffects) {
            //console.log("RootCausesEditor.shouldComponentUpdate: false: (loadingCausesAndEffects changed)");
            return false;
        }

        // console.log("RootCausesEditor.shouldComponentUpdate: " + shouldComponentUpdate);
        return shouldComponentUpdate;
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            ...this.state,
            impact: nextProps.selectedMisbehaviour.misbehaviour.impactLevel,
            updating: false
        });
    }

    renderTwasLink(twas) {
        return <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
                    trigger={["hover"]}
                    overlay={
                        <Tooltip id="threaten-asset-tooltip"
                                className={"tooltip-overlay"}>
                            {"Select trustworthiness attribute"}
                        </Tooltip>
                    }>
                    <span className="highlighted"
                        style={{cursor: "pointer", fontSize: "inherit"}}
                        onClick={() => this.props.dispatch(
                            changeSelectedTwas(twas))
                        }>
                        {twas.attribute.label}
                    </span>
                </OverlayTrigger>
    }

    renderAssetLink(asset) {
        return <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
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
                        {asset.label}
                    </span>
                </OverlayTrigger>
    }

    render() {
        //console.log("RootCausesEditor: render this.props:", this.props);
        const {model, attackPaths, selectedMisbehaviour, hoverThreat, dispatch, loading, ...modalProps} = this.props;

        if (!selectedMisbehaviour || _.isEmpty(selectedMisbehaviour.misbehaviour)) return null;
        const misbehaviour = selectedMisbehaviour.misbehaviour;
        //console.log("RootCausesEditor: misbehaviour: ", misbehaviour);

        //Get TWAS associated with this MS (if any)
        let twas = this.props.getTwasForMisbehaviourSet(misbehaviour.uri);

        //get related asset info
        let asset;
        let assetLabel = "";
        if (misbehaviour) {
            asset = this.getAssetByUri(misbehaviour.asset);
            if (asset) {
                assetLabel = asset["label"];
            }
        } else {
            return null;
        }

        let impact = this.state.impact;
        let likelihood = misbehaviour["likelihood"];
        let risk = misbehaviour["riskLevel"];

        let impactLevel = impact != null ? impact.label : "";

        let likelihoodRender = getRenderedLevelText(this.props.model.levels.Likelihood, likelihood);
        let riskRender = getRenderedLevelText(this.props.model.levels.RiskLevel, risk);

        let levels = this.props.model.levels ? this.props.model.levels["ImpactLevel"].sort(function(a, b) {
            return b.value - a.value;
        }) : [];

        let updating = this.state.updating;

        //Get misbehaviour label
        let misbLabel = misbehaviour.misbehaviourLabel;

        //Append TWAS label (if MS has associated TWAS)
        let consequenceLabelHeading = <span>
                {misbLabel}
                {twas ? <span> (or low {this.renderTwasLink(twas)})</span> : null}
            </span>

        let consequenceDesc = misbehaviour.description !== undefined ? misbehaviour.description : "";
        let consequenceDesc2 = twas ? <p style={{marginTop: "10px"}}>This also causes a reduction in the trustworthiness level for attribute {this.renderTwasLink(twas)} at the same asset.</p> : null;
        let assetLabelHeading = this.renderAssetLink(asset);

        return (
            <Rnd ref={ c => {this.rnd = c;} }
                 bounds={ '#view-boundary' }
                 default={{
                   x: window.outerWidth * 0.2,
                     y: (100 / window.innerHeight) * window.devicePixelRatio,
                   width: 560,
                   height: 600,
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
                 disableDragging={this.props.threatFiltersActive["direct-causes"] || this.props.threatFiltersActive["root-causes"]} //disable window dragging if threat filter has focus
                 cancel={".content, .text-primary, strong, span"} //disable dragging on misbehaviour label (hence allowing text selection)
                 // onDragStart={(e) => {
                 //     let elName = $(e.target)[0].localName;
                     // console.log("onDragStart: element: " + elName);

                     /*
                     if (elName === "input") {
                         return false;
                     }
                     */


                 // }}
                 onDrag={(e) => {
                     if (e.stopPropagation) e.stopPropagation();
                     if (e.preventDefault) e.preventDefault();
                     e.cancelBubble = true;
                     e.returnValue = false;
                 }}
                 onDragStop={(e) => {
                     /*
                     let el = $(e.target)[0];
                     let elName = el.localName;
                     let elID = el.id
                     console.log("onDragStop: element: " + elName + ": " + elID);
                     //console.log(el);
                     if (elName === "input") {
                         //console.log("onDragStop: setting focus for input textbox");
                         //el.focus();
                         console.log("onDragStop: calling activateThreatFilter");
                         this.props.dispatch(activateThreatFilter(elID, true));
                     }
                     */
                 }}
                 className={!this.props.show ? "hidden" : null}>
                <div className="misbehaviour-explorer">

                    <div className="header" onMouseDown={() => {
                        this.props.dispatch(bringToFrontWindow("misbehaviourExplorer"))
                    }}>
                        <h1>
                            <div className={"doc-help-explorer"}>
                                <div>
                                    {"Consequence Explorer"}
                                </div>
                            </div>
                        </h1>
                        <span classID="rel-delete"
                            className="menu-close fa fa-times"
                            onClick={() => {
                                this.props.dispatch(closeWindow("misbehaviourExplorer"));
                                this.props.onHide();
                            }}>
                        </span>
                        <span className="menu-close fa fa-question" onClick={e => openDocumentation(e, "redirect/misbehaviour-explorer")} />
                    </div>

                    <div className="content">
                        <div className="desc">
                            <div className="descriptor">
                                <h4>
                                    {consequenceLabelHeading}
                                    {" at "}
                                    {assetLabelHeading}
                                </h4>
                                {this.props.developerMode && <p>{misbehaviour.uri}</p>}
                                <p>{consequenceDesc}</p>
                                <div key={0} className='row head'>
                                    <span className="col-xs-2 impact">
                                        Direct Impact
                                    </span>
                                    <span className="col-xs-2 likelihood">
                                        Likelihood
                                    </span>
                                    <span className="col-xs-2 risk">
                                        Direct Risk
                                    </span>
                                    <span className="col-xs-6"></span>
                                </div>
                                <div className='row detail-info'>
                                    <span className="col-xs-2 impact">
                                        <FormControl
                                            disabled={!this.props.authz.userEdit}
                                            componentClass="select"
                                            className="impact-dropdown level"
                                            id={misbehaviour.misbehaviourLabel}
                                            value={impact ? impact.uri : ""}
                                            style={{ backgroundColor: getLevelColour(levels, impact) }}
                                            onChange={this.valueChanged}
                                            ref="select-initial-tw">
                                            {levels.map((level, index) =>
                                                <option key={index + 1}
                                                    value={level.uri}
                                                    style={{ backgroundColor: getLevelColour(levels, level) }}>
                                                    {level.label}
                                                </option>
                                            )}
                                        </FormControl>
                                        {updating ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw" /> : null}
                                    </span>
                                    <span className="likelihood col-xs-2">
                                        {likelihoodRender}
                                    </span>
                                    <span className="risk col-xs-2">
                                        {riskRender}
                                    </span>
                                    <span className="col-xs-6"></span>
                                </div>
                                {consequenceDesc2}
                            </div>
                        </div>

                        <MisbehaviourAccordion dispatch={dispatch}
                                               selectedMisbehaviour={selectedMisbehaviour}
                                               attackPaths={attackPaths}
                                               selectedThreat={this.props.selectedThreat}
                                               twas={twas}
                                               asset={asset}
                                               model={model}
                                               threatFiltersActive={this.props.threatFiltersActive}
                                               //directCauses={directCauses}
                                               //rootCauses={rootCauses}
                                               hoverThreat={hoverThreat}
                                               renderTwasLink={this.renderTwasLink}
                                               renderAssetLink={this.renderAssetLink}
                                               loading={loading}
                        />
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
    
    valueChanged(e) {
        let misbehaviourId = e.target.id;
        let selectedLevelUri = e.target.value;
        //console.log(misbehaviourId, selectedLevelUri);

        let selectedLevel = this.props.model.levels["ImpactLevel"].find((level) => level["uri"] === selectedLevelUri);
        //console.log(misbehaviourId, selectedLevel);

        let misbehaviour = this.props.selectedMisbehaviour.misbehaviour;

        let updatedMisbehaviour = {
            id: misbehaviour["id"],
            uri: misbehaviour["uri"],
            impactLevel: {uri: selectedLevelUri}
        };

        this.setState({
            ...this.state,
            impact: selectedLevel,
            updating: true
        });

        this.props.dispatch(updateMisbehaviourImpact(this.props.model["id"], updatedMisbehaviour));
    }
}

RootCausesEditor.propTypes = {
    isActive: PropTypes.bool, // is in front of other panels
    threatFiltersActive: PropTypes.object,
    model: PropTypes.object,
    getTwasForMisbehaviourSet: PropTypes.func,
    selectedMisbehaviour: PropTypes.object,
    attackPaths: PropTypes.object,
    selectedThreat: PropTypes.object,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    loadingCausesAndEffects: PropTypes.bool,
    hoverThreat: PropTypes.func,
    developerMode: PropTypes.bool,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["misbehaviourExplorer"]
    }
};

export default connect(mapStateToProps)(RootCausesEditor);
