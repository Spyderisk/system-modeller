import React from "react";
import ReactDOM from "react-dom";
import PropTypes from "prop-types";
import {connect} from "react-redux";
import {FormControl, OverlayTrigger, Tooltip} from 'react-bootstrap';
import {
    closeComplianceExplorer,
    closeControlExplorer,
    closeControlStrategyExplorer,
    closeRecommendationsExplorer,
    closeMisbehaviourExplorer,
    closeReportDialog,
    getModel,
    postAssertedAsset,
    toggleThreatEditor,
    updateEdit,
} from "../actions/ModellerActions"
import {getPlumbingInstance, hoveringThreat, setHoveringThreat, hoveredConns, setHoveredConns} from "./util/TileFactory";
import {getLevelColour, getRenderedLevelText} from "./util/Levels";
import {getThreatStatus} from "./util/ThreatUtils";
import LoadingOverlay from "./util/LoadingOverlay";
import AssetPane from "./panes/palette/AssetPane";
import DetailPane from "./panes/details/DetailPane";
import ThreatEditor from "./panes/threats/ThreatEditor";
import RootCausesEditor from "./panes/details/popups/RootCausesEditor";
import ComplianceExplorer from "./panes/compliance/ComplianceExplorer";
import ControlExplorer from "./panes/controlExplorer/ControlExplorer";
import ControlStrategyExplorer from "./panes/csgExplorer/ControlStrategyExplorer";
import RecommendationsExplorer from "./panes/recommendationsExplorer/RecommendationsExplorer";
import ControlPane from "./panes/controls/controlPane/ControlPane";
import OverviewPane from "./panes/controls/overviewPane/OverviewPane";
import Canvas from "./canvas/Canvas";
import ReportDialog from "./panes/reports/ReportDialog";
import * as Constants from "../../common/constants.js"
import "../index.scss";
import { axiosInstance } from "../../common/rest/rest";

/**
 * This is the smart component that handles the editor functionality.
 */
class Modeller extends React.Component {

    /**
     * Constructor is used to bind methods to this class.
     * @param props Props passed from store.
     */
    constructor(props) {
        super(props);

        this.isAssetNameTaken = this.isAssetNameTaken.bind(this);
        this.isGroupNameTaken = this.isGroupNameTaken.bind(this);
        this.handleTileCreation = this.handleTileCreation.bind(this);
        this.getAssetType = this.getAssetType.bind(this);
        this.getAssetsForType = this.getAssetsForType.bind(this);
        this.getAssetCount = this.getAssetCount.bind(this);
        this.getAssetTypeById = this.getAssetTypeById.bind(this);
        this.isAssetDisplayed = this.isAssetDisplayed.bind(this);
        this.isRelationDisplayed = this.isRelationDisplayed.bind(this);
        this.getLink = this.getLink.bind(this);
        this.getMisbehaviour = this.getMisbehaviour.bind(this);
        this.getTwasForMisbehaviourSet = this.getTwasForMisbehaviourSet.bind(this);
        this.renderTrustworthinessAttributes = this.renderTrustworthinessAttributes.bind(this);
        this.getModellingErrorThreats = this.getModellingErrorThreats.bind(this);
        this.getSelectedThreat = this.getSelectedThreat.bind(this);
        this.hoverThreat = this.hoverThreat.bind(this);
        this.isRelationDeletable = this.isRelationDeletable.bind(this);
        this.closeThreatEditor = this.closeThreatEditor.bind(this);
        this.closeMisbehaviourExplorer = this.closeMisbehaviourExplorer.bind(this);
        this.closeComplianceExplorer = this.closeComplianceExplorer.bind(this);
        this.closeControlExplorer = this.closeControlExplorer.bind(this);
        this.closeControlStrategyExplorer = this.closeControlStrategyExplorer.bind(this);
        this.closeRecommendationsExplorer = this.closeRecommendationsExplorer.bind(this);
        this.closeReportDialog = this.closeReportDialog.bind(this);
        this.populateThreatMisbehaviours = this.populateThreatMisbehaviours.bind(this);
        this.getSystemThreats = this.getSystemThreats.bind(this);
        this.getComplianceSetsData = this.getComplianceSetsData.bind(this);
        this.getComplianceThreats = this.getComplianceThreats.bind(this);
        this.getComplianceThreatByUri = this.getComplianceThreatByUri.bind(this);
        this.editUserRights = this.editUserRights.bind(this);
    }

    /**
     * Make sure the model is checked in (unlocked) if the user navigates away from the modeller application
     */
    onUnload = e => {
        e.preventDefault();
        if ($("meta[name='_mode']").attr("content") === "edit") {
            // Should only call checkin if we have edit rights
            axiosInstance.post("/models/" + $("meta[name='_model']").attr("content") + "/checkin");
        }
    }
 
    /**
     * Called before the component is rendered.
     * Begin collecting the model before the component starts loading to speed things up.
     */
     componentWillMount() {
        // The model key is stored in a meta tag for simplicity.
        this.props.dispatch(getModel($("meta[name='_model']").attr("content")));
        this.editUserRights($("meta[name='_mode']").attr("content"));
    } 
    componentDidMount() {
        window.addEventListener("beforeunload", this.onUnload);
     }

    componentWillUnmount() {
        window.removeEventListener("beforeunload", this.onUnload);
    }


