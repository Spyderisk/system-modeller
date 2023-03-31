import React from "react";
import PropTypes from "prop-types";
import {Badge, OverlayTrigger, Tooltip, FormControl} from "react-bootstrap";
import * as Constants from "../../../../../../common/constants.js";
import {
    getControlStrategiesForControlSet,
    updateControlOnAsset, 
    updateControlCoverageOnAsset, 
    revertControlCoverageOnAsset, 
    openControlStrategyExplorer} from "../../../../../actions/ModellerActions";
import {bringToFrontWindow, closeWindow} from "../../../../../actions/ViewActions";
import {renderCoverageLevel} from "../../../../util/Levels";

class ControlSetPanel extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            controlSets: props.controlSets,
            updatingCoverage: this.getResetUpdatingCoverage(props)
        };

        this.updateControls = this.updateControls.bind(this);
        this.getResetUpdatingCoverage = this.getResetUpdatingCoverage.bind(this);
        this.coverageValueChanged = this.coverageValueChanged.bind(this);
        this.onClickRevertCoverageLevel = this.onClickRevertCoverageLevel.bind(this);
    }

    getResetUpdatingCoverage(props) {
        let updatingCoverage = {};

        props.controlSets.forEach((cs) => {
            updatingCoverage[cs.label] = false;
        });

        return updatingCoverage;
    }

    shouldComponentUpdate(nextProps, nextState) {
        let shouldUpdate = (this.props.controlSets !== nextProps.controlSets) || (this.state.updateControls !== nextState.updateControls) || (this.state.controlSets !== nextState.controlSets);
        //console.log("shouldUpdate:", shouldUpdate);
        return shouldUpdate;
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.selectedAsset.id !== nextProps.selectedAsset.id) {
        
            this.setState({
                controlSets: nextProps.controlSets,
                updatingCoverage: this.getResetUpdatingCoverage(nextProps)
            });
            
            return;
        }

        let cs = nextProps.controlSets; //KEM - just set the cs to the incoming objects
        let updatedCS = cs.filter((c) => c !== undefined);

        this.setState({
            controlSets: updatedCS,
            updatingCoverage: this.getResetUpdatingCoverage(nextProps)
        });
    }

    render() {
        let {controlSets} = this.state; //KEM - need to use the value in state, so the spinnerActive check works below

        var levelsMap = new Map();
        this.props.levels.forEach((level) => {
            levelsMap.set(level.uri, level);
        });

        return (
            <div className="asset-controls detail-list">
                <div className="container-fluid">
                    <div key={0} className="row head" style={{paddingLeft: 12}}>
                        <span className="col-xs-9">
                            Control
                        </span>
                        <span className="col-xs-3">
                            Coverage Level
                        </span>
                    </div>
                    {controlSets.length > 0 ? controlSets.sort((a, b) => (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0)
                        .map((controlSet, index) => {
                            if (controlSet !== undefined) {
                                // pending response from server
                                let spinnerActive = this.props.controlSets.find((cs) =>
                                    cs["id"] === controlSet["id"])["proposed"] !== controlSets.find((cs) =>
                                    cs["id"] === controlSet["id"])["proposed"];

                                //Flag to indicate if coverage level is being updated
                                let updatingCoverage = this.state.updatingCoverage[controlSet.label];

                                let errorFlag = controlSet["error"];
                                let errorMsg = "Request failed. Please try again";
                                let csgsMap = getControlStrategiesForControlSet(controlSet["uri"], this.props.threats, this.props.controlStrategies);
                                let csgs = Object.values(csgsMap);
                                let csgCount = csgs.length;
                                let context = {"selection": "controlSet", "controlSet": controlSet};

                                let errorFlagOverlay = "";
                                if (errorFlag) {
                                    let errorFlagOverlayProps = {
                                        delayShow: Constants.TOOLTIP_DELAY, placement: "right",
                                        overlay: <Tooltip id={`csp-control-set-${index + 1}-error-tt`}
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
                                        overlay: <Tooltip id={`csp-control-set-${index + 1}-error-spinner-tt`}
                                                          className={"tooltip-overlay"}>
                                            <span><i className="fa fa-spinner fa-pulse fa-lg fa-fw"/>Processing</span>
                                        </Tooltip>
                                    };
                                    errorFlagOverlay = <OverlayTrigger {...errorFlagOverlayProps}>
                                        <span>
                                            &nbsp;
                                            <span><i className="fa fa-spinner fa-pulse fa-lg fa-fw"/></span>
                                        </span>
                                    </OverlayTrigger>;
                                }

                                let coverageLevel = levelsMap.get(controlSet.coverageLevel);

                                //show revert coverage button only if the level has been asserted by the user
                                let showRevertButton = controlSet.coverageAsserted;

                                return (
                                    controlSet.assertable ?
                                        <div key={index} className="row detail-info control-info row-hover">
                                            <div class="col-xs-9">
                                                <span
                                                    className={this.props.authz.userEdit ? "traffic-lights" : "traffic-lights-view-only"}
                                                    disabled={!controlSet.assertable}
                                                    style={{ cursor: "pointer" }}
                                                >
                                                    <OverlayTrigger key={"red" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                                                        <Tooltip id={"ctrl-red-tooltip-" + index}
                                                            className={"tooltip-overlay"}>
                                                            {"Control not implemented"}
                                                        </Tooltip>
                                                    }>
                                                        <span className="fa-stack" onClick={() => this.props.authz.userEdit ? this.updateControls(controlSet, false) : undefined}>
                                                            <i className="bg fa fa-circle fa-stack-1x" />
                                                            <i className={!controlSet.proposed ? "red fa fa-stop-circle fa-stack-1x" : "red fa fa-circle fa-stack-1x"} />
                                                        </span>
                                                    </OverlayTrigger>
                                                    <OverlayTrigger key={"amber" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                                                        <Tooltip id={"ctrl-amber-tooltip-" + index}
                                                            className={"tooltip-overlay"}>
                                                            {"Control to be implemented"}
                                                        </Tooltip>
                                                    }>
                                                        <span className="fa-stack" onClick={() => this.props.authz.userEdit ? this.updateControls(controlSet, true, true) : undefined}>
                                                            <i className="bg fa fa-circle fa-stack-1x" />
                                                            <i className={controlSet.proposed && controlSet.workInProgress ? "amber fa fa-stop-circle fa-stack-1x" : "amber fa fa-circle fa-stack-1x"} />
                                                        </span>
                                                    </OverlayTrigger>
                                                    <OverlayTrigger key={"green" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                                                        <Tooltip id={"ctrl-green-tooltip-" + index}
                                                            className={"tooltip-overlay"}>
                                                            {"Control is implemented"}
                                                        </Tooltip>
                                                    }>
                                                        <span className="fa-stack" onClick={() => this.props.authz.userEdit ? this.updateControls(controlSet, true) : undefined}>
                                                            <i className="bg fa fa-circle fa-stack-1x" />
                                                            <i className={controlSet.proposed && !controlSet.workInProgress ? "green fa fa-stop-circle fa-stack-1x" : "green fa fa-circle fa-stack-1x"} />
                                                        </span>
                                                    </OverlayTrigger>
                                                </span>
                                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} delayHide={0}
                                                    placement="left" overlay={
                                                        <Tooltip id={`csp-control-set-${index}-tooltip1`}
                                                            className={"tooltip-overlay"}>
                                                            <span>This control is used in {csgCount == 1 ? "1 control" +
                                                                " strategy" : csgCount + " control strategies"}</span>
                                                        </Tooltip>}>
                                                        <Badge 
                                                            style={{ marginLeft: "6px", marginRight: "5px", cursor: (csgCount > 0) ? "pointer" : "default"}}
                                                            onClick={() => (csgCount > 0) ? this.openCsgExplorer(csgs, context): undefined}
                                                        >{csgCount}</Badge>
                                                </OverlayTrigger>
                                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} delayHide={0}
                                                    placement="left" overlay={
                                                        <Tooltip id={`csp-control-set-${index}-tooltip2`}
                                                            className={"tooltip-overlay"}>
                                                            {controlSet.description !== null ?
                                                                controlSet["description"]
                                                                :
                                                                "No description available"
                                                            }
                                                        </Tooltip>
                                                    }>
                                                    <span>
                                                        {controlSet["label"]}
                                                    </span>
                                                </OverlayTrigger>
                                                {errorFlagOverlay}
                                            </div>
                                            <div class="col-xs-3" style={{paddingLeft: 8}}>
                                                {renderCoverageLevel(controlSet.label, coverageLevel, this.props.levels, this.props.authz.userEdit, updatingCoverage, this.coverageValueChanged)}
                                                &nbsp;
                                                <span style={{cursor: "pointer", display: showRevertButton ? "inline-block" : "none"}} className="fa fa-undo undo-button" 
                                                    onClick={((e) => {
                                                        this.onClickRevertCoverageLevel(controlSet);
                                                    })}
                                                />
                                            </div>
                                        </div>
                                    :
                                        null
                                );
                            }
                        }) : <span>No controls found</span>}
                </div>
            </div>
        );
    }

    openCsgExplorer(csgs, context) {
        if (csgs.length > 0) {
            this.props.dispatch(openControlStrategyExplorer(csgs, context));
            this.props.dispatch(bringToFrontWindow("controlStrategyExplorer"));
        }
    }

    updateControls(controlSet, proposed = true, workInProgress = false) {
        if (controlSet.hasOwnProperty("label")) {
            let updatedControl = {
                ...controlSet,
                proposed: proposed,
                workInProgress: workInProgress
            };

            this.props.dispatch(updateControlOnAsset(this.props.modelId, updatedControl["assetId"], updatedControl));

            this.setState({
                controlSets: this.state.controlSets.map((cs) => {
                    if (cs["id"] !== controlSet["id"]) {
                        return cs;
                    } else {
                        return {...cs, proposed: proposed, workInProgress: workInProgress, error: false};
                    }
                })
            });
        } else {
            //console.log("Control's hasOwnProperty(label) is false");
        }
    }

    coverageValueChanged(e) {
        //console.log("coverageValueChanged");
        let controlLabel = e.target.id;
        let selectedLevelUri = e.target.value;
        //console.log("controlLabel: ", controlLabel);
        //console.log("selectedLevelUri: ", selectedLevelUri);
        let controlSet = this.state.controlSets.find(cs => cs.label === controlLabel);
        //console.log("Located CS: ", controlSet);

        if (controlSet) {
            let updatedCS = {
                ...controlSet,
                coverageLevel: selectedLevelUri
            };

            this.props.dispatch(updateControlCoverageOnAsset(this.props.modelId, updatedCS["assetId"], updatedCS));

            let updatingCoverage = {...this.state.updatingCoverage};
            updatingCoverage[controlLabel] = true;

            this.setState({
                controlSets: this.state.controlSets.map((cs) => {
                    if (cs["id"] !== controlSet["id"]) {
                        return cs;
                    } else {
                        return {...cs, coverageLevel: selectedLevelUri, error: false};
                    }
                }),
                updatingCoverage: updatingCoverage
            });
        }

    }

    onClickRevertCoverageLevel(controlSet) {
        //console.log("Reverting control set:", controlSet);

        if (controlSet) {
            let controlLabel = controlSet.label;

            let updatingCoverage = {...this.state.updatingCoverage};
            updatingCoverage[controlLabel] = true;

            this.setState({
                controlSets: this.state.controlSets.map((cs) => {
                    if (cs["id"] !== controlSet["id"]) {
                        return cs;
                    } else {
                        return {...cs, error: false};
                    }
                }),
                updatingCoverage: updatingCoverage
            });

            this.props.dispatch(revertControlCoverageOnAsset(this.props.modelId, controlSet["assetId"], controlSet));
        }

    }

}

ControlSetPanel.propTypes = {
    modelId: PropTypes.string,
    levels: PropTypes.array,
    controlSets: PropTypes.array,
    threats: PropTypes.array,
    controlStrategies: PropTypes.object,
    selectedAsset: PropTypes.object,
    dispatch: PropTypes.func,
    authz: PropTypes.object
};

export default ControlSetPanel;
