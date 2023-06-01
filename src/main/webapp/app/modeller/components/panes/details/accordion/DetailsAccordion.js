import React from "react";
import PropTypes from 'prop-types';
import {OverlayTrigger, Panel, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";
import IncomingRelationsPanel from "./panels/IncomingRelationsPanel";
import OutgoingRelationsPanel from "./panels/OutgoingRelationsPanel";
import ControlSetPanel from "./panels/ControlSetPanel";
import ControlStrategiesPanel from "./panels/ControlStrategiesPanel";
import ThreatsPanel from "./panels/ThreatsPanel";
import MisbehavioursPanel from "./panels/MisbehavioursPanel";
import AdditionalPropertiesPanel from "./panels/AdditionalPropertiesPanel" ;
import TrustworthinessPanel from "./panels/TrustworthinessPanel";
import InferredAssetPanel from "./panels/InferredAssetPanel";
import {openDocumentation} from "../../../../../common/documentation/documentation"
import {togglePanel} from "../../../../actions/ModellerActions";

class DetailsAccordion extends React.Component {

    constructor(props) {
        super(props);

        this.formatRelationLabel = this.formatRelationLabel.bind(this);
        this.getTWAS = this.getTWAS.bind(this);

        this.renderHeader = this.renderHeader.bind(this);
        this.renderControlSetsHeader = this.renderControlSetsHeader.bind(this);
        this.renderThreatsHeader = this.renderThreatsHeader.bind(this);
        this.onToggleTwas = this.onToggleTwas.bind(this);

        this.state = {
            expanded: {
                twas: props.expanded.assetDetails.twas
            }
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            expanded: {...this.state.expanded, twas: nextProps.expanded.assetDetails.twas}
        });
    }

    // shouldComponentUpdate(nextProps, nextState) {
    //     //console.log("DetailsAccordion.shouldComponentUpdate");
    //
    //     // if (nextProps.selectedAsset["id"] != this.props.selectedAsset["id"]) {
    //     //     console.log("selectedAsset is changing from " + this.props.selectedAsset["id"] + " to " + nextProps.selectedAsset["id"] + " - don't render yet..");
    //     //     //this.props.dispatch(getCompiledAssetDetails(nextProps.model["id"], nextProps.selectedAsset["id"]));
    //     //     return true;
    //     // }
    //     // else if (nextProps.model["validationStatus"] && (nextProps.model["validationStatus"] == "success") ) {
    //     //     console.log("DetailsAccordion.shouldComponentUpdate detected successful validation. Updating selected asset..");
    //     //     this.props.dispatch(resetValidationStatus()); //reset validationStatus to ensure the getCompiledAssetDetails is only called once here
    //     //     //this.props.dispatch(getCompiledAssetDetails(nextProps.model["id"], nextProps.selectedAsset["id"]));
    //     //     return true;
    //     // }
    //     return true;
    // }

    render() {
        let {model, selectedAsset} = this.props;

        let hasSelectedAsset = selectedAsset["id"] !== "";

        let asset = {id: "", uri: ""};
        if (selectedAsset["id"] !== "" && model["assets"] !== undefined) {
            let foundAsset = model["assets"].find((asset) => asset["id"] === this.props.selectedAsset["id"]);
            if (foundAsset)
                asset = foundAsset;
        }

        let dataTypes = [];
        let assetType = this.props.getAssetType(asset["type"]);
        if (assetType !== null) {
            dataTypes = model.palette["dataTypes"].filter((dataType) => {
                return dataType.appliesTo.indexOf(assetType["id"]) > -1;
            });
        }

        let inferredAssets = [];
        if (asset["inferredAssets"]) {
            //console.log('asset["inferredAssets"]:', asset["inferredAssets"]);

            inferredAssets = asset["inferredAssets"].map((assetUri) => {
                let inferredAsset = model["assets"].find((asset) => asset["uri"] === assetUri);
                if (inferredAsset) {
                    return inferredAsset; //eliminate any undefined values
                }
                else {
                    console.warn("Could not locate inferred asset: ", assetUri);
                }
            }).filter((a) => a !== undefined); //filter any undefined values
        }

        //console.log("inferredAssets:", inferredAssets);

        //get incoming/outgoing relations
        const aId = this.props.selectedAsset.id;
        if (this.props.model.relations != undefined) {
            var outgoing = this.props.model.relations.filter(function (rel) {
                return rel.fromID === aId;
            });
            var incoming = this.props.model.relations.filter(function (rel) {
                return rel.toID === aId;
            });
        } else {
            var incoming, outgoing = [];
        }

        let controlSets = this.props.model["controlSets"] ? this.props.model["controlSets"] : [];
        let assetControlSets = controlSets.filter((controlSet) => controlSet["assetId"] === asset["id"]);
        if (assetControlSets === undefined) {
            assetControlSets = [];
        }
        let checkedAssetControlSets = assetControlSets.filter((controlSet) => controlSet["proposed"]);

        let threats = this.props.threats;
        let assetThreats = threats.filter((threat) => threat["threatensAssets"] === asset["uri"]);
        //console.log("assetThreats", assetThreats);
        if (assetThreats === undefined) {
            assetThreats = [];
        }
        let treatedThreats = assetThreats.filter((threat) => threat["resolved"] || threat["acceptanceJustification"] !== null);

        let assetCsgsSet = new Set(); //set of CSG URIs for this asset
        assetThreats.forEach((threat) => {
            let threatCsgs = Object.keys(threat.controlStrategies);
            //console.log("threatCsgs:", threatCsgs);
            threatCsgs.forEach((csg) => {
                assetCsgsSet.add(csg);
            });
        });

        //console.log("assetCsgsSet:", assetCsgsSet);
        let assetCsgUris = assetCsgsSet;
        //console.log("assetCsgUris:", assetCsgUris);

        let csgsByName = new Map();
        assetCsgUris.forEach((csgUri) => {
            let csg = this.props.model.controlStrategies[csgUri];
            let name = csg.label;
            csgsByName.set(name, csg);
        });
        
        csgsByName = new Map([...csgsByName.entries()].sort());
        //console.log("csgsByName:", csgsByName);
        let csgsArray = Array.from(csgsByName);
        //console.log("csgsArray:", csgsArray);
        let enabledCsgs = csgsArray.filter((csgEntry) => csgEntry[1].enabled);
        //console.log("enabledCsgs:", enabledCsgs);

        //console.log("asset.misbehaviourSets: ", asset.misbehaviourSets);

        let misbehaviourSets = asset.misbehaviourSets ? asset.misbehaviourSets.map((uri) => {
            return this.props.model.misbehaviourSets[uri];
        }) : [];

        let misbehaviours = getMisbehaviours(misbehaviourSets, asset.label);
        //console.log(asset.label);
        
        //console.log("misbehaviours:", Object.values(misbehaviours));
        
        //Determine displayed misbehaviours - in this case those that are "visible"
        let displayedMisbehaviours = Object.values(misbehaviours).filter((misbehaviour) => {
            let visible = misbehaviour["visible"];
            let invisible = visible !== undefined && !visible;
            return !invisible;
        });
        //console.log("displayedMisbehaviours:", displayedMisbehaviours);

        //console.log("DetailsAccordion: misbehaviours:");
        //console.log(misbehaviours);

        // get full TWAS objects array for this asset
        let twas = this.getTWAS(asset);

        return (
            <div className="panel-group detail-accordion">

                {/* <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <span><i className="fa fa-info-circle " />Asset Details and Cardinality</span>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <AssetDetailsPanel>

                            </AssetDetailsPanel>
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel> */}

                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                <span><i className="fa fa-puzzle-piece" />Additional Properties of <i>{asset["label"]}</i></span>
                                <button onClick={e => openDocumentation(e, "redirect/asset-additional-properties")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <AdditionalPropertiesPanel dispatch={this.props.dispatch}
                                modelId={this.props.model["id"]}
                                asset={asset.uri === "" ? undefined : asset}
                                authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

                <Panel bsStyle="primary" expanded={this.state.expanded.twas} onToggle={this.onToggleTwas}>
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                <span><i className="fa fa-thumbs-o-up" />Trustworthiness of <i>{asset["label"]}</i></span>
                                <button onClick={e => openDocumentation(e, "redirect/asset-trustworthiness")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <TrustworthinessPanel dispatch={this.props.dispatch}
                                modelId={this.props.model["id"]}
                                levels={this.props.model.levels["TrustworthinessLevel"]}
                                renderTrustworthinessAttributes={this.props.renderTrustworthinessAttributes}
                                asset={asset.uri === "" ? undefined : asset}
                                twas={twas}
                                authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

                {inferredAssets.length > 0 &&
                    <Panel bsStyle="primary">
                        <Panel.Heading>
                            <Panel.Title toggle>
                                <div className={"doc-help"}>
                                    <span><i className="fa fa-leaf" />Inferred Assets at <i>{asset["label"]}</i> {" (" + inferredAssets.length + ")"}</span>
                                    <button onClick={e => openDocumentation(e, "redirect/asset-inferred-assets")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                                </div>
                            </Panel.Title>
                        </Panel.Heading>
                        <Panel.Collapse>
                            <Panel.Body>
                                <InferredAssetPanel dispatch={this.props.dispatch}
                                    asset={asset.uri === "" ? undefined : asset}
                                    inferredAssets={inferredAssets}
                                />
                            </Panel.Body>
                        </Panel.Collapse>
                    </Panel>
                }

                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                <span><i className="fa fa-compress" />Incoming relations to <i>{asset["label"]}</i> {" (" + incoming.length + ")"}</span>
                                <button onClick={e => openDocumentation(e, "redirect/asset-incoming-relations")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <IncomingRelationsPanel dispatch={this.props.dispatch}
                                model={this.props.model}
                                asset={asset}
                                selectedAsset={this.props.selectedAsset}
                                getAssetType={this.props.getAssetType}
                                getAssetsForType={this.props.getAssetsForType}
                                getLink={this.props.getLink}
                                handleAdd={this.props.handleAdd}
                                formatRelationLabel={this.formatRelationLabel}
                                isRelationDeletable={this.props.isRelationDeletable}
                                authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                <span><i className="fa fa-expand" />Outgoing relations from <i>{asset["label"]}</i> {" (" + outgoing.length + ")"}</span>
                                <button onClick={e => openDocumentation(e, "redirect/asset-outgoing-relations")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <OutgoingRelationsPanel dispatch={this.props.dispatch}
                                model={this.props.model}
                                asset={asset}
                                selectedAsset={this.props.selectedAsset}
                                getAssetType={this.props.getAssetType}
                                getAssetsForType={this.props.getAssetsForType}
                                handleAdd={this.props.handleAdd}
                                formatRelationLabel={this.formatRelationLabel}
                                isRelationDeletable={this.props.isRelationDeletable}
                                authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                {this.renderControlSetsHeader(checkedAssetControlSets.length, assetControlSets.length, asset["label"])}
                                <button onClick={e => openDocumentation(e, "redirect/asset-controls")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <ControlSetPanel dispatch={this.props.dispatch}
                                modelId={this.props.model["id"]}
                                levels={this.props.model.levels["TrustworthinessLevel"]}
                                threats={this.props.model.threats}
                                controlStrategies={this.props.model.controlStrategies}
                                controlSets={assetControlSets}
                                selectedAsset={this.props.selectedAsset}
                                authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                {this.renderControlStrategiesHeader(enabledCsgs.length, csgsArray.length, asset["label"])}
                                <button onClick={e => openDocumentation(e, "redirect/asset-control-strategies")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <ControlStrategiesPanel dispatch={this.props.dispatch}
                                modelId={this.props.model["id"]}
                                asset={asset}
                                assetCsgs={csgsArray}
                                authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

                <Panel bsStyle="primary" eventKey="6" defaultExpanded>
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                <span><i className="fa fa-sitemap" />Consequences and their Impact at <i>{asset["label"]}</i> {" (" + displayedMisbehaviours.length + ")"}</span>
                                <button onClick={e => openDocumentation(e, "redirect/asset-consequences-and-impact")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <MisbehavioursPanel dispatch={this.props.dispatch}
                                model={this.props.model}
                                levels={this.props.model.levels["ImpactLevel"]}
                                selectedAsset={this.props.selectedAsset}
                                misbehaviours={misbehaviours}
                                selectedMisbehaviour={this.props.selectedMisbehaviour}
                                loading={this.props.loading}
                                authz={this.props.authz}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            <div className={"doc-help"}>
                                {this.renderThreatsHeader(treatedThreats.length, assetThreats.length, asset["label"])}
                                <div>
                                    <button onClick={e => openDocumentation(e, "redirect/asset-threats")} className={"doc-help-button"}><i className="fa fa-question" /></button>
                                </div>
                            </div>
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <ThreatsPanel dispatch={this.props.dispatch}
                                name={"asset-threats"}
                                context={asset.uri}
                                model={this.props.model}
                                threats={this.props.threats}
                                selectedAsset={{ ...this.props.selectedAsset, uri: asset.uri }}
                                selectedThreat={this.props.selectedThreat}
                                displayRootThreats={false}
                                hoverThreat={this.props.hoverThreat}
                                getRootThreats={null} // method not required here
                                //isThreatFilterActive={false}
                                threatFiltersActive={null} // not required here
                                loading={this.props.loading}
                            />
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>

            </div>
        );
    }

    renderHeader(title, connector, assetLabel, iconName, n1, n2, tt_text) {
        let props = {
            delayShow: Constants.TOOLTIP_DELAY, placement: "top", trigger: ["hover"],
            overlay: <Tooltip id={title.toLowerCase().replace(" ", "-") +
            "tooltip"}>{tt_text}</Tooltip>
        };
        if (tt_text != null) props.show = null;
        return (
            <span>
                <i className={iconName}/>{title} {connector} <i>{assetLabel}</i> {" "}
                <OverlayTrigger {...props}>
                    <span>{"(" + n1 + "/" + n2 + ")"}</span>
                </OverlayTrigger>
            </span>
        );
    }

    renderControlSetsHeader(n1, n2, assetLabel) {
        let tt_text = n1 + " out of " + n2 + " selected";
        return this.renderHeader("Controls", "at", assetLabel, "fa fa-tag", n1, n2, tt_text);
    }

    renderControlStrategiesHeader(n1, n2, assetLabel) {
        let tt_text = n1 + " out of " + n2 + " enabled";
        return this.renderHeader("Control Strategies", "at", assetLabel, "fa fa-shield", n1, n2, tt_text);
    }

    renderThreatsHeader(n1, n2, assetLabel) {
        let tt_text = n1 + " out of " + n2 + " addressed";
        return this.renderHeader("Threats", "to", assetLabel, "fa fa-exclamation-triangle", n1, n2, tt_text);
    }

    formatRelationLabel(label) {
        if (label === undefined)
            return "";

        //return label.replace(/([A-Z])/g, ' $1').trim().toLowerCase();
        return label;
    }

    getTWAS(asset) {
        //console.log("getTWAS for asset:", asset);
        let twas = [];

        let twasURIs = asset.trustworthinessAttributeSets ? asset.trustworthinessAttributeSets : [];
        //console.log("twasURIs:", twasURIs);
        //console.log("this.props.model.twas:", this.props.model.twas);

        twas = twasURIs.map((uri) => {
            return this.props.model.twas[uri];
        });

        //console.log("twas:", twas);
        return twas;
    }

    onToggleTwas() {
        this.props.dispatch(togglePanel("twas", !this.state.expanded.twas))
    }
}

function getMisbehaviours(assetMisbehaviours, assetLabel) {
    //console.log("DetailsAccordion: getMisbehaviours()", assetMisbehaviours);

    //This is a set of misbehaviours, grouped into those with the same label
    var misbehavioursSet = {};

    if (assetMisbehaviours) {
        assetMisbehaviours.map((misbehaviour) => {
                var misbehavioursGroup;

                if (misbehavioursSet.hasOwnProperty(misbehaviour.misbehaviourLabel)) {
                    misbehavioursGroup = misbehavioursSet[misbehaviour.misbehaviourLabel]
                }
                else {
                    misbehavioursGroup = misbehaviour;
                    misbehavioursSet[misbehaviour.misbehaviourLabel] = misbehavioursGroup;
                }
            });

        //console.log("misbehavioursSet:", misbehavioursSet);
    }

    return misbehavioursSet;
}



DetailsAccordion.propTypes = {
    selectedAsset: PropTypes.object,
    selectedThreat: PropTypes.object,
    selectedMisbehaviour: PropTypes.object,
    expanded: PropTypes.object,
    loading: PropTypes.object,
    getAssetType: PropTypes.func,
    getAssetsForType: PropTypes.func,
    getLink: PropTypes.func,
    handleAdd: PropTypes.func,
    model: PropTypes.object,
    threats: PropTypes.array,
    hoverThreat: PropTypes.func,
    isRelationDeletable: PropTypes.func,
    renderTrustworthinessAttributes: PropTypes.func,
    dispatch: PropTypes.func,
    activeKey: PropTypes.string,
    authz: PropTypes.object
};

export default DetailsAccordion;