    /**
     * Called when the component is rendered.
     * Use this lifecycle method to make all divs of type tile selectable (details panel).
     */
    shouldComponentUpdate(nextProps, nextState) {
        /*
        console.log("this.props:");
        console.log(this.props);
        console.log("nextProps: ");
        console.log(nextProps);

        //Following doesn't work, as loading overlay does not update
        //Individual components oould use a check like this
        //let shouldComponentUpdate = this.props.loading === nextProps.loading;
        */
        let shouldComponentUpdate = true;
        //console.log("Modeller.shouldComponentUpdate: " + shouldComponentUpdate);
        return shouldComponentUpdate;
    }

    editUserRights(mode) {
        let edit;
        if (mode == "edit") {
            edit = true
            console.log("edit rights changed to:", edit);
        } else {
            edit = false
            console.log("edit rights changed to:", edit);
        }
        this.props.dispatch(updateEdit(edit));
    }

    componentDidUpdate() {
        ReactDOM.render(<div className="side-app">
            <ControlPane model={this.props.model}
                         canvas={this.props.canvas}
                         selectedLayers={this.props.selectedLayers}
                         layers={this.props.model.palette.layers}
                         upload={this.props.upload}
                         dispatch={this.props.dispatch}
                         loading={this.props.loading}
                         authz={this.props.authz}
                         developerMode={this.props.developerMode}
                         //isValidating={this.props.isValidating}
                         //isCalculatingRisks={this.props.isCalculatingRisks}
                         />
            <OverviewPane threats={this.props.model.threats}
                          //misbehaviourSets={this.props.model.misbehaviourSets}
                          risk={this.props.model.risk}
                          loading={this.props.loading}
                          validating={this.props.model.validating}
                          />
        </div>, document.getElementById("app-extension-container"));
    }

