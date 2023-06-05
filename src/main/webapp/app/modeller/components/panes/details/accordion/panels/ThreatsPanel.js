import React from "react";
import PropTypes from 'prop-types';
import {FormGroup, Label, OverlayTrigger, Tooltip, Checkbox, InputGroup, Form, FormControl} from "react-bootstrap";
import PagedPanel from "../../../../../../common/components/pagedpanel/PagedPanel"
import * as Constants from "../../../../../../common/constants.js";
import {nameWrap} from "../../../../util/wordWrap"
import {activateThreatFilter, suppressCanvasRefresh, toggleThreatEditor} from "../../../../../actions/ModellerActions";
import {bringToFrontWindow} from "../../../../../actions/ViewActions";
import {getRenderedLevelText, getThreatColor} from "../../../../util/Levels";
import {getThreatStatus} from "../../../../util/ThreatUtils";
import {connect} from "react-redux";

var _ = require('lodash');


class ThreatsPanel extends React.Component {

    constructor(props) {
        super(props);

        this.getDefaultState = this.getDefaultState.bind(this);
        this.renderThreats = this.renderThreats.bind(this);
        this.openThreatEditor = this.openThreatEditor.bind(this);
        this.getAssetByUri = this.getAssetByUri.bind(this);
        this.sortByCol = this.sortByCol.bind(this);
        this.getFilterID = this.getFilterID.bind(this);

        this.state = this.getDefaultState(props);
    }

    getDefaultState(props) {
        return {
            search: "",
            selectedAssetOnly: true,
            showOnlyUntreatedThreats: false,
            showOnlyRootCauseThreats: false,
            showOnlyThreatsWithCSG: false,
            showOnlyThreatsWithInactiveCSG: false,
            showPrimaryThreats: true,
            showSecondaryThreats: true,
            showUntriggeredThreats: false, //only used to filter compliance threats for now
            showFilters: false,
            sort: {
                col: (props.name === "direct-effects") ? "likelihood" : (props.name === "attack-path-threats") ? "distance" : "risk",
                dir: "desc"
            }
        }
    }

    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps");

