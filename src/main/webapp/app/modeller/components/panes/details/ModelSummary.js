import React, {Component} from 'react';
import PropTypes from "prop-types";
import {
    Button,
    DropdownButton,
    Label,
    MenuItem,
    OverlayTrigger,
    Panel,
    Row,
    Tooltip,
    Form, FormGroup, InputGroup, FormControl, Checkbox, ButtonToolbar
} from "react-bootstrap";
import * as Constants from "../../../../common/constants.js";
import ChangeRelationModal from "./popups/ChangeRelationModal";
import {
    changeSelectedAsset,
    openComplianceExplorer,
    openControlExplorer,
    openControlStrategyExplorer,
    openReportDialog,
    postAssertedRelation,
    putRelationRedefine,
    resetControls,
    updateModel
} from "../../../actions/ModellerActions";
import {getPlumbingInstance, hoveredConns, setHoveredConns} from "../../util/TileFactory";
import ThreatsPanel from "./accordion/panels/ThreatsPanel";
import PagedPanel from "../../../../common/components/pagedpanel/PagedPanel"
import EditModelModal from "../../../../dashboard/components/popups/EditModelModal"; //TODO: make common component?
import ModelMisBehavPanel from "./accordion/panels/ModelMisBehavPanel";
import {bringToFrontWindow} from "../../../actions/ViewActions";
import {openDocumentation} from "../../../../common/documentation/documentation"

class ModelSummary extends Component {

    constructor(props) {
        super(props);

        this.openReport = this.openReport.bind(this);
        this.updateModel = this.updateModel.bind(this);
        this.hoverAsset = this.hoverAsset.bind(this);
        this.formatRelationLabel = this.formatRelationLabel.bind(this);
        this.handleEditRelation = this.handleEditRelation.bind(this);
        this.openChangeRelationModal = this.openChangeRelationModal.bind(this);
        this.submitChangeRelationModal = this.submitChangeRelationModal.bind(this);
        this.closeChangeRelationModal = this.closeChangeRelationModal.bind(this);
        this.renderPossibleModellingErrorsPanel = this.renderPossibleModellingErrorsPanel.bind(this);
        this.renderCompliancePanel = this.renderCompliancePanel.bind(this);
        this.renderControlSetsPanel = this.renderControlSetsPanel.bind(this);
        this.renderControlStrategiesPanel = this.renderControlStrategiesPanel.bind(this);
        this.resetControls = this.resetControls.bind(this);
        this.togglePanel = this.togglePanel.bind(this);
        this.renderHeader = this.renderHeader.bind(this);
        this.renderPossibleModellingErrorsHeader = this.renderPossibleModellingErrorsHeader.bind(this);
        this.renderAssetsHeader = this.renderAssetsHeader.bind(this);
        this.renderControlSetsHeader = this.renderControlSetsHeader.bind(this);
        this.renderControlStrategiesHeader = this.renderControlStrategiesHeader.bind(this);
        this.renderThreatsHeader = this.renderThreatsHeader.bind(this);
        this.renderComplianceHeader = this.renderComplianceHeader.bind(this);
        this.countProposedControls = this.countProposedControls.bind(this);
        this.countProposedControlStrategies = this.countProposedControlStrategies.bind(this);


        this.state = {
            changeRelationModal: {
                show: false,
                assets: [],
                relation: {},
                host: {},
                isFrom: true,
                issue: {}
            },
            editDetailsModal: false,
            assets: {
                search: "",
                filter: false,
                selectedAssetOnly: true
            },
            controls: {
                updating: []
            },
        }
    }

    componentDidUpdate(prevProps) {
        //if controls were reset, clear the updating controls list 
        if (!prevProps.model.controlsReset && this.props.model.controlsReset) {
            this.setState({
                ...this.state,
                controls: {
                    updating: []
                }
            })
        }
    }
    
    togglePanel(panel) {
        let panelsOpen = {...this.state.panelsOpen};
        panelsOpen[panel] = !panelsOpen[panel];
        this.setState({...this.state,
            panelsOpen: panelsOpen
        });
    }