    /**
     * This renders our React components to the Virtual DOM. In this case we have an asset panel, details panel, and
     * canvas rendered inside a wrapping div container.
     * @returns {XML} It is returning the XML structure used to render the HTML.
     */
    render() {
        $("#modelTitle").text("- " + this.props.model["name"]);
        let selectedThreatId = this.props.selectedThreat["id"];
        let selectedThreat;

        if (selectedThreatId !== "") {
            selectedThreat = this.getSelectedThreat()
        } else if (this.props.selectedAsset["isThreatEditorActive"]) {
            this.closeThreatEditor();
        }

        let threats = this.getSystemThreats();
        let complianceSetsData = this.getComplianceSetsData();
        let hasModellingErrors = this.getHasModellingErrors();

        let controlSetsMap = {};
        this.props.model.controlSets.forEach(cs => controlSetsMap[cs.uri] = cs);
        
        return (
            <div style={{width: "100%", height: "100%", zIndex: 0}}>
                <LoadingOverlay modelId={this.props.model["id"]} loadingId={this.props.model["loadingId"]} isValidating={this.props.model.validating}
                                isValid={this.props.model.valid} validationProgress={this.props.validationProgress}
                                hasModellingErrors={hasModellingErrors}
                                isCalculatingRisks={this.props.model.calculatingRisks}
                                isCalculatingRecommendations={this.props.model.calculatingRecommendations}
                                recommendationsJobId={this.props.recommendationsJobId}
                                isDroppingInferredGraph={this.props.isDroppingInferredGraph}
                                isLoading={this.props.loading.model} loadingProgress={this.props.loadingProgress}
                                dispatch={this.props.dispatch}/>

                <ThreatEditor
                    isVisible={this.props.selectedAsset["isThreatEditorVisible"]}
                    isActive={this.props.selectedAsset["isThreatEditorActive"]} // is window displayed at front
                    isAcceptancePanelActive={this.props.isAcceptancePanelActive}
                    threat={selectedThreat}
                    selectedMisbehaviour={this.props.selectedMisbehaviour}
                    loadingCausesAndEffects={this.props.selectedAsset["loadingCausesAndEffects"]}
                    loadingRootCauses={this.props.selectedAsset["loadingRootCauses"]}
                    closeMenu={this.closeThreatEditor}
                    model={this.props.model}
                    isRelationDeletable={this.isRelationDeletable}
                    renderTrustworthinessAttributes={this.renderTrustworthinessAttributes}
                    dispatch={this.props.dispatch}
                    loading={this.props.loading}
                    authz={this.props.authz}
                    developerMode={this.props.developerMode}
                    />

                <RootCausesEditor
                    isActive={this.props.isMisbehaviourExplorerActive} // is window displayed at front
                    threatFiltersActive={this.props.threatFiltersActive}
                    dispatch={this.props.dispatch}
                    model={this.props.model}
                    getTwasForMisbehaviourSet={this.getTwasForMisbehaviourSet}
                    selectedMisbehaviour={this.props.selectedMisbehaviour}
                    attackPaths={this.props.attackPaths}
                    selectedThreat={this.props.selectedThreat}
                    loadingCausesAndEffects={this.props.selectedAsset["loadingCausesAndEffects"]}
                    show={this.props.isMisbehaviourExplorerVisible}
                    onHide={this.closeMisbehaviourExplorer}
                    hoverThreat={this.hoverThreat}
                    loading={this.props.loading}
                    authz={this.props.authz}
                    developerMode={this.props.developerMode}
                    />

                <ComplianceExplorer
                    isActive={this.props.isComplianceExplorerActive} // is window displayed at front
                    threatFiltersActive={this.props.threatFiltersActive}
                    dispatch={this.props.dispatch}
                    model={this.props.model}
                    complianceSetsData={complianceSetsData}
                    selectedThreat={this.props.selectedThreat}
                    show={this.props.isComplianceExplorerVisible}
                    onHide={this.closeComplianceExplorer}
                    hoverThreat={this.hoverThreat}
                    loading={this.props.loading}
                    authz={this.props.authz}
                    />

                <ControlExplorer
                    selectedAsset={this.props.selectedAsset}
                    //isActive={this.props.isControlExplorerActive} // is window displayed at front
                    model={this.props.model}
                    show={this.props.isControlExplorerVisible}
                    onHide={this.closeControlExplorer}
                    hoverThreat={this.hoverThreat}
                    getAssetType={this.getAssetType}
                    loading={this.props.loading}
                    dispatch={this.props.dispatch}
                    authz={this.props.authz}
                />

                <ControlStrategyExplorer
                    selectedAsset={this.props.selectedAsset}
                    selectedControlStrategy={this.props.selectedControlStrategy}
                    csgExplorerContext={this.props.csgExplorerContext}
                    isActive={this.props.isControlStrategyExplorerActive} // is window displayed at front
                    threatFiltersActive={this.props.threatFiltersActive}
                    dispatch={this.props.dispatch}
                    model={this.props.model}
                    controlSets={controlSetsMap}
                    show={this.props.isControlStrategyExplorerVisible}
                    onHide={this.closeControlStrategyExplorer}
                    hoverThreat={this.hoverThreat}
                    getAssetType={this.getAssetType}
                    loading={this.props.loading}
                    authz={this.props.authz}
                />

                <RecommendationsExplorer
                    selectedAsset={this.props.selectedAsset}
                    isActive={this.props.isRecommendationsExplorerActive} // is window displayed at front
                    recommendations={this.props.recommendations}
                    show={this.props.isRecommendationsExplorerVisible}
                    onHide={this.closeRecommendationsExplorer}
                    loading={this.props.loading}
                    dispatch={this.props.dispatch}
                />

                <Canvas ref="tile-canvas"
                        model={this.props.model}
                        movedAsset={this.props.movedAsset}
                        groups={this.props.groups}
                        grouping={this.props.grouping}
                        canvas={this.props.canvas}
                        loading={this.props.loading}
                        isAssetDisplayed={this.isAssetDisplayed}
                        isRelationDisplayed={this.isRelationDisplayed}
                        selectedLayers={this.props.selectedLayers}
                        selectedAsset={this.props.selectedAsset}
                        selectedThreat={this.props.selectedThreat}
                        selectedInferredAsset={this.props.selectedInferredAsset}
                        view={this.props.view}
                        suppressCanvasRefresh={this.props.suppressCanvasRefresh}
                        redrawRelations={this.props.redrawRelations}
                        getAssetType={this.getAssetType}
                        isAssetNameTaken={this.isAssetNameTaken}
                        isGroupNameTaken={this.isGroupNameTaken}
                        sidePanelActivated={this.props.sidePanelActivated}
                        dispatch={this.props.dispatch}
                        authz={this.props.authz}
                        />

                {this.props.authz.userEdit ? (
                    <AssetPane
                        paletteAssets={this.props.model.palette.assets}
                        isAssetDisplayed={this.isAssetDisplayed}
                        selectedLayers={this.props.selectedLayers}
                        handleTileCreation={this.handleTileCreation}
                        getAssetCount={this.getAssetCount}
                    />
                ) : (
                    <div></div>
                )}

                <DetailPane selectedAsset={this.props.selectedAsset}
                            selectedThreat={this.props.selectedThreat}
                            selectedMisbehaviour={this.props.selectedMisbehaviour}
                            expanded={this.props.expanded}
                            filters={this.props.filters}
                            loading={this.props.loading}
                            getAssetType={this.getAssetType}
                            getAssetsForType={this.getAssetsForType}
                            getLink={this.getLink}
                            isAssetNameTaken={this.isAssetNameTaken}
                            model={this.props.model}
                            threats={threats}
                            complianceSetsData={complianceSetsData}
                            hasModellingErrors={hasModellingErrors}
                            hoverThreat={this.hoverThreat}
                            isRelationDeletable={this.isRelationDeletable}
                            renderTrustworthinessAttributes={this.renderTrustworthinessAttributes}
                            dispatch={this.props.dispatch}
                            backEnabled={this.props.backEnabled}
                            forwardEnabled={this.props.forwardEnabled}
                            sidePanelActivated={this.props.sidePanelActivated}
                            authz={this.props.authz}
                />

                <ReportDialog model={this.props.model}
                            reportType={this.props.reportType}
                            show={this.props.isReportDialogVisible}
                            onHide={this.closeReportDialog}
                            getAssetType={this.getAssetType}
                />

                {this.props.loading.newFact.length > 0 && <div className="creation-overlay visible"><span
                    className="fa fa-refresh fa-spin fa-3x fa-fw"/><h1>Creating new {this.props.loading.newFact[0]}...</h1>
                </div>}
            </div>
        );
    }

    //Get TWAS associated with this MS (if any)
    getTwasForMisbehaviourSet(msUri) {
        let twasUri = this.props.misbehaviourTwas[msUri];
        let twas = twasUri ? this.props.model.twas[twasUri] : null;
        return twas;
    }

    // Get system threats. Filter out untriggered threats unless in developer mode
    getSystemThreats() {
        //console.log("getSystemThreats: this.props.model.threats:", this.props.model.threats);
        if (this.props.developerMode) {
            // include untriggered threats
            return this.props.model.threats;
        }
        else {
            // filter out untriggered threats
            return this.props.model.threats.filter(threat => !threat.triggerable || (threat.triggerable && threat.triggered));
        }
    }