        if (this.props.selectedAsset !== null && this.props.selectedAsset.id !== nextProps.selectedAsset.id) {
            this.setState({...this.state, search: "", selectedAssetOnly: true});
        }
    }

    shouldComponentUpdate(nextProps) {
        //console.log("ThreatsPanel.shouldComponentUpdate: " + this.props.name);
        //console.log("this.props:");
        //console.log(this.props);
        //console.log("nextProps: ");
        //console.log(nextProps);

        let shouldComponentUpdate = !(this.props.selectedAsset !== null && (this.props.selectedAsset.id === "") && (nextProps.selectedAsset.id === ""));
        //console.log("ThreatsPanel.shouldComponentUpdate: " + this.props.name + ": "+ shouldComponentUpdate);
        return shouldComponentUpdate;
    }

    componentDidUpdate() {
        //console.log("this.props.threatFiltersActive", this.props.threatFiltersActive);
        if (this.props.threatFiltersActive) {
            //console.log("ThreatsPanel.componentDidUpdate: " + this.props.name + ": threatFiltersActive = ");
            //console.log(this.props.threatFiltersActive);
            if (this.props.threatFiltersActive[this.props.name]) {
                //let id = "threats-filter-" + this.props.name;
                let id = this.getFilterID();
                //console.log("ThreatsPanel.componentDidUpdate: " + this.props.name + ": setting focus for threat filter: " + id);
                let filter = $("#" + id);
                if (filter) filter.focus();
            }
        }
    }

    getFilterID() {
        let id = "threats-filter-" + this.props.name;
        if (this.props.name === "compliance-threats") id += "-" + this.props.index; //used to distinguish compliance threat panels
        //console.log("getFilterID: ", id);
        return id;
    }

    render() {
        let threats = [];

        if (this.props.displayRootThreats) {
            if (this.props.getIndirectThreats) {
                if (this.props.complianceSet) {
                    threats = this.props.getIndirectThreats(this.props.complianceSet);
                }
                else {
                    threats = this.props.getIndirectThreats();
                }
            }
            else if (this.props.getDirectThreats) {
                if (this.props.complianceSet) {
                    threats = this.props.getDirectThreats(this.props.complianceSet);
                }
                else {
                    threats = this.props.getDirectThreats();
                }
            }
            else if (this.props.getRootThreats || this.props.complianceSet) {
                if (this.props.complianceSet) {
                    let complianceThreats = this.props.complianceSet["complianceThreats"];

                    //Show untriggered threats if flag is false
                    if (this.state.showUntriggeredThreats) {
                        threats = complianceThreats; //show ALL threats (including untriggered ones)
                    }
                    else {
                        //N.B. for now, there is no need to filter out untriggered threats here, as these are already
                        //filtered by default in the ComplianceAccordion
                        //threats = complianceThreats.filter((t) => !t.untriggered); //include if NOT untriggered
                        threats = complianceThreats;
                    }
                    //console.log("Returned compliance threats (excluding untriggered):", threats);
                }
                else {
                    threats = this.props.getRootThreats();
                }
            }
        }
        else {
            threats = this.props.threats ? this.props.threats.filter((threat) => this.props.selectedAsset === null || threat["threatensAssets"] === this.props.selectedAsset.uri || !this.state.selectedAssetOnly) : [];
        }

        //console.log("unfiltered threats: ", threats.length);

        let inputID = this.getFilterID();

        let searchText = this.state.search.trim().toLowerCase();

        let filteredThreats = threats.filter((threat) => {
            if (!threat) return false;
            let threatLabel = threat["label"].toLowerCase();
            let threatDescNameIdx = threat["description"].indexOf(": ");
            if (threatDescNameIdx !== -1) {
                return threat["description"].slice(0, threatDescNameIdx)
                    .toLowerCase().indexOf(searchText) > -1;
            } else return threatLabel.indexOf(searchText) > -1;
        });

        //Include only untreated threats (if flag is set)
        if (this.state.showOnlyUntreatedThreats) {
            filteredThreats = filteredThreats.filter((t) => (t.resolved === false && t.acceptanceJustification === null));
        }

        //Include only root causes (if flag is set)
        if (this.state.showOnlyRootCauseThreats) {
            filteredThreats = filteredThreats.filter((t) => t.rootCause);
        }

        //Include only threats with a control strategy (if flag is set)
        if (this.state.showOnlyThreatsWithCSG) {
            filteredThreats = filteredThreats.filter((t) => Object.keys(t.controlStrategies).length > 0);
        }

        //Include only threats with one or more inactive control strategy (if flag is set)
        if (this.state.showOnlyThreatsWithInactiveCSG) {
            filteredThreats = filteredThreats.filter((t) => Object.values(t.controlStrategies).some((csg) => {
                //console.log(csg);
                return !csg.enabled;
            }));
        }

        //Include primary/secondary threats, according to flags
        filteredThreats = filteredThreats.filter((t) => (this.state.showPrimaryThreats && !t.secondaryThreat) ||
                                                        (this.state.showSecondaryThreats && t.secondaryThreat)
        );

        //console.log("filtered threats: ", filteredThreats.length);

        let showFiltersDiv;
        if (threats.length > 0 && !this.state.showFilters) {
            showFiltersDiv = <div>
                {this.props.getDescription && this.props.getDescription(threats.length)}
                <div>
                    <a className="filter show-filter"
                        onClick={() => {
                            this.setState({...this.state, showFilters: true})
                        }}>Show filters
                    </a>
                    <a className="filter reset-filter"
                        onClick={() => {
                            this.sortByCol("")
                        }}>Reset sort
                    </a>
                </div>
            </div>
        } else if (threats.length > 0 && this.state.showFilters) {
            showFiltersDiv = <div>
                {this.props.getDescription && this.props.getDescription(threats.length)}
                <div>
                    <a className="filter show-filter"
                        onClick={() => {
                            this.setState({...this.state, showFilters: false})
                        }}>Hide filters
                    </a>
                    <a className="filter reset-filters"
                        onClick={() => {
                            //reset all filters, but keep filters open
                            this.setState({...this.getDefaultState(this.props), showFilters: true});
                        }}>Reset filters
                    </a>
                    <a className="filter reset-filter"
                        onClick={() => {
                            this.sortByCol("")
                        }}>Reset sort
                    </a>
                </div>
            </div>
        } else {
            if (this.props.getDescription) {
                showFiltersDiv = this.props.getDescription(threats.length);
            }
            else {
                showFiltersDiv = <span>No threats found</span>
            }
        }

        return <div className="threats detail-list">
            <div className="container-fluid">
                <Form>
                    <FormGroup>
                        {showFiltersDiv}
                    </FormGroup>
                    {this.state.showFilters && <FormGroup>
                        <InputGroup>
                            <InputGroup.Addon><i className="fa fa-lg fa-filter"/></InputGroup.Addon>
                            <FormControl
                                type="text"
                                id={inputID}
                                value={this.state.search}
                                placeholder="Filter threats by name"
                                onMouseOver={(e) => {
                                    //console.log("Threat filter: onMouseOver " + this.props.name);
                                    //onMouseOver is used only in misbehaviour explorer to disable window dragging, so not required here
                                    if (this.props.name == "direct-causes" || this.props.name == "root-causes" || this.props.name == "compliance-threats") {
                                        let el = $(e.target)[0];
                                        let elID = el.id;
                                        //console.log("Threat filter: onMouseOver " + elID);
                                        this.props.dispatch(activateThreatFilter(elID, true));
                                    }
                                }}
                                onMouseOut={(e) => {
                                    //console.log("Threat filter: onMouseOut " + this.props.name);
                                    //onMouseOut is used only in misbehaviour explorer to re-enable window dragging, so not required here
                                    if (this.props.name == "direct-causes" || this.props.name == "root-causes" || this.props.name == "compliance-threats") {
                                        let el = $(e.target)[0];
                                        let elID = el.id;
                                        //console.log("Threat filter: onMouseOut " + elID);
                                        this.props.dispatch(activateThreatFilter(elID, false));
                                    }
                                }}
                                onFocus={(e) => {
                                    //console.log("Threat filter: onFocus " + this.props.name);
                                    //this.props.dispatch(activateThreatFilter(true));
                                }}
                                onChange={(e) => {
                                    let searchText = e.nativeEvent.target.value;
                                    //console.log("searchText: " + searchText);
                                    this.setState({...this.state, search: searchText})
                                }}
                                // need to prevent the Form being submitted when Return is pressed
                                onKeyPress={(e) => { e.key === 'Enter' && e.preventDefault(); }}
                            />
                        </InputGroup>
                    </FormGroup>}
                    {this.state.showFilters && <FormGroup>
                        {!this.props.displayRootThreats && this.props.selectedAsset !== null && <Checkbox
                            checked={this.state.selectedAssetOnly}
                            onChange={() => {
                                this.setState({...this.state, selectedAssetOnly: !this.state.selectedAssetOnly})
                            }}>
                            Relating to selected asset
                        </Checkbox>}

                        <Checkbox
                            checked={this.state.showOnlyUntreatedThreats}
                            onChange={() => {
                                this.setState({...this.state, showOnlyUntreatedThreats: !this.state.showOnlyUntreatedThreats})
                            }}>
                            Only untreated threats
                        </Checkbox>

                        {(!this.props.complianceSet && (this.props.name !== "root-causes")) && <Checkbox
                            checked={this.state.showOnlyRootCauseThreats}
                            onChange={() => {
                                this.setState({...this.state, showOnlyRootCauseThreats: !this.state.showOnlyRootCauseThreats})
                            }}>
                            Only root causes
                        </Checkbox>}

                        <Checkbox
                            checked={this.state.showOnlyThreatsWithCSG}
                            onChange={() => {
                                this.setState({...this.state, showOnlyThreatsWithCSG: !this.state.showOnlyThreatsWithCSG})
                            }}>
                            Only threats with a control strategy
                        </Checkbox>

                        <Checkbox
                            checked={this.state.showOnlyThreatsWithInactiveCSG}
                            onChange={() => {
                                this.setState({...this.state, showOnlyThreatsWithInactiveCSG: !this.state.showOnlyThreatsWithInactiveCSG})
                            }}>
                            Only threats with an inactive control strategy
                        </Checkbox>

                        {(!this.props.complianceSet && (this.props.name !== "root-causes")) && <Checkbox
                            checked={this.state.showPrimaryThreats}
                            onChange={() => {
                                this.setState({...this.state, showPrimaryThreats: !this.state.showPrimaryThreats})
                            }}>
                            Include primary threats
                        </Checkbox>}

                        {(!this.props.complianceSet && (this.props.name !== "root-causes")) && <Checkbox
                            checked={this.state.showSecondaryThreats}
                            onChange={() => {
                                this.setState({...this.state, showSecondaryThreats: !this.state.showSecondaryThreats})
                            }}>
                            Include secondary threats
                        </Checkbox>}
                    </FormGroup>}
                </Form>

                {threats.length > 0 && this.renderThreats(filteredThreats)}

            </div>
        </div>
    }

    renderThreats(threats) {
        let sortCol = this.state.sort.col;
        let sortDir = this.state.sort.dir;
        let sortDirIcon = sortDir === "asc" ? "fa fa-caret-up" : "fa fa-caret-down";
        //console.log(threats);
        let sortedThreats = threats;
        //console.log("this.props.name:", this.props.name);
        let compliance = this.props.name === "compliance-threats" ||
                this.props.name === "modelling-errors"; // are we displaying compliance or modelling threats?
        //console.log("compliance:", compliance);

        let attackPath = this.props.name === "attack-path-threats";

        let threatHeader = this.props.complianceSet ? (this.props.name === "modelling-errors" ? "Modelling Error" : "Compliance Threat") : "Threat";

        //sort threats by currently selected column and direction
        //console.log("sortCol: ", sortCol, " sortDir: ", sortDir);

        if (sortCol === "threat") {
            if (sortDir === "asc") {
                //sort alphabetically (asc)
                sortedThreats.sort(function (a, b) {
                    let threatDescNameIdx = a["description"].indexOf(": ");
                    let labelA = threatDescNameIdx !== -1 ?
                        a["description"].slice(0, threatDescNameIdx) : a["label"];

                    threatDescNameIdx = b["description"].indexOf(": ");
                    let labelB = threatDescNameIdx !== -1 ?
                        b["description"].slice(0, threatDescNameIdx) : b["label"];

                    return (labelA < labelB) ? -1 : (labelA > labelB) ? 1 : 0;
                })
            }
            else {
                //sort alphabetically (desc)
                sortedThreats.sort(function (a, b) {
                    let threatDescNameIdx = a["description"].indexOf(": ");
                    let labelA = threatDescNameIdx !== -1 ?
                        a["description"].slice(0, threatDescNameIdx) : a["label"];

                    threatDescNameIdx = b["description"].indexOf(": ");
                    let labelB = threatDescNameIdx !== -1 ?
                        b["description"].slice(0, threatDescNameIdx) : b["label"];

                    return (labelA > labelB) ? -1 : (labelA < labelB) ? 1 : 0;
                })
            }
        }
        else if (sortCol === "likelihood") {
            if (sortDir === "asc") {
                //sort by likelihood (low to high), then alphabetically
                sortedThreats.sort(function (a, b) {
                    let a_val = a.likelihood ? a.likelihood.value : -1;
                    let b_val = b.likelihood ? b.likelihood.value : -1;
                    return (a_val < b_val) ? -1 : (a_val > b_val) ? 1 : (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0;
                })
            }
            else {
                //sort by likelihood (high to low), then alphabetically
                sortedThreats.sort(function (a, b) {
                    let a_val = a.likelihood ? a.likelihood.value : -1;
                    let b_val = b.likelihood ? b.likelihood.value : -1;
                    let result = (a_val > b_val) ? -1 : (a_val < b_val) ? 1 : (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0;
                    return result;
                })
            }
        }
        else if (sortCol === "distance") {
            if (sortDir == "asc") {
                //sort by distance (low to high), then alphabetically
                sortedThreats.sort(function (a,b) {
                    let a_val = a.distance ? a.distance : -1;
                    let b_val = b.distance ? b.distance : -1;
                    return (a_val < b_val) ? -1 : (a_val > b_val) ? 1 : (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0;
                })
            }
            else {
                //sort by distance (high to low), then alphabetically
                sortedThreats.sort(function (a, b) {
                    let a_val = a.distance ? a.distance : -1;
                    let b_val = b.distance ? b.distance : -1;
                    let result = (a_val > b_val) ? -1 : (a_val < b_val) ? 1 : (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0;
                    return result;
                })
            }

        }
        else if (sortCol === "risk") {
            if (sortDir === "asc") {
                //sort by risk level (low to high), then alphabetically
                sortedThreats.sort(function (a, b) {
                    let a_val = a.riskLevel ? a.riskLevel.value : -1;
                    let b_val = b.riskLevel ? b.riskLevel.value : -1;
                    return (a_val < b_val) ? -1 : (a_val > b_val) ? 1 : (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0;
                })
            }
            else {
                //sort by risk level (high to low), then alphabetically
                sortedThreats.sort(function (a, b) {
                    let a_val = a.riskLevel ? a.riskLevel.value : -1;
                    let b_val = b.riskLevel ? b.riskLevel.value : -1;
                    return (a_val > b_val) ? -1 : (a_val < b_val) ? 1 : (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0;
                })
            }
        } else if (sortCol === "asset") {
            if (sortDir === "asc") {
                //sort assets alphabetically (asc)
                sortedThreats.sort((a, b) => {
                    let asset_a = this.getAssetByUri(a.threatensAssets);
                    let assetName_a = '';
                    if (asset_a) assetName_a = asset_a["label"];
                    else {
                        asset_a = a.pattern.nodes.find(x => a["threatensAssets"] === x.asset);
                        if (asset_a) {
                            assetName_a = asset_a["assetLabel"];
                        }
                    }

                    let asset_b = this.getAssetByUri(b.threatensAssets);
                    let assetName_b = '';
                    if (asset_b) assetName_b = asset_b["label"];
                    else {
                        asset_b = b.pattern.nodes.find(x => b["threatensAssets"] === x.asset);
                        if (asset_b) {
                            assetName_b = asset_b["assetLabel"];
                        }
                    }

                    return (assetName_a < assetName_b) ? -1 : (assetName_a > assetName_b) ? 1 : 0;
                })
            } else {
                //sort assets alphabetically (desc)
                sortedThreats.sort((a, b) => {
                    let asset_a = this.getAssetByUri(a.threatensAssets);
                    let assetName_a = '';
                    if (asset_a) assetName_a = asset_a["label"];
                    else {
                        asset_a = a.pattern.nodes.find(x => a["threatensAssets"] === x.asset);
                        if (asset_a) {
                            assetName_a = asset_a["assetLabel"];
                        }
                    }

                    let asset_b = this.getAssetByUri(b.threatensAssets);
                    let assetName_b = '';
                    if (asset_b) assetName_b = asset_b["label"];
                    else {
                        asset_b = b.pattern.nodes.find(x => b["threatensAssets"] === x.asset);
                        if (asset_b) {
                            assetName_b = asset_b["assetLabel"];
                        }
                    }

                    return (assetName_a > assetName_b) ? -1 : (assetName_a < assetName_b) ? 1 : 0;
                })
            }
        }

        //This is to be filled with the rendered threat HTML blocks
        let threatsRender = [];
        let displayOneAsset = this.props.selectedAsset !== null && this.state.selectedAssetOnly;

        sortedThreats.map((threat, index) => {
            let asset;
            let assetLabel = "";
            if (threat) {
                asset = this.getAssetByUri(threat.threatensAssets);
                if (asset) assetLabel = asset["label"];
                else {
                    if (threat.pattern) {
                        asset = threat.pattern.nodes.find(a => threat["threatensAssets"] === a.asset);
                        if (asset) {
                            assetLabel = asset["assetLabel"];
                        }
                    }
                    else {
                        console.warn("Threat has missing pattern: ", threat);
                    }
                }
            } else {
                return null;
            }

            let threatDescNameIdx = threat["description"].indexOf(": ");
            let threatName = threat["label"];
            //console.log(threatName);

            if (threatDescNameIdx !== -1) {
                // use name from description if found
                threatName = threat["description"].slice(0, threatDescNameIdx);
                // add 4 chars of threat ID for uniqueness
                threatName = `${threatName} (${threat["id"].slice(0, 4)})`
            }

            let overlayAssetName = assetLabel;
            if (compliance) {
                assetLabel = nameWrap(assetLabel, 660)
            } else {
                assetLabel = nameWrap(assetLabel, this.props.rightSidePanelWidth)
            }

            const threatUri = threat["uri"];

            // if we could'nt find a label, create one based on the URI
            if (!threatName) {
                if (threatUri && threatUri.indexOf('#') > -1) {
                    threatName = threatUri.split('#')[1];
                } else {
                    threatName = threatUri;
                }
            }

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

            //console.log("status: ", status);
            //console.log("triggeredStatus: ", triggeredStatus);

            let threatColorAndBE = ((status === "BLOCKED") || (status === "MITIGATED")) ?
            getThreatColor(threat, this.props.model.controlStrategies ,this.props.model.levels["TrustworthinessLevel"], true) : undefined;
            //console.log("threatColorAndBE: ", threatColorAndBE);

            //status: UNMANAGED, ACCEPTED, MITIGATED, BLOCKED
            let statusText = "";
            let symbol;

            /* Uncomment to add triggered state, e.g. for debugging
            if (triggeredStatus === "UNTRIGGERED") {
                statusText = "Untriggered / ";
            }
            else if (triggeredStatus === "TRIGGERED") {
                statusText = "Triggered / ";
            }
            */

            if (status === "BLOCKED") {
                statusText += "Managed (" + threatColorAndBE.be.label + ")";
                symbol = <span className="fa fa-check threat-icon" style={{backgroundColor: threatColorAndBE.color}}/>;
            } else if (status === "MITIGATED") {
                statusText += "Managed (" + threatColorAndBE.be.label + ")";
                symbol = <span className="fa fa-minus threat-icon" style={{backgroundColor: threatColorAndBE.color}}/>;
            } else if (status === "ACCEPTED") {
                statusText += "Accepted";
                // TODO: put these colors and style in a stylesheet
                symbol = <span className="fa fa-thumbs-up threat-icon" style={{backgroundColor: "red", color: "white"}}/>
            } else {
                statusText += "Unmanaged";
                symbol = <span className="fa fa-exclamation-triangle threat-icon" style={{backgroundColor: "red", color: "white"}}/>;
            }

            if (triggeredStatus === "UNTRIGGERED") {
                statusText = "Untriggered side effect";
                symbol = <span className="fa fa-check threat-icon"/>;
            }

            let root_cause = threat.rootCause;
            let secondary = threat.secondaryThreat;
            let primary = !secondary;
            var resolved = false;

            if (threat["resolved"] !== undefined) {
                resolved = threat["resolved"];
            }
            else if (threat["active"] !== undefined) {
                resolved = !threat["active"];
            }
            else if (threat['acceptanceJustification'] !== undefined) {
                resolved = (threat['acceptanceJustification'] !== null);
            }

            //console.log(threatLabel + " resolved = " + resolved);

            //is threat selected?
            let selected = this.props.selectedThreat && this.props.selectedThreat.id === threat.id;

            //let impact = threat["impactLevel"];
            let likelihood = threat["likelihood"];
            let risk = threat["riskLevel"];

            let distance = threat.distance;

            let likelihoodRender = getRenderedLevelText(this.props.model.levels.Likelihood, likelihood);
            let riskRender = getRenderedLevelText(this.props.model.levels.RiskLevel, risk);

            threatsRender.push(
                <div key={index + 1} className={
                    `row detail-info ${
                        selected === true ? "selected-row" : "row-hover"
                    }`
                    }
                    onMouseEnter={() => this.props.hoverThreat(true, threat)}
                    onMouseLeave={() => this.props.hoverThreat(false, threat)}
                >
                    <span className="col-xs-1" style={{minWidth: "44px"}}>
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                            trigger={["hover"]}
                            overlay={
                                <Tooltip id={`threats-stat-${index + 1}-tooltip`}
                                    className="tooltip-overlay"
                                >
                                    {statusText}
                                </Tooltip>
                            }
                        >
                            {symbol}
                        </OverlayTrigger>
                        {primary && root_cause ? <OverlayTrigger
                            delayShow={Constants.TOOLTIP_DELAY}
                            placement="left"
                            trigger={["hover"]}
                            overlay={
                                <Tooltip id={`threats-root-${index + 1}-tooltip`}
                                    className="tooltip-overlay"
                                >
                                    Primary Threat (Root Cause)
                                </Tooltip>
                            }
                        >
                            <span className="threat-icon">1</span>
                        </OverlayTrigger>
                        : null }
                        {primary && !root_cause ? <OverlayTrigger
                            delayShow={Constants.TOOLTIP_DELAY}
                            placement="left"
                            trigger={["hover"]}
                            overlay={
                                <Tooltip id={`threats-stat-${index + 1}-tooltip`}
                                    className="tooltip-overlay"
                                >
                                    Primary Threat
                                </Tooltip>
                            }
                        >
                            <span className="threat-icon">1</span>
                        </OverlayTrigger>
                        : null }
                        {secondary ? <OverlayTrigger
                            delayShow={Constants.TOOLTIP_DELAY}
                            placement="left"
                            trigger={["hover"]}
                            overlay={
                                <Tooltip id={`threats-secondary-${index + 1}-tooltip`}
                                    className="tooltip-overlay"
                                >
                                    Secondary Threat
                                </Tooltip>
                            }
                        >
                            <span className="threat-icon">2</span>
                        </OverlayTrigger>
                        : null }
                        {!compliance && !root_cause && !primary && !secondary ? <span
                            className="threat-icon" style={{color: "#7e7e7e"}}>0</span>
                        : null}
                    </span>
                    <OverlayTrigger
                        delayShow={Constants.TOOLTIP_DELAY}
                        placement="left"
                        trigger={["hover"]}
                        overlay={
                            <Tooltip id={`threats-view-${index + 1}-tooltip`}
                                        className={"tooltip-overlay"}>
                                { threat.description ? threat.description :
                                    "View threat" + (selected ? " (selected)" : "") }
                            </Tooltip>
                        }>
                        <span className={"threat-label " + (selected ? "" : "clickable ") + (
                            compliance ? "col-xs-7" : (!displayOneAsset ? "col-xs-6" : "col-xs-9")
                        )}
                            onClick={() => this.openThreatEditor(threat["id"])}
                        >
                            {threatName}
                        </span>
                    </OverlayTrigger>
                    {!displayOneAsset && !attackPath ?
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                            trigger={["hover"]}
                            overlay={
                                <Tooltip id={`threats-asset-${index + 1}-tooltip`}
                                            className={"tooltip-overlay"}>
                                    {overlayAssetName}
                                </Tooltip>
                            }>
                            <span className={(compliance ? "col-xs-4" : "col-xs-3") + " threat-label"}
                                style={{minWidth: "50px", cursor: "default"}}
                            >
                                {assetLabel}
                            </span>
                        </OverlayTrigger> : null}
                    {!compliance && attackPath &&
                    <span className="col-xs-1 distance">
                        {distance}
                    </span>
                    }
                    {!compliance &&
                    <span className="col-xs-1 likelihood">
                        {likelihoodRender}
                    </span>
                    }
                    {!compliance && !attackPath &&
                    <span className="col-xs-1 risk">
                        {riskRender}
                    </span>
                    }
                </div>
            );
        });

        return (
            <div>
                <div key={0} className='row head-sortable'>
                    <span className="col-xs-1" style={{minWidth: "44px"}}/>
                    {/* {!compliance &&
                    <span className="col-xs-1"/>
                    } */}
                    <span className={(
                        compliance ? "col-xs-7" : (!displayOneAsset ? "col-xs-6" : "col-xs-9")
                    )}>
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                                        trigger={["hover"]}
                                        overlay={
                                            <Tooltip id={`threats-panel-sort-tooltip`}
                                                     className={"tooltip-overlay"}>
                                                Sort by Threat
                                            </Tooltip>
                                        }>
                            <span onClick={() => this.sortByCol("threat")}>
                                {threatHeader} <span className={sortDirIcon}
                                   style={{display: sortCol === "threat" ? "inline-block" : "none"}}/>
                            </span>
                        </OverlayTrigger>
                    </span>
                    {!displayOneAsset && !attackPath ? 
                        <span className={(compliance ? "col-xs-4" : "col-xs-3")} style={{minWidth: "50px"}}>
                            <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                                            trigger={["hover"]}
                                            overlay={
                                                <Tooltip id={`threats-panel-sort-asset-tooltip`}
                                                        className={"tooltip-overlay"}>
                                                    Sort by Asset
                                                </Tooltip>
                                            }>
                                <span onClick={() => this.sortByCol("asset")}>
                                    Asset <span className={sortDirIcon} style={{
                                            display: sortCol === "asset" ? "inline-block" : "none"
                                    }}/>
                                </span>
                            </OverlayTrigger>
                        </span>
                    : null }
                    {!compliance && attackPath &&
                    <span className="col-xs-1 distance">
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                                        trigger={["hover"]}
                                        overlay={
                                            <Tooltip id={`threats-panel-sort-distance-tooltip`}
                                                     className={"tooltip-overlay"}>
                                                Sort by Distance to Target
                                            </Tooltip>
                                        }>
                            <span onClick={() => this.sortByCol("distance")}>
                                <span className="head-text">Distance</span>
                                <span className={sortDirIcon}
                                    style={{display: sortCol === "distance" ? "inline-block" : "none"}}/>
                            </span>
                        </OverlayTrigger>
                    </span>
                    }
                    {!compliance &&
                    <span className="col-xs-1 likelihood">
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                                        trigger={["hover"]}
                                        overlay={
                                            <Tooltip id={`threats-panel-sort-likelihood-tooltip`}
                                                     className={"tooltip-overlay"}>
                                                Sort by Likelihood
                                            </Tooltip>
                                        }>
                            <span onClick={() => this.sortByCol("likelihood")}>
                                <span className="head-text">Likelihood</span>
                                <span className={sortDirIcon}
                                    style={{display: sortCol === "likelihood" ? "inline-block" : "none"}}/>
                            </span>
                        </OverlayTrigger>
                    </span>
                    }
                    {!compliance && !attackPath &&
                    <span className="col-xs-1 risk">
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                                        trigger={["hover"]}
                                        overlay={
                                            <Tooltip id={`threats-panel-sort-risk-tooltip`}
                                                     className={"tooltip-overlay"}>
                                                Sort by Risk
                                            </Tooltip>
                                        }>
                            <span onClick={() => this.sortByCol("risk")}>
                                <span className="head-text">System Risk</span>
                                <span className={sortDirIcon}
                                    style={{display: sortCol === "risk" ? "inline-block" : "none"}}/>
                            </span>
                        </OverlayTrigger>
                    </span>
                    }
                </div>
                <PagedPanel panelData={threatsRender}
                            pageSize={15}
                            context={this.props.name + "-" + this.props.context}
                            noDataMessage={"No threats"}/>


            </div>
        )
    }

    sortByCol(sortCol) {
        //console.log("Sort by:", sortCol);

        let dir = this.state.sort.dir;

        //if still same sort col, simply reverse direction
        if (sortCol === this.state.sort.col) {
            if (dir === "asc")
                dir = "desc"
            else
                dir = "asc"
        }
        //otherwise, set starting direction appropiately (by threat ascending, otherwise highest value first
        else {
            if (sortCol === "threat")
                dir = "asc";
            else
                dir = "desc";
        }

        this.setState({...this.state,
            sort: {...this.state.sort,
                col: sortCol,
                dir: dir
            }
        });
    }

    openThreatEditor(threatId) {
        //console.log("openThreatEditor for threatId: " + threatId);
        this.props.dispatch(suppressCanvasRefresh(true));
        this.props.dispatch(bringToFrontWindow("threatEditor"));

        /* KEM 27/7/17 - does not seem to be required
        // If we are displaying root threats we do not want to update the root causes modal
        let updateRootCausesModel = !this.props.displayRootThreats;

        this.props.dispatch(getCauseEffect(this.props.model["id"], threatId, updateRootCausesModel));
        */

        //this.props.dispatch(getCauseEffect(this.props.model["id"], threatId));
        this.props.dispatch(toggleThreatEditor(true, threatId));
    }

    getAssetByUri(assetUri) {
        //console.log("getAssetByUri: " + assetUri);
        let asset = this.props.model.assets.find((asset) => {
            return (asset.uri === assetUri);
        });
        return asset;
    }

}

ThreatsPanel.propTypes = {
    name: PropTypes.string,
    index: PropTypes.number, //only used to distinguish compliance threats
    model: PropTypes.object,
    threats: PropTypes.array,
    context: PropTypes.string,
    getDescription: PropTypes.func,
    selectedAsset: PropTypes.object,
    selectedThreat: PropTypes.object,
    complianceSet: PropTypes.object,
    displayRootThreats: PropTypes.bool,
    threatFiltersActive: PropTypes.object,
    loading: PropTypes.object,
    hoverThreat: PropTypes.func,
    getDirectThreats: PropTypes.func,
    getIndirectThreats: PropTypes.func,
    //getRootThreats: PropTypes.func,
    dispatch: PropTypes.func
    //riskLevels: PropTypes.array,
    //likelihoodLevels: PropTypes.array
};

let mapStateToProps = function (state) {
    return {
        rightSidePanelWidth: state.modeller.view.rightSidePanelWidth
    }
};

export default connect(mapStateToProps)(ThreatsPanel);
