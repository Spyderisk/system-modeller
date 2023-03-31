import React, {Fragment} from "react";
import PropTypes from "prop-types";
import AddRelationModal from "./popups/AddRelationModal";
import EditAssetTypeModal from "./popups/EditAssetTypeModal";
import DetailsAccordion from "./accordion/DetailsAccordion";
import SlidingPanel from "../../../../common/components/slidingpanel/SlidingPanel";
import {Button, ButtonToolbar, OverlayTrigger, Tooltip, Panel} from "react-bootstrap";
import * as Constants from "../../../../common/constants.js";
import Input from "../../util/Input";
import {Portal} from "react-portal";
import ModelSummary from "./ModelSummary";
import {
    assetHistoryBack,
    assetHistoryForward,
    postAssertedRelation,
    putAssertedAssetPopulation,
    putAssertedAssetType
} from "../../../actions/ModellerActions";
import {renderPopulationLevel} from "../../util/Levels";

class DetailPane extends React.Component {

    constructor(props) {
        super(props);

        this.isRelationExists = this.isRelationExists.bind(this);

        //this.handleSlide = this.handleSlide.bind(this);
        this.handleAdd = this.handleAdd.bind(this);
        this.populationValueChanged = this.populationValueChanged.bind(this);

        this.openAddRelationModal = this.openAddRelationModal.bind(this);
        this.submitAddRelationModal = this.submitAddRelationModal.bind(this);
        this.closeAddRelationModal = this.closeAddRelationModal.bind(this);

        this.editType = this.editType.bind(this);
        this.openEditAssetTypeModal = this.openEditAssetTypeModal.bind(this);
        this.submitEditAssetTypeModal = this.submitEditAssetTypeModal.bind(this);
        this.closeEditAssetTypeModal = this.closeEditAssetTypeModal.bind(this);

        this.forwardEnabled = false;
        this.backEnabled = false;
    }

    /**
     * React Lifecycle Methods
     */

    componentWillMount() {
        //console.log("componentWillMount");
        this.setState({
            asset: {
                id: "",
                label: "",
                type: "",
                description: "",
                iconX: 0,
                iconY: 0
            },
            editAssetTypeModal: {
                show: false
            },
            RelationModal: {
                show: false,
                assets: [],
                relation: {},
                host: {},
                isFrom: true
            },
            addRelationModal: {
                show: false,
                links: [],
                host: {},
                isIncoming: true
            },
            updatingPopulation: false,
            backEnabled: false,
            forwardEnabled: false
        });
    }

    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps:", nextProps);
        if(this.refs!=null && this.refs.btnback!=null && this.refs.btnforward!=null){
            this.refs.btnback.disabled = !nextProps.backEnabled;
            this.refs.btnforward.disabled = !nextProps.forwardEnabled;
        }

        //this.setState({
        //    backEnabled: nextProps.backEnabled,
        //    forwardEnabled: nextProps.forwardEnabled
        //  })
        
        let hasSelectedAsset = (nextProps.selectedAsset["id"] !== "");
        let asset;
        
        if (hasSelectedAsset) {
            asset = nextProps.model["assets"].find((asset) => {
                if (asset["id"] === nextProps.selectedAsset["id"]) {
                    return asset;
                }
            }, this);
            if (!asset) console.warn("Asset does not exist: " + nextProps.selectedAsset["id"]);
        }