    // The complianceSet in the model looks like, e.g.:
    // "complianceSets" : [ {
    //     "uri" : "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#GDPR",
    //     "label" : "GDPR",
    //     "description" : "GDPR compliance threats.",
    //     "systemThreats" : [ ],
    //     "compliant" : true,
    //     "id" : "12ec1daf"
    //   }, {
    //     "uri" : "http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain#Anomalies",
    //     "label" : "Modelling Errors",
    //     "description" : "Threats corresponding to combinations of assets and possibly controls that are inconsistent or may represent a system modelling oversight.",
    //     "systemThreats" : [ list of URIs ],
    //     "compliant" : true,
    //     "id" : "19497687"
    //   } ],
    getComplianceSetsData() {
        let modelComplianceSets = this.props.model.complianceSets;
        //console.log("getComplianceSetsData: modelCmplianceSets: ", modelCmplianceSets);
        
        let allComplianceSets = modelComplianceSets.map(modelComplianceSet => {
            let complianceThreats = this.getComplianceThreats(modelComplianceSet.systemThreats);
            let nCompThreats = complianceThreats.length;
            let treatedThreats = complianceThreats.filter((threat) => threat["resolved"]);
            let nTreatedThreats = treatedThreats.length;
            // If we treat a threat then nTreatedThreats will increment but the "compliant" property will not (until revalidation).
            // Therefore we need to look at both these things to determine compliance.
            let compliant = modelComplianceSet.compliant || nCompThreats === 0 || (nTreatedThreats === nCompThreats);
            
            return {...modelComplianceSet,
                complianceThreats: complianceThreats,
                nCompThreats: nCompThreats,
                treatedThreats: treatedThreats,
                nTreatedThreats: nTreatedThreats,
                compliant: compliant
            }
        })

        let complianceSets = allComplianceSets.filter(cs => cs.uri !== Constants.MODELLING_ERRORS_URI);  // all but the modelling error compliance set
        let modellingErrorsCS = allComplianceSets.find(cs => cs.uri === Constants.MODELLING_ERRORS_URI);  // just the modelling error compliance set
        let nCompliant = complianceSets.filter(cs => cs.compliant == true).length;  // number of non-modelling error compliance sets that are compliant

        let complianceSetsData = {
            complianceSets: complianceSets,
            nComplianceSets: complianceSets.length,
            nCompliant: nCompliant,
            modellingErrors: modellingErrorsCS
        }
        
        return complianceSetsData;
    }
    
    getComplianceThreats(complianceThreats) {
        let updatedThreats = this.updateThreats(complianceThreats);
        //console.log("updatedThreats:", updatedThreats);
        
        // (only include if NOT untriggered);
        let filteredThreats = updatedThreats.filter(t => !t.untriggered);
        //console.log("filteredThreats:", filteredThreats);
        
        return filteredThreats;
    }

    updateThreats(threatUris) {
        //console.log("updateThreats: ", threatUris);

        if (threatUris === undefined) return [];

        return threatUris.map(threatUri => {
            //console.log(threatUri);
            let modelThreat;

            if (threatUri) {
                modelThreat = this.getComplianceThreatByUri(threatUri);
                if (modelThreat === undefined) {
                    console.log("WARNING: threat does not exist: " + threatUri);
                    //return threat;
                }

                return modelThreat;
            }
        });
    }

    getComplianceThreatByUri(threatUri) {
        //console.log("getComplianceThreatByUri: " + threatUri);
        let threat = this.props.model.complianceThreats.find(threat => threat.uri === threatUri);
        
        //get threat status and determine if it is UNTRIGGERED (so it may be filtered out, etc)
        let status = getThreatStatus(threat, this.props.model.controlStrategies);
        //console.log("status: ", status);
        let untriggered = status.includes("UNTRIGGERED");
        //console.log("untriggered: ", untriggered);
        threat.untriggered = untriggered;
        
        return threat;
    }
    
    isAssetNameTaken(name, newName) {
        return this.props.model["assets"].find(asset => newName === asset["label"]) === undefined;
    }
    
    getModellingErrorThreats() {
        let modellingErrorThreats = [];
        let modellingErrorsCS = this.props.model.complianceSets.find(cs => cs.uri === Constants.MODELLING_ERRORS_URI);
        //console.log("getSelectedThreat: modelling errors compliance set: ", modellingErrorsCS);

        if (modellingErrorsCS && modellingErrorsCS.systemThreats) {
            modellingErrorThreats = modellingErrorsCS.systemThreats;
        }
        
        return modellingErrorThreats;
    }

    getHasModellingErrors() {
        let compliant = false;
        let modellingErrorsCS = this.props.model.complianceSets.find(cs => cs.uri === Constants.MODELLING_ERRORS_URI);
        if (modellingErrorsCS) {
            compliant = modellingErrorsCS.compliant;
        }
        return !compliant;
    }

    isGroupNameTaken(name, newName) {
        return this.props.groups.find((group) => newName === group["label"]) === undefined;
    }

