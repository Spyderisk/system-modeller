import React from "react";
import PropTypes from 'prop-types';
import {Panel} from "react-bootstrap";
import {connect} from "react-redux";
import {JsonView, defaultStyles} from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import Explorer from "../common/Explorer";
import ControlStrategiesPanel from "../details/accordion/panels/ControlStrategiesPanel";
import * as Constants from "../../../../common/constants.js";

class RecommendationsExplorer extends React.Component {

    constructor(props) {
        super(props);

        this.renderContent = this.renderContent.bind(this);
        this.renderJson = this.renderJson.bind(this);
        this.renderRecommendations = this.renderRecommendations.bind(this);
        this.renderNoRecommendations = this.renderNoRecommendations.bind(this);
        this.getRiskVector = this.getRiskVector.bind(this);
        this.getHighestRiskLevel = this.getHighestRiskLevel.bind(this);
        this.getAssetByUri = this.getAssetByUri.bind(this);

        this.state = {
        }

    }

    render() {
        if (!this.props.show) {
            return null;
        }

        return (
            <Explorer
                title={"Recommendations Explorer"}
                windowName={"recommendationsExplorer"}
                documentationLink={"redirect/recommendations-explorer"}
                rndParams={{xScale: 0.20, width: 700, height: 600}}
                selectedAsset={this.props.selectedAsset}
                isActive={this.props.isActive}
                show={this.props.show}
                onHide={this.props.onHide}
                loading={this.props.loading}
                dispatch={this.props.dispatch}
                renderContent={this.renderContent}
                windowOrder={this.props.windowOrder}
            />
        )
    }

    renderContent() {
        let renderRecommentations = true; 
        let recommendations = this.props.recommendations;

        if (renderRecommentations) {
            return this.renderRecommendations(recommendations);
        }
        else {
            return this.renderJson(recommendations);
        }
    }

    renderJson(recommendations) {
        return (
            <div className="content">
                {recommendations && <JsonView data={recommendations} shouldExpandNode={shouldExpandRecommendationsNode} style={defaultStyles} />}
            </div>
        )
    }

    renderRecommendations(report) {
        if (jQuery.isEmptyObject(report)) {
            return null;
        }

        let recommendations = report.recommendations;
        let currentRiskVector = this.getRiskVector(report.current.risk);
        let currentRiskLevel = this.getHighestRiskLevel(currentRiskVector);

        let csgAssets = this.props.csgAssets;

        return (
            <div className="content">
                <p>Current risk: {currentRiskLevel.label}</p>
                {!recommendations ? this.renderNoRecommendations() : 
                <div className="panel-group accordion">
                    {recommendations.map((rec, index) => {
                        let id = rec.identifier;
                        let reccsgs = rec.controlStrategies;
                        let state = rec.state;
                        let riskVector = this.getRiskVector(state.risk);
                        let riskLevel = this.getHighestRiskLevel(riskVector);
                        let csgsByName = new Map();

                        reccsgs.forEach((reccsg) => {
                            let csguri = Constants.URI_PREFIX + reccsg.uri;
                            let csg = this.props.model.controlStrategies[csguri];
                            let name = csg.label;
                            let assetUri = csgAssets[csguri];
                            let asset = assetUri ? this.getAssetByUri(assetUri) : {label: "Unknown"}
                            csg.asset = asset;
                            csgsByName.set(name, csg);
                            return csg;
                        });

                        csgsByName = new Map([...csgsByName.entries()].sort());
                        let csgsArray = Array.from(csgsByName);

                        return (
                            <Panel key={id} bsStyle="primary">
                                <Panel.Heading>
                                    <Panel.Title toggle>
                                        <p>Recommendation {id}</p>
                                    </Panel.Title>
                                </Panel.Heading>
                                <Panel.Collapse>
                                    <Panel.Body>
                                        <p>Residual risk: {riskLevel.label}</p>
                                        <p>Control Strategies</p>
                                        <ControlStrategiesPanel dispatch={this.props.dispatch}
                                            modelId={this.props.model["id"]}
                                            assetCsgs={csgsArray}
                                            displayAssetName={true}
                                            authz={this.props.authz}
                                        />
                                    </Panel.Body>
                                </Panel.Collapse>
                            </Panel>
                        );
                    })}
                </div>}
            </div>
        )
    }

    renderNoRecommendations() {
        return (
            <p>The are no current recommendations for reducing the system model risk any further.</p>
        );
    }

    getRiskVector(reportedRisk) {
        let shortUris = Object.keys(reportedRisk);
        let riskLevels = this.props.model.levels["RiskLevel"];

        let riskLevelsMap = {}
        riskLevels.forEach(level => {
            let levelUri = level.uri;
            riskLevelsMap[levelUri] = level;
        });
        
        let riskVector = shortUris.map(shorturi => {
            let uri = Constants.URI_PREFIX + shorturi;
            let riskLevel = riskLevelsMap[uri];
            let riskLevelCount = {level: riskLevel, count: reportedRisk[shorturi]}
            return riskLevelCount;
        });

        //Finally sort the risk vector by level value
        riskVector.sort((a, b) => {
            if (a.level.value < b.level.value) {
                return -1;
            }
            else if (a.level.value > b.level.value) {
                return 1;
            }
            else {
                return 0;
            }
        });

        return riskVector;
    }

    //Get highest risk level from given risk vector
    //i.e. which is the highest risk level that has >0 misbehaviours
    //TODO: could this be moved to a utility function?
    getHighestRiskLevel(riskVector) {
        let overall = 0;
        let hishestLevel = null;
        riskVector.forEach(riskLevelCount => {
            let level = riskLevelCount.level;
            let count = riskLevelCount.count;
            if (count > 0 && level.value >= overall) {
                overall = level.value;
                hishestLevel = level;
            }
        });

        return hishestLevel;
    }

    //TODO: this should be a utility function on the model
    getAssetByUri(assetUri) {
        let asset = this.props.model.assets.find((asset) => {
            return (asset.uri === assetUri);
        });
        return asset;
    }
}

function shouldExpandRecommendationsNode(level) {
    return level <= 1;
}

RecommendationsExplorer.propTypes = {
    model: PropTypes.object,
    csgAssets: PropTypes.object,
    selectedAsset: PropTypes.object,
    isActive: PropTypes.bool, // is in front of other panels
    recommendations: PropTypes.object,
    show: PropTypes.bool,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    dispatch: PropTypes.func,
    windowOrder: PropTypes.number,
    authz: PropTypes.object,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["recommendationsExplorer"]
    }
};

export default connect(mapStateToProps)(RecommendationsExplorer);