        if (!hasSelectedAsset || !asset) {
            this.setState({
                ...this.state,
                asset: {
                    id: "",
                    label: "",
                    type: "",
                    description: "",
                    iconX: 0,
                    iconY: 0
                },
                updatingPopulation: false,
                backEnabled: nextProps.backEnabled,
                forwardEnabled: nextProps.forwardEnabled
            });
            return;
        }
        if (asset) {
            //console.log("this.state:", this.state);
            let showAssetTypeModal = this.state.editAssetTypeModal.show;
            //console.log("showssetTypeModal = " + showssetTypeModal);
            if (nextProps.loading.asset) {
                console.log("Asset loading detected - closing asset type modal..");
                showAssetTypeModal = false;
                //console.log("showssetTypeModal = " + showssetTypeModal);
            }
            this.setState({
                ...this.state,
                asset: asset,
                editAssetTypeModal: {
                    show: showAssetTypeModal
                },
                updatingPopulation: false,
                backEnabled: nextProps.backEnabled,
                forwardEnabled: nextProps.forwardEnabled
            })

        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        //console.log("DetailPane.shouldComponentUpdate");
        let shouldComponentUpdate = true;
        //console.log("this.props:", this.props);
        //console.log("nextProps: ", nextProps);
        //console.log("this.state:", this.state);
        //console.log("nextState: ", nextState);

        //console.log("this.state.addRelationModal:", this.state.addRelationModal);
        //console.log("nextState.addRelationModal: ", nextState.addRelationModal);

        if (nextProps.loading.model) {
            //console.log("DetailPane.shouldComponentUpdate: false: (model loading)");
            return false;
        }

        if (nextProps.model.validating) {
            //console.log("DetailPane.shouldComponentUpdate: false: (model validating)");
            return false;
        }

        //If only the loading prop has changed, no need to update (as this is required on main data change)
        /*
        console.log("this.props.loading:");
        console.log(this.props.loading);
        console.log("nextProps.loading:");
        console.log(nextProps.loading);
        let shouldComponentUpdate = this.props.loading !== nextProps.loading;
        console.log("DetailPane.shouldComponentUpdate: " + shouldComponentUpdate);
        //return shouldComponentUpdate;
        */

        //If we are loading causes and effects, no need to refresh DetailPane
        //console.log("this.props.selectedAsset.loadingCausesAndEffects: " + this.props.selectedAsset.loadingCausesAndEffects);
        //console.log("nextProps.selectedAsset.loadingCausesAndEffects: " + nextProps.selectedAsset.loadingCausesAndEffects);
        if (this.props.selectedAsset.loadingCausesAndEffects != nextProps.selectedAsset.loadingCausesAndEffects) {
            //console.log("DetailPane.shouldComponentUpdate: false: (loadingCausesAndEffects changed)");
            return false;
        }

        /*
        console.log("this.props:");
        console.log(this.props);
        console.log("nextProps: ");
        console.log(nextProps);
        */

        //console.log("DetailPane.shouldComponentUpdate: " + shouldComponentUpdate);
        return shouldComponentUpdate;
    }

    componentDidUpdate() {
        //console.log("componentDidUpdate: loading.newFact = ", this.props.loading.newFact);
        if (this.props.loading.newFact.length > 0) {
            //console.log("componentDidUpdate: calling closeAddRelationModal");
            this.closeAddRelationModal(); //is this sufficient? close all modals?
        }
    }

    /**
     * This renders our detail panel with any lists wanted.
     * @returns {XML} Returns the HTML that will be rendered to the Virtual DOM.
     */
    render() {
        let asset = this.state.asset;
        var assetType = this.props.getAssetType(asset["type"]);

        //Check assetType. If this is null, display default icon (this can happen if we are using the wrong palette, for example)
        var defaultIcon = "fallback.svg";
        var icon = assetType ? assetType["icon"] : defaultIcon;
        const icon_path = process.env.config.API_END_POINT + "/images/" + icon;

        if (assetType === null) {
            assetType = {};
            alert("ERROR: No palette icon for asset: " + asset["type"]);
        }

        var iconStyles = {
            backgroundImage: "url(" + icon_path + ")",
            backgroundSize: "contain",
            backgroundRepeat: "no-repeat",
            backgroundPosition: "center center",
            width: "50%",
            height: "100%",
            float: "right"
        };

        // TODO: this first panel needs to be put into an AssetDetalsPanel component and rendered from the DetailsAccordion
        // The embedded marginBottom style can then be removed

        let updatingPopulation = this.state.updatingPopulation
        let showRevertButton = false; //TODO: not sure if this will be required. Set as false for now

        if (!this.props.model.levels)
            return null;

        let populationLevels = this.props.model.levels["PopulationLevel"];
        let populationLevel = populationLevels.find((level) => level.uri === asset.population);

        let content = this.props.selectedAsset.id !== "" ?
            <div>
                <Panel bsStyle="primary" style={{marginBottom: "0px"}}>
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <span><i className="fa fa-info-circle " />Asset Details and Population</span>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <div className="description" style={{ flex: "0 0 100px", height: "100px", width: "100%" }}>
                                {icon !== "" ? <div style={iconStyles} /> : ""}
                                <div className="descriptor">
                                    <p>
                                        <strong>{assetType["label"] !== "" ? "Type: " : ""}</strong>
                                        {assetType["label"] === null ? "None" : assetType["label"] === "" ? "" :
                                            <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top" overlay={<Tooltip
                                                id="edit-asset-type-tooltip"><strong>{"Edit type"}</strong></Tooltip>}>
                                                {this.props.authz.userEdit ?
                                                    <strong>
                                                        <span className="asset-type clickable"
                                                            onClick={() => this.editType()}>
                                                            {assetType["label"]}
                                                        </span>
                                                    </strong>
                                                    :
                                                    <strong>{assetType["label"]}</strong>
                                                }
                                            </OverlayTrigger>
                                        }
                                    </p>
                                    <p>
                                        <strong>{assetType["description"] !== "" ? "Description: " : ""}</strong>{assetType["description"] === null ? "None" : assetType["description"]}
                                    </p>
                                </div>
                            </div>