    getSelectedThreat() {
        let selectedThreatId = this.props.selectedThreat["id"];
        //console.log("getSelectedThreat(): ", selectedThreatId);

        if (selectedThreatId === "") {
            return null;
        }

        let threat = this.props.model.threats.find((threat) => threat["id"] === selectedThreatId);
        //console.log("selected threat:", threat);

        if (threat) {
            threat.isComplianceThreat = false;
            threat.isModellingError = false;
        }
        else {
            // console.log("Threat {} not found in threats list. Checking compliance threats...", selectedThreatId);
            threat = this.props.model.complianceThreats.find((threat) => threat["id"] === selectedThreatId);
            threat.isComplianceThreat = true;
            //console.log("selected compliance threat:", threat);
            
            //get current modelling errors (threats)
            let modellingErrorThreats = this.getModellingErrorThreats();
            
            //check if this threat is in the list
            threat.isModellingError = modellingErrorThreats.includes(threat.uri);
        }

        let populatedThreat = this.populateThreatMisbehaviours(threat);
        //console.log("populated threat:", populatedThreat);

        return populatedThreat;
    }

    populateThreatMisbehaviours(threat) {
        //get URIs of misbehaviours
        let causeURIs = threat.secondaryEffectConditions;
        //let effectURIs = threat.misbehaviours;

        //TODO: we may want to use the misbehaviours field, which contains ALL possible misbehaviours, whether active or not
        //then perhaps grey out the ones that are not active?

        //let effectURIs = threat.directEffects;
        let effectURIs = threat.misbehaviours; //see comment above!
        let secondaryEffectURIs = threat.indirectEffects;

        let causes = causeURIs ? causeURIs.map((uri) => {
            return this.props.model.misbehaviourSets[uri];
        }) : [];

        let effects = effectURIs ? effectURIs.map((uri) => {
            return this.props.model.misbehaviourSets[uri];
        }) : [];

        let secondaryEffects = secondaryEffectURIs ? secondaryEffectURIs.map((uri) => {
            return this.props.model.misbehaviourSets[uri];
        }) : [];

        let populatedThreat = {...threat,
            secondaryEffectConditions: causes,
            misbehaviours: effects,
            effects: secondaryEffects
        };

        return populatedThreat;
    }

    hoverThreat(show, threat) {
        //this.props.selectedThreatVisibility = show;
        //console.log(threat);
        //console.log("threat.pattern: ", threat.pattern);
        //console.log(this.props.model);
        //console.log(this.props.model.assets);
        //console.log("hoverThreat: ", show);
        setHoveringThreat(show);
        //console.log("hoveringThreat: ", hoveringThreat);

        if ((threat.pattern) && (threat.pattern.nodes)) {

            //first unhighlight asset tiles and hide role tiles
            $("#tile-canvas").find(".tile").removeClass("pattern-hover");
            $("#tile-canvas").find("div.tile.role-tile").hide();

            //highlight each node
            threat.pattern.nodes.map((n) => {
                //get asset id
                var a = this.props.model.assets.find(function (a) {
                    return a.uri === n.asset;
                });

                //add or remove class
                if (show === true && a) {
                    var assetTile = $("#tile-canvas").find("#tile-" + a.id).first();
                    assetTile.addClass("pattern-hover");
                    var roleTile = assetTile.find("div.tile.role-tile").first();
                    roleTile.find("label").text(n.roleLabel);
                    roleTile.show();
                }
            });

            if (show === true) {
                //KEM: Don't see the reason for using a timer here, and it also causes unpredictable
                //problems, such as highlighted relations not being clearer properly after "unhover"
                //TODO: remove these commented lines, tidy up and consolidate with almost identical code
                //in the PatternPanel class
                //let timer = 10;
                //setTimeout(() => {getPlumbingInstance().batch(() => {
                    let hConns = [];
                    //highlight all links
                    threat.pattern.links.map((link) => {
                        var assetFrom = this.props.model["assets"].find((mAsset) => mAsset["uri"] === link["fromAsset"]);
                        var assetTo = this.props.model["assets"].find((mAsset) => mAsset["uri"] === link["toAsset"]);
                        if (!assetFrom || !assetTo) return;
                        getPlumbingInstance().select(
                            {
                                source: "tile-" + assetFrom["id"],
                                target: "tile-" + assetTo["id"],
                                scope: ["relations", "inferred-relations"]
                            }, true
                        ).each((conn) => {
                            let relation = {
                                label: link["typeLabel"],
                                fromID: assetFrom["id"],
                                toID: assetTo["id"]
                            };
                            let connLabel = conn.getOverlay("label");
                            let connEl = connLabel.getElement();
                            if (connEl.innerHTML === relation["label"]) {
                                connEl.className += ' pattern-hover';
                                let currType = conn.getType();
                                let originalType = [...currType];
                                conn.setType("pattern");
                                
                                var hoveredConn = {
                                    conn:  conn,
                                    labelEl: connEl,
                                    originalType: originalType
                                };
                                
                                hConns.push(hoveredConn);
                            }
                        });
                    });
                    setHoveredConns(hConns);
                //})}, timer); //see comment above
            }
            else {
                //console.log("Resetting hoveredConns: ", hoveredConns);
                hoveredConns.map((hoveredConn) => {
                    //console.log("Unhighlighting conn: " + hoveredConn);
                    let conn = hoveredConn.conn;
                    let labelEl = hoveredConn.labelEl;
                    let originalType = hoveredConn.originalType;
                    labelEl.classList.remove("pattern-hover");
                    conn.setType(originalType.join(" "));
                });

                //Finally, clear the list of hovered conns
                setHoveredConns([]);
            }
        }
        else if (! threat.pattern) {
            console.log("WARNING: no pattern defined for threat: ", threat);
        }
        else if (! threat.pattern.nodes) {
            console.log("WARNING: no pattern.nodes defined for threat: ", threat);
        }
    }