    render() {
        const pageSize = 15;
        let assets = this.props.model.assets;

        let controlSets = new Map();
        this.props.model.controlSets.forEach((cs) => {
            if (!cs.assertable) return;
            controlSets.set(cs.label, controlSets.get(cs.label) == null ? [cs] : controlSets.get(cs.label).concat([cs]));
        });
        controlSets = new Map([...controlSets].sort());

        let csgs = Object.values(this.props.model.controlStrategies);
        //console.log("csgs:", csgs);

        let csgsByName = new Map();

        csgs.forEach((csg) => {
            let name = csg.label;
            csgsByName.set(name, csgsByName.get(name) == null ? [csg] : csgsByName.get(name).concat([csg]));
        });

        csgsByName = new Map([...csgsByName.entries()].sort());
        //console.log("csgsByName:", csgsByName);
        let csgsArray = Array.from(csgsByName);
        //console.log("csgsArray:", csgsArray);

        let threats = this.props.threats;
        let treatedThreats = threats.filter((threat) => threat["resolved"] || threat["acceptanceJustification"] !== null);
        let filteredAssets = assets.filter(a => a.asserted || !this.state.assets.filter).filter((a) => a["label"].toLowerCase().indexOf(this.state.assets.search.toLowerCase()) > -1);
        let domain = this.props.model["domainGraph"] != null ? this.props.model["domainGraph"].replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-", "").toUpperCase() : "-";
        let domainVersion = this.props.model["domainVersion"] != null ? this.props.model["domainVersion"] : "-";
        let validatedDomainVersion = this.props.model["validatedDomainVersion"] != null ? this.props.model["validatedDomainVersion"] : "-";
        let description = this.props.model["description"];

        let versionWarningText = "Version does not match current knowledgebase version (" + domainVersion + "). Please revalidate!";
        let versionMismatch = (validatedDomainVersion !== domainVersion);
        
        let assetsRender = [];
        filteredAssets.sort((a, b) => a.label.localeCompare(b.label)).forEach((a, index) => {
            assetsRender.push(<Row key={index}
                                   className="row-hover bare-list"
                                   onMouseEnter={() => this.hoverAsset(a, true)}
                                   onMouseLeave={() => this.hoverAsset(a, false)}>
                <a className="clickable"
                      style={{paddingLeft: "0px"}}
                      onClick={() => this.props.dispatch(changeSelectedAsset(a.id))}>
                    {a.label}
                </a>
                {// use this in the future to edit/delete assets
                    /*<Button bsClass="btn btn-primary btn-xs col-xs-3"*/}
                {/*        onClick={() => this.props.dispatch(changeSelectedAsset(a.id))}>View</Button>*/}
            </Row>)
        });

        let misbehaviourSets = this.props.model.misbehaviourSets;
        
        //Determine displayed misbehaviours - in this case those that are "visible"
        let displayedMisbehaviours = Object.values(misbehaviourSets).filter((misbehaviour) => {
            let visible = misbehaviour["visible"];
            let invisible = visible !== undefined && !visible;
            return !invisible;
        });

        return (
            <div>
                {this.props.model !== undefined &&
                    <Panel bsStyle="primary" defaultExpanded>
                        <Panel.Heading>
                            <Panel.Title toggle>
                                <div className={"doc-help"}>
                                    <span><i className="fa fa-info-circle " />Model Details and Reports</span>
                                    <button onClick={e => openDocumentation(e, "redirect/model-details")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                                </div>
                            </Panel.Title>
                        </Panel.Heading>
                        <Panel.Collapse>
                            <Panel.Body>
                                <dl className="dl-compact">
                                    <dt>Name</dt><dd>{this.props.model.name || "-"}</dd>
                                    <dt>Knowledgebase</dt><dd>{domain}</dd>
                                    <dt>KB Version</dt><dd>{validatedDomainVersion}
                                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="bottom"
                                            overlay={<Tooltip id="version-tooltip">
                                                <strong>{versionWarningText}</strong></Tooltip>}>
                                            <div style={{
                                                display: versionMismatch ? "inline-block" : "none",
                                                marginLeft: "10px"
                                            }}>
                                                <i className="fa fa-exclamation-triangle warning"></i>
                                            </div>
                                        </OverlayTrigger>
                                    </dd>
                                    <dt>Description</dt><dd>{description || "-"}</dd>
                                    <dt>Number of Assets</dt><dd>{this.props.model.assets.length}</dd>
                                    <dt>Number of Relations</dt><dd>{this.props.model.relations.length}</dd>
                                    <dt>Number of Threats</dt><dd>{this.props.model.threats.length}</dd>
                                </dl>
                                <ButtonToolbar>
                                    <Button disabled={!this.props.authz.userEdit} bsClass="btn btn-primary btn-xs"
                                        onClick={() => { this.setState({ ...this.state, editDetailsModal: true }) }}
                                    >Edit Details</Button>
                                    <DropdownButton
                                        id="open-reports"
                                        bsStyle="primary"
                                        bsSize="xs"
                                        title="Open Report"
                                    >
                                        <MenuItem onClick={() => this.openReport("technicalReport")}>Technical Report</MenuItem>
                                        {/* Risk treatment plan option is currently disabled until it can either be fixed, or replaced by something better */}
                                        {/* <MenuItem divider />
                                        <MenuItem onClick={() => this.openReport("riskTreatmentPlan")}>Risk Treatment Plan</MenuItem> */}
                                    </DropdownButton>
                                </ButtonToolbar>
                            </Panel.Body>
                        </Panel.Collapse>
                    </Panel>}

                {this.renderPossibleModellingErrorsPanel()}

                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderAssetsHeader(assets.length)}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            {assets.length > 0 &&
                                <Form>
                                    <FormGroup>
                                        <InputGroup>
                                            <InputGroup.Addon><i className="fa fa-lg fa-filter"/></InputGroup.Addon>
                                            <FormControl 
                                                type="text"
                                                placeholder="Filter assets by name"
                                                value={this.state.assets.search}
                                                onChange={(e) => {
                                                    this.setState({
                                                        ...this.state,
                                                        assets: {...this.state.assets, search: e.nativeEvent.target.value.trim()}
                                                    })
                                                }}
                                                // need to prevent the Form being submitted when Return is pressed
                                                onKeyPress={(e) => { e.key === 'Enter' && e.preventDefault(); }}
                                            />
                                        </InputGroup>
                                    </FormGroup>
                                    <FormGroup>
                                        <Checkbox
                                            checked={this.state.assets.filter}
                                            onChange={(e) => {
                                                this.setState({
                                                    ...this.state,
                                                    assets: {
                                                        ...this.state.assets,
                                                        filter: e.nativeEvent.target.checked
                                                    }
                                                })
                                            }}>
                                            Only asserted assets
                                        </Checkbox>
                                    </FormGroup>
                                </Form>
                            }
                            <PagedPanel panelData={assetsRender}
                                        pageSize={15}
                                        context={"assets-" + this.props.model.id}
                                        noDataMessage={"No assets found"}/>
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderControlSetsHeader(controlSets.size)}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            {this.renderControlSetsPanel(Array.from(controlSets))}
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderControlStrategiesHeader(csgsArray.length)}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            {this.renderControlStrategiesPanel(csgsArray)}
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel bsStyle="primary" defaultExpanded>
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                <span>
                                    <i className="fa fa-sitemap "/>Consequences and their Impact ({displayedMisbehaviours.length})
                                </span>
                                <button onClick={e => openDocumentation(e, "redirect/model-consequences-and-impacts")} className={"doc-help-button"}><i className="fa fa-question"/></button>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <ModelMisBehavPanel panelType="model-misbehaviours"
                                                selectedMisbehaviours={[]}
                                                adjustAssetNameSizes={true}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            {this.renderThreatsHeader(treatedThreats.length, threats.length)}
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <ThreatsPanel dispatch={this.props.dispatch}
                                          name={"model-summary"}
                                          context={this.props.model.id}
                                          model={this.props.model}
                                          threats={threats}
                                          selectedAsset={null}
                                          selectedThreat={this.props.selectedThreat}
                                          displayRootThreats={false}
                                          hoverThreat={this.props.hoverThreat}
                                          getRootThreats={null} // method not required here
                                          threatFiltersActive={null} // not required here
                                />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                {/* <Panel bsStyle="primary" defaultExpanded>
                    <Panel.Heading>
                        <Panel.Title toggle>
                            Compliance ({this.props.model.complianceSets.length})
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            {this.renderCompliancePanel()}
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel> */}
                {this.renderCompliancePanel()}
                <ChangeRelationModal show={this.state.changeRelationModal.show}
                                     onHide={this.closeChangeRelationModal}
                                     assets={this.state.changeRelationModal.assets}
                                     host={this.state.changeRelationModal.host}
                                     relation={this.state.changeRelationModal.relation}
                                     issue={this.state.changeRelationModal.issue}
                                     isFrom={this.state.changeRelationModal.isFrom}
                                     formatRelationLabel={this.formatRelationLabel}
                                     submit={this.submitChangeRelationModal}/>
                <EditModelModal show={this.state.editDetailsModal} model={this.props.model} updateModel={this.updateModel}
                                onHide={() => this.setState({...this.state, editDetailsModal: false})}/>
            </div>
        )
    }

    renderHeader(title, iconName, n1, n2, tt_text, helpLink) {
        let overlay = "";
        if (tt_text) {
            let overlayProps = {
                delayShow: Constants.TOOLTIP_DELAY, placement: "top", trigger: ["hover"],
                overlay: <Tooltip id={title.toLowerCase().replace(" ", "-") +
                    "tooltip"}>{tt_text}</Tooltip>
            };
            overlay = <OverlayTrigger {...overlayProps}>
                <span>{n1 > -1 ? "(" + n1 + "/" + n2 + ")" : "(" + n2 + ")"}
                </span>
            </OverlayTrigger>;
        }

        return (
            <div className={"doc-help"}>
                <span>
                    <i className={iconName}/>{title}{" "}{overlay}
                </span>
                <button onClick={e => openDocumentation(e, "redirect/" + helpLink)} className={"doc-help-button"}><i className="fa fa-question"/></button>
            </div>
        );
    }
    
    renderPossibleModellingErrorsHeader(complianceSet, nCompliant, nCompThreats) {
        let complianceLabel = "Possible Modelling Errors";        
        let icon = complianceSet.compliant ? "fa fa-check" : "fa fa-warning";
        let tt_text = (nCompThreats > 0) ? nCompliant + " out of " + nCompThreats + " addressed" : "no modelling errors";
        return this.renderHeader(complianceLabel, icon, nCompliant, nCompThreats, tt_text, "model-possible-modelling-errors");
    }

    renderAssetsHeader(nAssets) {
        let tt_text = nAssets + " assets (asserted and inferred)";
        return this.renderHeader("Assets", "fa fa-cube", -1, nAssets, tt_text, "model-assets");
    }
    
    /* KEM: this version of the method could be used if we want to summarize all control sets, not just the control types
    renderControlSetsHeader(n1, n2) {
        let tt_text = n1 + " out of " + n2 + " selected";
        return this.renderHeader("Controls", "fa fa-tag", n1, n2, tt_text); //Controls or Control Sets?
    }
    */
    
    renderControlSetsHeader(nControls) {
        let tt_text = nControls + " control types";
        return this.renderHeader("Controls", "fa fa-tag", -1, nControls, tt_text, "model-controls"); //Controls or Control Sets?
    }

    renderControlStrategiesHeader(nCSGs) {
        let tt_text = nCSGs + " control strategies";
        return this.renderHeader("Control Strategies", "fa fa-shield", -1, nCSGs, tt_text, "model-control-strategies");
    }

    renderThreatsHeader(n1, n2) {
        let tt_text = n1 + " out of " + n2 + " addressed";
        return this.renderHeader("Threats", "fa fa-exclamation-triangle", n1, n2, tt_text, "model-threats");
    }

    renderComplianceHeader(n1, n2) {
        let tt_text = n1 + " out of " + n2 + " compliant";
        return this.renderHeader("Compliance", "fa fa-check", n1, n2, tt_text, "model-compliance");
    }

    renderControlSetsPanel(controlSets) {
        let controlsRender = [];
        controlSets.map((controlSet, index) => {
            let name = controlSet[0]; //e.g. AccessControl 
            let csList = controlSet[1]; //list of cs uris
            let listName = "ControlSet" + index;
            var assetUris = new Map();
            csList.forEach((c) => {
                assetUris.set(c.assetUri, c);
            });
            var assets = new Map();
            this.props.model.assets.forEach((a) => {
                if(Array.from(assetUris.keys()).includes(a.uri)){
                    assets.set(a, assetUris.get(a.uri));
                }
            });
            assets = new Map([...assets].sort((a, b) => {return (a[0].label > b[0].label) ? 1 : ((b[0].label > a[0].label) ? -1 : 0)}));
            let spinnerActive = this.state.controls.updating.includes(name);

            let ctrlSetOverlayProps = {
                delayShow: Constants.TOOLTIP_DELAY, placement: "left",
                overlay: <Tooltip id={"control-set-" + 1 + "-error-tooltip"}
                                  className={"tooltip-overlay"}>
                    {csList[0].description ? (csList[0] && csList[0].description) : csList[0].control }
                </Tooltip>
            };

            controlsRender.push(
                <Row
                    key={index}
                    className="row-hover bare-list"
                >
                    <OverlayTrigger {...ctrlSetOverlayProps}>
                        <span onMouseEnter={() => this.hoverControl(assets, true)}
                             onMouseLeave={() => this.hoverControl(assets, false)}
                             className="clickable"
                             onClick={() => {
                                 this.props.dispatch(openControlExplorer(csList[0].control));
                                 this.props.dispatch(bringToFrontWindow("controlExplorer"));
                             }}>
                             {name} : {this.countProposedControls(csList)} of {csList.length} {" "}
                             {spinnerActive ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw"/> : null}
                        </span>
                    </OverlayTrigger>
                    {// use this to apply all controls in the future
                        /*<Button bsClass="btn btn-primary btn-xs col-xs-3" onClick={}>View</Button>*/}
                </Row>);
        });

        let controlSetReset = <FormGroup>
            <Button bsStyle="danger" bsSize="xsmall"
                onClick={() => this.resetControls(
                    Array.from(this.props.model.controlSets)
                )}>
                Remove All Controls
            </Button>
        </FormGroup>

        return (
            <div>
            { controlSets.length > 0 ? controlSetReset : "" }
            <PagedPanel panelData={controlsRender}
                        pageSize={15}
                        context={"controls-" + this.props.model.id}
                        noDataMessage={"No controls found"}/>
            </div>
        )
    }

    renderControlStrategiesPanel(csgs) {
        let csgsRender = [];

        csgs.map((csgEntry, index) => {
            let name = csgEntry[0];
            let csgList = csgEntry[1];
            let context = {"selection": "csgType"};
            let spinnerActive = false; //may not need this

            let csgOverlayProps = {
                delayShow: Constants.TOOLTIP_DELAY, placement: "left",
                overlay: <Tooltip id={"csg-" + 1 + "-error-tooltip"}
                                  className={"tooltip-overlay"}>
                    {csgList[0].description ? csgList[0].description : "" }
                </Tooltip>
            };

            csgsRender.push(
                <Row
                    key={index}
                    className="row-hover bare-list"
                >
                    <OverlayTrigger {...csgOverlayProps}>
                        <span 
                             className="clickable"
                             onClick={() => {
                                 this.props.dispatch(openControlStrategyExplorer(csgList, context));
                                 this.props.dispatch(bringToFrontWindow("controlStrategyExplorer"));
                             }}>
                             {name} : {this.countProposedControlStrategies(csgList)} of {csgList.length} {" "}
                             {spinnerActive ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw"/> : null}
                        </span>
                    </OverlayTrigger>
                </Row>);
        });

        return (
            <div>
                <PagedPanel panelData={csgsRender}
                            pageSize={15}
                            context={"csgs-" + this.props.model.id}
                            noDataMessage={"No control strategies found"}/>
            </div>
        )
    }

    updateModel(id, updatedModel) {
        this.setState({ ...this.state, editDetailsModal: false });
        this.props.dispatch(updateModel(id, updatedModel));
    }
    
    resetControls(controlsSet) {
        let flag = false; //reset 
        
        //KEM - this could be improved further, as we may simply need a "reset all" method/request,
        //but this would require an extra method on the server.
        
        let controlLabels = [];
        let controlsToUpdate = Array.from(controlsSet.map((c) => {
            if (c.assertable && flag !== c.proposed) {
                if (! controlLabels.includes(c.label))
                    controlLabels.push(c.label);
                return c.uri;
            }
        }).filter(uri => {return uri !== undefined}));
        
        if (controlsToUpdate.length > 0) {
            this.updateControlsState(controlLabels);
            this.props.dispatch(resetControls(this.props.model.id, controlsToUpdate));
        }
        else {
            //console.log("No controls to update");
        }
    }

    updateControlsState(controlLabels) {                
        this.setState({...this.state,
            controls: {
                updating: controlLabels
            }
        });
    }

    renderPossibleModellingErrorsPanel() {
        let modellingErrorsCS = this.props.complianceSetsData["modellingErrors"];
        
        if (modellingErrorsCS === undefined || Object.keys(modellingErrorsCS).length == 0) {
            return null;
        }

        //of these, determine resolved list
        let nModellingErrors = modellingErrorsCS.nCompThreats;
        let nTreated = modellingErrorsCS.nTreatedThreats;
        
        return (
            <Panel bsStyle={modellingErrorsCS.compliant ? "success" : "danger"}>
                <Panel.Heading>
                    <Panel.Title toggle>
                        {this.renderPossibleModellingErrorsHeader(modellingErrorsCS, nTreated, nModellingErrors)}
                    </Panel.Title>
                </Panel.Heading>
                <Panel.Collapse>
                    <Panel.Body>
                        <ThreatsPanel dispatch={this.props.dispatch}
                                      name={"modelling-errors"}
                                      index={0}
                                      model={this.props.model}
                                      selectedAsset={null}
                                      selectedThreat={this.props.selectedThreat}
                                      displayRootThreats={true} //technically false, but allows us to use getRootThreats below
                                      hoverThreat={this.props.hoverThreat}
                                      getRootThreats={this.getComplianceThreats}
                                      complianceSet={modellingErrorsCS}
                                      threatFiltersActive={this.props.threatFiltersActive}
                                      loading={this.props.loading}/>
                    </Panel.Body>
                </Panel.Collapse>
            </Panel>
        )
    }

    renderCompliancePanel() {
        let complianceSetsData = this.props.complianceSetsData;
        let complianceSets = complianceSetsData["complianceSets"];
        let nComplianceSets = complianceSets.length;
        let nCompliant = complianceSetsData["nCompliant"];

        // TODO: put these colors and style in a stylesheet
        let compliantSymbol = <span className="fa fa-check threat-icon" style={{ backgroundColor: "rgb(216,230,230)" }} />;
        let nonCompliantSymbol = <span className="fa fa-exclamation-triangle threat-icon" style={{ backgroundColor: "red", color: "white" }} />

        return (
        <Panel bsStyle={nCompliant < nComplianceSets ? "danger" : "success"}>
            <Panel.Heading>
                <Panel.Title toggle>
                    {this.renderComplianceHeader(nCompliant, nComplianceSets)}
                </Panel.Title>
            </Panel.Heading>
            <Panel.Collapse>
                <Panel.Body>
            <div>
            {complianceSets.map((complianceSet, index) => {
                let nCompThreats = complianceSet["nCompThreats"];
                let nTreatedThreats = complianceSet["nTreatedThreats"];
                let complianceLabel = complianceSet.label;
                let compliant = complianceSet.compliant;
                
                let status = compliant ? "Compliant" : "Non-compliant";
                let symbol = compliant ? compliantSymbol : nonCompliantSymbol;
                
                if (nCompThreats === 0) {
                    complianceLabel += ` (0)`;
                    complianceLabel = complianceLabel.replace("Issues", "Issue");
                    complianceLabel = complianceLabel.replace("Errors", "Error");
                } else if (nCompThreats === 1) {
                    complianceLabel += ` (${nTreatedThreats}/${nCompThreats})`;
                    complianceLabel = complianceLabel.replace("Issues", "Issue");
                    complianceLabel = complianceLabel.replace("Errors", "Error");
                } else {
                    complianceLabel += ` (${nTreatedThreats}/${nCompThreats})`;
                }

                let overlay = "";
                if (status) {
                    let overlayProps = {
                        delayShow: Constants.TOOLTIP_DELAY, placement: "left", trigger: ["hover"],
                        overlay: <Tooltip id={`compliance-set-${index + 1}-tooltip`}>
                            {status}</Tooltip>
                    };
                    overlay = <OverlayTrigger {...overlayProps}>
                        {symbol}
                    </OverlayTrigger>;
                }

                return (
                    <div key={index + 1} className="row detail-info">
                        <span className="col-xs-1">{overlay}</span>
                        <span className="col-xs-11 compliance-label">{complianceLabel}</span>
                    </div>
                );
            })}
            <Button bsClass="btn btn-primary btn-xs" style={{marginTop: "10px"}}
                    onClick={() => {
                        this.props.dispatch(openComplianceExplorer());
                        this.props.dispatch(bringToFrontWindow("complianceExplorer"));
                    }}>See Details</Button>
            </div>
                </Panel.Body>
            </Panel.Collapse>
        </Panel>
        )
    }

    countProposedControls(controlSets) {
        var num = 0;
        controlSets.forEach((cs) => {
            if(cs.proposed){
                num++;
            }
        });
        return num;
    }

    countProposedControlStrategies(csgs) {
        var num = 0;
        csgs.forEach((csg) => {
            if(csg.enabled){
                num++;
            }
        });
        return num;
    }

    hoverControl(assets, flag) {
        if (!flag) {
            hoveredConns.map((hoveredConn) => {
                let conn = hoveredConn.conn;
                let labelEl = hoveredConn.labelEl;
                let originalType = hoveredConn.originalType;
                conn.setType(originalType.join(" "));
            });

            //Finally, clear the list of hovered conns
            setHoveredConns([]);
        };

        assets.forEach((a, b) => {this.hoverAsset(b, flag, assets.get(b).proposed ? "green" : "default")})
    }

    hoverAsset(asset, flag, colour="default") {
        var styleClass = (colour == null || colour == "default" ? "hover" : "active-tile");

        if (asset.asserted) {
            if (flag) {
                $("#tile-" + asset["id"]).addClass(styleClass);
            } else {
                $("#tile-" + asset["id"]).removeClass(styleClass);
            }
        } else {
            let a = this.props.model.assets.filter(b => b.inferredAssets.indexOf(asset.uri) > -1);
            if (a.length !== 0) {
                for (let c = 0; c < a.length; c++) {
                    if (flag) {
                        $("#tile-" + a[c]["id"]).addClass(styleClass);
                    } else {
                        $("#tile-" + a[c]["id"]).removeClass(styleClass);
                    }
                }
            }
            a = this.props.model.relations.filter(b => b.inferredAssets.indexOf(asset.uri) > -1);
            if (a.length !== 0 && flag) {
                let hConns = hoveredConns;
                for (let c = 0; c < a.length; c++) {
                    getPlumbingInstance().select(
                        {
                            source: "tile-" + a[c]["fromID"],
                            target: "tile-" + a[c]["toID"],
                            scope: ["relations", "inferred-relations"]
                        }, true
                    ).each((conn) => {
                        let connLabel = conn.getOverlay("label");
                        let labelLoc = connLabel.getLocation();
                        let connEl = connLabel.getElement();
                        if (connEl.innerHTML === a[c]["label"]) {
                            let currType = conn.getType();
                            let originalType = [...currType];
                            conn.setType("hover");

                            var hoveredConn = {
                                conn:  conn,
                                labelEl: connEl,
                                originalType: originalType
                            };
                            
                            hConns.push(hoveredConn);
                        }
                    });
                };
                setHoveredConns(hConns);
            }
        }
    }

    formatRelationLabel(label) {
        if (label === undefined)
            return "";

        //Just use label as it is now (if label text is wrong, it should be fixed in the original label, in the knowledgebase)
        //return label.replace(/([A-Z])/g, ' $1').trim().toLowerCase();
        return label;
    }

    handleEditRelation(linkTo, relation, host, isFrom, issue) {
        let assets = this.props.getAssetsForType(linkTo);

        if (assets.length <= 0) {
            console.warn("No matching assets found");
        }

        this.openChangeRelationModal(assets, relation, host, isFrom, issue);
    }

    openChangeRelationModal(assets, relation, host, isFrom, issue) {
        this.setState({
            ...this.state,
            changeRelationModal: {
                ...this.state.changeRelationModal,
                show: true,
                assets: assets,
                relation: relation,
                host: host,
                isFrom: isFrom,
                issue: issue
            }
        });
    }

    submitChangeRelationModal(relation) {
        if (typeof relation["id"] === 'undefined') {
            this.props.dispatch(postAssertedRelation(this.props.model["id"], relation["fromID"], relation["toID"], relation));
        }
        else {
            this.props.dispatch(putRelationRedefine(this.props.model["id"], relation["id"], relation));
        }
    }

    closeChangeRelationModal() {
        this.setState({
            ...this.state,
            changeRelationModal: {
                ...this.state.changeRelationModal,
                show: false
            }
        });
    }

    openReport(reportType) {
        this.props.dispatch(openReportDialog(reportType));
        this.props.dispatch(bringToFrontWindow("reportDialog"));
    }
}

ModelSummary.propTypes = {
    model: PropTypes.object,
    threats: PropTypes.array,
    complianceSetsData: PropTypes.object,
    hasModellingErrors: PropTypes.bool,
    selectedThreat: PropTypes.object,
    getAssetType: PropTypes.func,
    getAssetsForType: PropTypes.func,
    getLink: PropTypes.func,
    hoverThreat: PropTypes.func,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
};

export default ModelSummary;