                            <div className="population-div" style={{ flex: "0 0 50px" }}>
                                <div className="population-form">
                                    <strong>Population:</strong>
                                    &nbsp;
                                    {renderPopulationLevel(asset, populationLevel, populationLevels, this.props.authz.userEdit, updatingPopulation, this.populationValueChanged)}
                                    &nbsp;
                                    <span style={{cursor: "pointer", display: showRevertButton ? "inline-block" : "none"}} className="fa fa-undo undo-button" 
                                        onClick={((e) => {
                                            this.onClickRevertPopulationLevel(controlSet);
                                        })}
                                    />
                                </div>
                            </div>
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>


                <DetailsAccordion dispatch={this.props.dispatch}
                                  model={this.props.model}
                                  threats={this.props.threats}
                                  selectedAsset={this.props.selectedAsset}
                                  selectedThreat={this.props.selectedThreat}
                                  selectedMisbehaviour={this.props.selectedMisbehaviour}
                                  expanded={this.props.expanded}
                    //selectedThreatVisibility={this.props.selectedThreatVisibility}
                                  getAssetType={this.props.getAssetType}
                                  getAssetsForType={this.props.getAssetsForType}
                                  getLink={this.props.getLink}
                                  handleAdd={this.handleAdd}
                                  hoverThreat={this.props.hoverThreat}
                                  isRelationDeletable={this.props.isRelationDeletable}
                                  renderTrustworthinessAttributes={this.props.renderTrustworthinessAttributes}
                                  loading={this.props.loading}
                                  authz={this.props.authz}
                />

                <EditAssetTypeModal show={this.state.editAssetTypeModal.show}
                                    ref={"edit-asset-modal"}
                                    onHide={this.closeEditAssetTypeModal}
                                    assetType={assetType}
                                    palette_assets={this.props.model.palette.assets}
                                    submit={this.submitEditAssetTypeModal}
                />

                <AddRelationModal show={this.state.addRelationModal.show}
                                  onHide={this.closeAddRelationModal}
                                  assets={this.props.model.assets}
                                  host={this.state.addRelationModal.host}
                                  links={this.state.addRelationModal.links}
                                  isIncoming={this.state.addRelationModal.isIncoming}
                                  isRelationExists={this.isRelationExists}
                                  submit={this.submitAddRelationModal}
                />
                {this.props.loading.details && <div className="loading-overlay visible"><span
                    className="fa fa-refresh fa-spin fa-3x fa-fw"/><h1>Loading details...</h1></div>}
            </div>
            :
            <ModelSummary model={this.props.model}
                          threats={this.props.threats}
                          complianceSetsData={this.props.complianceSetsData}
                          hasModellingErrors={this.props.hasModellingErrors}
                          selectedThreat={this.props.selectedThreat}
                          getAssetType={this.props.getAssetType}
                          getAssetsForType={this.props.getAssetsForType}
                          getLink={this.props.getLink}
                          hoverThreat={this.props.hoverThreat}
                          dispatch={this.props.dispatch} 
                          authz={this.props.authz}
                          />;