    isRelationDeletable(rel) {
        //KEM - ideally we should use the immutable flag too, however this is not properly being set by the server
        //let deletable = !rel["immutable"] || rel["asserted"];
        //console.log(rel);
        let deletable = rel["asserted"];
        return deletable;
    }

    closeThreatEditor() {
        this.props.dispatch(toggleThreatEditor(false, ""));
    }

    closeComplianceExplorer() {
        this.props.dispatch(closeComplianceExplorer());
    }

    closeControlExplorer() {
        this.props.dispatch(closeControlExplorer());
    }

    closeControlStrategyExplorer() {
        this.props.dispatch(closeControlStrategyExplorer());
    }

    closeRecommendationsExplorer() {
        this.props.dispatch(closeRecommendationsExplorer());
    }

    closeMisbehaviourExplorer() {
        this.props.dispatch(closeMisbehaviourExplorer());
    }

    closeReportDialog() {
        this.props.dispatch(closeReportDialog());
    }

    /**
     * AssertedAsset Creation: Called through PaletteAsset -> AssetList -> AssetPanel -> Modeller (dispatched)
     * @param assetTypeId The asset type to use in the creation.
     * @param x The x position of the new tile.
     * @param y The y position of the new tile.
     */
    handleTileCreation(paletteAsset, x, y) {
        // Dispatch the new asset to the backend to have it checked.
        //console.log("handleTileCreation:", paletteAsset);
        let label = paletteAsset.label ? paletteAsset.label : "asset";
        
        let asset = {
            "label": label + "_" + Math.random().toString(36).substring(2, 7),
            "type": paletteAsset.id,
            "asserted": true,
            "visible": true,
            "iconX": x,
            "iconY": y,
            "population": Constants.ASSET_DEFAULT_POPULATION
        };
        this.props.dispatch(postAssertedAsset(this.props.model.id, asset, x, y));
    }

    /**
     * Used to collect details on an asserted asset"s type from the palette. I.e. the icon URL
     * @param type The ID to look up in the palette.
     * @returns {*} The JSON of the Asset Type found.
     */
    getAssetType(type) {
        //console.log('getAssetType: type = ' + type);
        if (type === "") {
            return {
                id: "0",
                label: "",
                type: "",
                subtype: "",
                description: "",
                icon: ""
            };
        }
        for (var i = 0; i < this.props.model.palette.assets.length; i++) {
            var asset = this.props.model.palette.assets[i];
            if (asset.id === type) {
                return asset;
            }
        }
        return null;
    }


    /**
     * Used for example by IncomingRelationsPanel, in order to populate drop-down list of assets to connect to
     * when creating a missing relation
     */
    getAssetsForType(linkTo) {
        //console.log("getAssetsForType:", linkTo);

        //finding all possible assets that can be used with this link type
        let linkToType = linkTo; //now uses full URL, not fragment!
        //console.log("linkToType:", linkToType);

        //palette assets
        //console.log("palette assets:", this.props.model.palette.assets);

        //get list of all suitable palette assets
        let paletteAssetOptions = this.props.model.palette.assets.filter((asset) => {
            //check if requested type is in the palette asset type list (may be a supertype, for example)
            return asset["type"].indexOf(linkToType) >= 0;
        });

        //console.log("paletteAssetOptions:", paletteAssetOptions);

        //set initial option to requested type
        let options = [linkTo];

        //from candidate palette assets, get their specific types
        paletteAssetOptions.map((paletteAsset, index) => {
            //console.log(paletteAsset.id);
            if (options.indexOf(paletteAsset.id) < 0) {
                //add this type to the list of options
                options.push(paletteAsset.id);
            }
        });

        //options now contains all asset types that either match the requested type, or where the type is in palette asset type list
        //console.log("options", options);

        //filter all assets and get only the ones that match one of the options above
        const assets = this.props.model["assets"].filter((asset) => {
            //console.log(asset["type"]);
            return options.indexOf(asset["type"]) >= 0;
        });

        //console.log("Matched assets:", assets);

        return assets;
    }

    getAssetCount(uri) {
        return this.props.model.assets.filter(asset => asset.type === uri).length;
    }

    /**
     * Returns the asset type of a specific asset ID.
     * @param id The ID of the asset to check.
     * @returns {{id, name, type, subtype, description, icon}|*} The asset type of the specified asset.
     */
    getAssetTypeById(id) {
        let asset = this.props.model.assets.find((asset) => {
            return asset.id === id;
        });

        if (asset !== undefined) {
            return this.getAssetType(asset.type);
        }
    }

    /**
     * Function to check if an asset is described in the current view.
     * @param assetType The asset type to check
     * @returns {boolean} True if visible; false if not.
     */
    isAssetDisplayed(assetType) {
        var self = this;

        if (this.props.selectedLayers.indexOf("all") > -1) {
            return true;
        }

        if (this.props.layers.find((layer) => {
                if (self.props.selectedLayers.indexOf(layer["name"]) > -1) {
                    if (layer["assets"].indexOf(assetType["id"]) > -1 || layer["assets"].indexOf(assetType["type"]) > -1) {
                        return true;
                    }
                }
            }) !== undefined) {
            return true;
        }
        return false;
    }

    /**
     * Function to check if a relationship is described in the current view.
     * @param relation The relationship to check
     * @returns {boolean} True if visible; false if not.
     */
    isRelationDisplayed(relation) {
        return true;
        /*var self = this;

        if (this.props.selectedLayers.indexOf("all") > -1) {
            return true;
        }

        if (this.props.layers.find((layer) => {
                if (self.props.selectedLayers.indexOf(layer["name"]) > -1 && layer["relationships"] !== undefined) {
                    var connType = self.getFragment(relation["type"]);
                    var fromAsset = self.getAssetTypeById(relation["fromID"]);
                    var toAsset = self.getAssetTypeById(relation["toID"]);

                    if (layer["relationships"].indexOf(fromAsset["name"] + " " + connType + " " + toAsset["name"]) > -1
                        || layer["relationships"].indexOf(fromAsset["name"] + " " + connType + " " + toAsset["type"]) > -1
                        || layer["relationships"].indexOf(fromAsset["type"] + " " + connType + " " + toAsset["name"]) > -1
                        || layer["relationships"].indexOf(fromAsset["type"] + " " + connType + " " + toAsset["type"]) > -1) {
                        return true;
                    }
                }
            }) !== undefined) {
            return true;
        }
        return false;*/
    }
    
    getLink(assetType, relType, direction) {
        let link = undefined;
        //console.log("getLink for ", assetType, relType, direction);
        if (this.props.model.palette.links) {
            let linkTypes = this.props.model.palette.links[assetType];
            //console.log("linkTypes:", linkTypes);
            if (linkTypes) {
                let links = linkTypes[direction];
                //console.log("possible links:", links);
                if (links) {
                    link = links.find(link => link.type === relType);
                }
            }
        }
        
        if (! link) {
            link = {label: "unknown", type: relType};
        }
        
        //console.log("found link:", link);
        return link;
    }

    getMisbehaviour(id) {
        return this.props.model.misbehaviourSets[id];
    }