        let warningText = "Some information in this panel is out of date. The model needs to be re-validated.";
        return (
            <Portal isOpened={true}>

                <SlidingPanel isLeft={false} isResizable={true} width={530}
                    sidePanelActivated={this.props.sidePanelActivated}
                    top={50} title={
                        <Fragment>
                            <h1 class="title">{this.props.selectedAsset.id !== "" ? asset["label"] : "Model Summary"}
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="bottom"
                                    overlay={<Tooltip id="asset-label-tooltip">
                                        <strong>{warningText}</strong></Tooltip>}>
                                    <div style={{
                                        display: this.props.model.valid ? "none" : "inline-block",
                                        marginLeft: "10px"
                                    }}>
                                        <i className="fa fa-exclamation-triangle warning"></i>
                                    </div>
                                </OverlayTrigger>
                            </h1>
                            <div class="button">
                                <ButtonToolbar>
                                <Button
                                        bsStyle="primary"
                                        disabled={!this.state.backEnabled}
                                        onClick={() => this.backInHistory()}
                                        ref="btnback"
                                    >
                                        <i class="fa fa-arrow-left"/>
                                    </Button>
                                    <Button
                                        bsStyle="primary"
                                        disabled={!this.state.forwardEnabled}
                                        onClick={() => this.forwardInHistory()}
                                        ref="btnforward"
                                    >
                                        <i class="fa fa-arrow-right"/>
                                    </Button>
                                </ButtonToolbar>
                            </div>
                        </Fragment>
                    }>
                    {content}
                </SlidingPanel>

            </Portal>
        );
    }

    /**
     * Visuals
     */

    isRelationExists(assetFrom, assetTo, relType) {
        return this.props.model.relations.find((relation) => {
            return relation["fromID"] === assetFrom &&
                relation["toID"] === assetTo &&
                relation["type"] === relType;
        }) !== undefined;
    }

    handleAdd(asset, isIncoming) {
        //console.log("handleAdd for asset:", asset);
        //console.log("isIncoming = " + isIncoming);
        var linkTypes = this.props.model.palette["links"];
        var assetType = this.props.getAssetType(asset["type"]);
        this.openAddRelationModal(asset, isIncoming ? linkTypes[assetType["id"]]["linksTo"] : linkTypes[assetType["id"]]["linksFrom"], isIncoming);
    }

    populationValueChanged(e) {
        let asset = this.state.asset;
        let selectedLevelUri = e.target.value;

        let updatedAsset = {
            id: asset.id,
            uri: asset.uri,
            population: selectedLevelUri
        };

        this.props.dispatch(putAssertedAssetPopulation(this.props.model["id"], updatedAsset))

        this.setState({
            asset: {...this.state.asset,
                population: selectedLevelUri
            },
            updatingPopulation: true
        });

    }

    openAddRelationModal(host, links, isIncoming) {
        //console.log("openAddRelationModal");
        this.setState({
            ...this.state,
            addRelationModal: {
                ...this.state.addRelationModal,
                show: true,
                links: links,
                host: host,
                isIncoming: isIncoming
            }
        });
    }

    submitAddRelationModal(assetFromId, assetToId, relType) {
        //console.log("submitAddRelationModal");
        this.props.dispatch(postAssertedRelation(this.props.model["id"], assetFromId, assetToId, relType));
    }

    closeAddRelationModal() {
        if (this.state.addRelationModal.show) {
            //console.log("closeAddRelationModal (closing)");
            this.setState({
                ...this.state,
                addRelationModal: {
                    ...this.state.addRelationModal,
                    show: false
                }
            });
        }
        //else {
        //    console.log("closeAddRelationModal (already closed)");
        //}
    }

    editType() {
        this.openEditAssetTypeModal();
    }

    openEditAssetTypeModal(host, links, isIncoming) {
        this.setState({
            ...this.state,
            editAssetTypeModal: {
                ...this.state.editAssetTypeModal,
                show: true
                //links: links,
                //host: host,
                //isIncoming: isIncoming
            }
        });
    }

    submitEditAssetTypeModal(assetType) {
        //console.log("DetailPane: submitEditAssetTypeModal: " + assetType);
        let assetId = this.state.asset["id"];
        let updatedAsset = {
            ...this.state.asset,
            type: assetType
        };
        //console.log("assetId: " + assetId);
        //console.log("updatedAsset: ");
        //console.log(updatedAsset);
        this.props.dispatch(putAssertedAssetType(this.props.model["id"], assetId, updatedAsset));
    }

    closeEditAssetTypeModal() {
        console.log("closeEditAssetTypeModal");
        this.setState({
            ...this.state,
            editAssetTypeModal: {
                ...this.state.editAssetTypeModal,
                show: false
            }
        });
        this.refs["edit-asset-modal"].handleClose();
    }

    backInHistory() {
        this.props.dispatch(assetHistoryBack());
    }

    forwardInHistory() {
        this.props.dispatch(assetHistoryForward());
    }

}

DetailPane.propTypes = {
    selectedAsset: PropTypes.object,
    selectedThreat: PropTypes.object,
    selectedMisbehaviour: PropTypes.object,
    expanded: PropTypes.object,
    assetHistory: PropTypes.array,
    historyPointer: PropTypes.number,
    backEnabled: PropTypes.bool,
    forwardEnabled: PropTypes.bool,
    sidePanelActivated: PropTypes.bool,
    //selectedThreatVisibility: PropTypes.bool,
    getAssetType: PropTypes.func,
    getAssetsForType: PropTypes.func,
    getLink: PropTypes.func,
    model: PropTypes.object,
    threats: PropTypes.array,
    complianceSetsData: PropTypes.object,
    hasModellingErrors: PropTypes.bool,
    loading: PropTypes.object,
    hoverThreat: PropTypes.func,
    isRelationDeletable: PropTypes.func,
    renderTrustworthinessAttributes: PropTypes.func,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
};

/* Export the detail panel as required. */
export default DetailPane;