    renderTrustworthinessAttributes(attributes, levels, self, showInvisibleTwas) {
        return (
            <div>
                <div key={0} className="row head">
                    <span className="col-xs-7">
                        Attribute at Asset
                    </span>
                    <span className="col-xs-3 impact">
                        Assumed
                    </span>
                    <span className="col-xs-2 inferred-tw">
                        Calculated
                    </span>
                </div>
                {attributes.map((field, index) => {
                    let updating = self.state.updating[field.label];
                    let twas = self.state.twas[field.label];
                    
                    //Is TWAS visible?
                    let visible = twas["visible"];

                    if (!showInvisibleTwas && (visible !== undefined) && !visible) {
                        return;
                    }

                    //here, we reverse color map by setting reverseColours = true (as high trustworthiness is good!)
                    let reverseColours = true;
                    let currentRender = (twas !== undefined) ? getRenderedLevelText(levels, twas.inferredTWLevel, reverseColours) : "";
                    let causingMisbehaviourSet = (twas !== undefined) ? twas.causingMisbehaviourSet : null;
                    let misbehaviour = this.getMisbehaviour(causingMisbehaviourSet);
                    
                    //is misbehaviour selected?
                    let selected = this.props.selectedMisbehaviour.misbehaviour && misbehaviour && this.props.selectedMisbehaviour.misbehaviour.id === misbehaviour.id;

                    //show revert twas level button only if the level has been asserted by the user
                    let showRevertButton = twas.twLevelAsserted;

                    return (
                        <div
                            key={index + 1}
                            className={`row detail-info ${selected ? "selected-row" : "row-hover"}`}>
                            <span className="col-xs-7">
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                                trigger={["hover"]}
                                                overlay={
                                                    <Tooltip id={"related-misbehaviour-" + index + 1}
                                                             className={"tooltip-overlay"}>
                                                        { twas.attribute.description }
                                                    </Tooltip>
                                                }>
                                    <span className={"misbehaviour" + (selected ? "" : " clickable")}
                                          onClick={() => self.openMisbehaviourExplorer(misbehaviour)}>
                                        {field.label}
                                    </span>
                                </OverlayTrigger>
                            </span>
                            <span className="col-xs-3 impact">
                                <FormControl
                                    disabled={!this.props.authz.userEdit}
                                    componentClass="select"
                                    className="impact-dropdown level"
                                    id={field.label}
                                    value={twas.assertedTWLevel.uri}
                                    style={{ backgroundColor: getLevelColour(levels, twas.assertedTWLevel, reverseColours) }}
                                    onChange={self.twValueChanged}
                                    ref="select-initial-tw">
                                    {levels.map((level, index) =>
                                        <option key={index + 1}
                                            value={level.uri}
                                            style={{ backgroundColor: getLevelColour(levels, level, reverseColours) }}>
                                            {level.label}
                                        </option>
                                    )};
                                </FormControl>
                                {updating ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw" /> : null}
                                &nbsp;
                                <span style={{cursor: "pointer", display: showRevertButton ? "inline-block" : "none"}} className="fa fa-undo undo-button" 
                                    onClick={((e) => {
                                        self.onClickRevertTwasLevel(twas);
                                    })}
                                />
                            </span>
                            <span className="inferred-tw col-xs-2">
                                {currentRender}
                            </span>
                        </div>
                    );
                })}
            </div>
        )
    }
}

/**
 * This connects the smart component to the Redux store.
 * @param state This represents the Redux store as a whole.
 * @returns {{model: (*|model|{assets}|modelState.model|{id, name, palette, assets, relations}|{relations})}} This
 * maps the associated object to this component"s props. Here we can see the potential properties.
 */
var mapStateToProps = function (state) {
    return {
        model: state.modeller.model,
        recommendationsJobId: state.modeller.recommendationsJobId,
        recommendations: state.modeller.recommendations,
        movedAsset: state.modeller.movedAsset,
        groups: state.modeller.groups,
        grouping: state.modeller.grouping,
        canvas: state.modeller.canvas,
        layers: state.modeller.model.palette.layers,
        selectedLayers: state.modeller.selectedLayers,
        upload: state.modeller.upload,
        selectedAsset: state.modeller.selectedAsset,
        selectedControlStrategy: state.modeller.selectedControlStrategy,
        csgExplorerContext: state.modeller.csgExplorerContext,
        selectedThreat: state.modeller.selectedThreat,
        selectedMisbehaviour: state.modeller.selectedMisbehaviour,
        attackPaths: state.modeller.attackPaths,
        expanded: state.modeller.expanded,
        filters: state.modeller.filters,
        misbehaviourTwas: state.modeller.misbehaviourTwas,
        isMisbehaviourExplorerVisible: state.modeller.isMisbehaviourExplorerVisible,
        isMisbehaviourExplorerActive: state.modeller.isMisbehaviourExplorerActive,
        isComplianceExplorerVisible: state.modeller.isComplianceExplorerVisible,
        isComplianceExplorerActive: state.modeller.isComplianceExplorerActive,
        isControlExplorerVisible: state.modeller.isControlExplorerVisible,
        isControlExplorerActive: state.modeller.isControlExplorerActive,
        isControlStrategyExplorerVisible: state.modeller.isControlStrategyExplorerVisible,
        isControlStrategyExplorerActive: state.modeller.isControlStrategyExplorerActive,
        isRecommendationsExplorerVisible: state.modeller.isRecommendationsExplorerVisible,
        isRecommendationsExplorerActive: state.modeller.isRecommendationsExplorerActive,
        isReportDialogVisible: state.modeller.isReportDialogVisible,
        isReportDialogActive: state.modeller.isReportDialogActive,
        threatFiltersActive: state.modeller.threatFiltersActive,
        isAcceptancePanelActive: state.modeller.isAcceptancePanelActive,
        loading: state.modeller.loading,
        reportType: state.modeller.reportType,
        selectedInferredAsset: state.modeller.selectedInferredAsset,
        developerMode: state.modeller.developerMode,
        view: state.modeller.view,
        suppressCanvasRefresh: state.modeller.suppressCanvasRefresh,
        redrawRelations: state.modeller.redrawRelations,
        isDroppingInferredGraph: state.modeller.isDroppingInferredGraph,
        validationProgress: state.modeller.validationProgress,
        loadingProgress: state.modeller.loadingProgress,
        backEnabled: state.modeller.backEnabled,
        forwardEnabled: state.modeller.forwardEnabled,
        sidePanelActivated: state.modeller.sidePanelActivated,
        authz: state.modeller.authz,
    }
};

/**
 * This is specifying the prop types. This reduces the manual error checking required.
 * @type {{model: *, selectedAsset: *, dispatch: *}} Types specified in JSON format.
 */
Modeller.propTypes = {
    model: PropTypes.object,
    recommendationsJobId: PropTypes.string,
    recommendations: PropTypes.object,
    movedAsset: PropTypes.bool,
    groups: PropTypes.array,
    grouping: PropTypes.object,
    canvas: PropTypes.object,
    layers: PropTypes.array,
    selectedLayers: PropTypes.array,
    upload: PropTypes.object,
    selectedAsset: PropTypes.object,
    selectedControlStrategy: PropTypes.array,
    csgExplorerContext: PropTypes.object,
    selectedThreat: PropTypes.object,
    selectedMisbehaviour: PropTypes.object,
    attackPaths: PropTypes.object,
    expanded: PropTypes.object,
    filters: PropTypes.object,
    misbehaviourTwas: PropTypes.object,
    isControlExplorerVisible: PropTypes.bool,
    isControlExplorerActive: PropTypes.bool,
    isControlStrategyExplorerVisible: PropTypes.bool,
    isControlStrategyExplorerActive: PropTypes.bool,
    isMisbehaviourExplorerVisible: PropTypes.bool,
    isRecommendationsExplorerVisible: PropTypes.bool,
    isRecommendationsExplorerActive: PropTypes.bool,
    isMisbehaviourExplorerActive: PropTypes.bool,
    isReportDialogVisible: PropTypes.bool,
    isReportDialogActive: PropTypes.bool,
    threatFiltersActive: PropTypes.object,
    isAcceptancePanelActive: PropTypes.bool,
    reportType: PropTypes.string,
    loading: PropTypes.object,
    selectedInferredAsset: PropTypes.string,
    developerMode: PropTypes.bool,
    view: PropTypes.object,
    suppressCanvasRefresh: PropTypes.bool,
    redrawRelations: PropTypes.number,
    dispatch: PropTypes.func,
    isDroppingInferredGraph: PropTypes.bool,
    validationProgress: PropTypes.object,
    loadingProgress: PropTypes.object,
    backEnabled: PropTypes.bool,
    jsPlumbReady: PropTypes.bool,
    sidePanelActivated: PropTypes.bool,
    forwardEnabled: PropTypes.bool,
    authz: PropTypes.object,
};

/* We export the smart component with a connection to the Redux store. */
export default connect(mapStateToProps)(Modeller);
