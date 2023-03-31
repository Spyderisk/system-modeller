import React from "react";
import PropTypes from "prop-types";
import {Checkbox, Label} from "react-bootstrap";
import {getLevelColour, getThreatColor} from "../../util/Levels";
import {getThreatStatus} from "../../util/ThreatUtils";

class Report extends React.Component {

    constructor(props) {
        super(props);
    }
    
    render() {
        console.log("render Report");
        let model = this.props.model;
        
        //model summary
        let domain = model["domainGraph"] !== null ? model["domainGraph"].replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-", "").toUpperCase() : "unknown";
        let assertedAssets = model.assets.filter(asset => asset.asserted).sort((a, b) => a.label.localeCompare(b.label));
        let inferredAssets = model.assets.filter(asset => !asset.asserted).sort((a, b) => a.label.localeCompare(b.label));
        
        return (
            <div className="report-content" id="report-content">
                <div className="summary">
                    <h3>Model Summary</h3>
                    <p><strong>Name:</strong> {model.name}</p>
                    <p><strong>Knowledgebase:</strong> {domain}</p>
                    <p><strong>Description:</strong> {model.name}</p>
                    <p><strong>Assets:</strong> {model.assets.length}</p>
                    <p><strong>Relations:</strong> {model.relations.length}</p>
                    <p><strong>Threats:</strong> {model.threats.length}</p>
                </div>
                <div className="assets">
                    <h3>Assets</h3>
                    
                    <h4>Asserted</h4>
                    {this.renderAssets(assertedAssets)}
                    
                    <h4>Inferred</h4>
                    {this.renderAssets(inferredAssets)}

                </div>
                <div className="threats">
                    <h3>Threats</h3>
                    {this.renderThreats()}                    
                </div>
                <div className="compliance">
                    <h3>Compliance</h3>
                    {this.renderCompliance()}                    
                </div>
            </div>
        );
    }
    
    renderAssets(assets) {
        return (
            <table>
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Type</th>
                        <th>Trustworthiness</th>
                        <th>Controls</th>
                        <th>Misbehaviours</th>
                    </tr>
                </thead>
                <tbody>
                    {assets.map((a, index) => {
                        let assetType = this.getAssetType(a);
                        return (
                            <tr key={index} style={{verticalAlign: "top"}}>
                                <td>{a.label}</td>
                                <td>{assetType}</td>
                                <td>{this.renderTWAS(a)}</td>
                                <td>{this.renderControlSets(a)}</td>
                                <td>{this.renderMisbehaviours(a)}</td>
                            </tr>
                        )
                    })}
                </tbody>
            </table>
        );
    }
    
    renderThreats() {
        let threats = this.props.model.threats;
        
        //sort alphabetically for now
        threats.sort(function (a, b) {
            return (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0;
        });
        
        //console.log("threats:", threats);
        
        let likelihoodLevels = this.props.model.levels["Likelihood"];
        let riskLevels = this.props.model.levels["RiskLevel"];
        
        return (
            <table>
                <thead>
                    <tr>
                        <th></th>
                        <th>Name</th>
                        <th>Description</th>
                        <th>Likelihood</th>
                        <th>Risk</th>
                    </tr>
                </thead>
                <tbody>
                    {threats.map((threat, index) => {
                        let status = getThreatStatus(threat, this.props.model.controlStrategies);
                        let symbol = this.getThreatStatusSymbol(status, threat);
                        
                        let root_cause = threat.rootCause;
                        let secondary = threat.secondaryThreat;
                        let primary = !secondary;
                                        
                        let likelihood = threat.likelihood ? threat.likelihood.label : "N/A";
                        let likelihood_colour = getLevelColour(likelihoodLevels, threat.likelihood);
                        let risk = threat.riskLevel ? threat.riskLevel.label : "N/A";
                        let risk_colour = getLevelColour(riskLevels, threat.riskLevel);
                        
                        return (
                            <tr key={index} style={{verticalAlign: "top"}}>
                                <td><span style={{marginRight: "5px"}}>{symbol}</span>
                                    <span>{primary ? 
                                        root_cause ? <Label style={{backgroundColor: "#bf0500"}}><span>1</span></Label> : <Label><span>1</span></Label>
                                        : <Label><span>2</span></Label>}
                                    </span>
                                </td>
                                <td>{threat.label}</td>
                                <td>{threat.description}</td>
                                <td style={{backgroundColor: likelihood_colour, whiteSpace: "nowrap"}}>{likelihood}</td>
                                <td style={{backgroundColor: risk_colour, whiteSpace: "nowrap"}}>{risk}</td>
                            </tr>
                        )
                    })}
                </tbody>
            </table>
        );
    }
    
    renderCompliance() {
        let complianceSets = this.props.model.complianceSets;
        //console.log("complianceSets:", complianceSets);
        
        return (
            <div>
                {complianceSets.map((complianceSet, index) => {
                    return (
                        <div key={index}>
                            <h4>{complianceSet.label}</h4>
                            <p><strong>Description: </strong>{complianceSet.description}</p>
                            <p><strong>Compliant: </strong>{complianceSet.compliant ? "true" : "false"}</p>
                            {this.renderComplianceThreats(complianceSet)}
                            <br></br>
                        </div>
                    );
                })}
            </div>
        );        
    }
    
    renderComplianceThreats(complianceSet) {
        let complianceThreats = this.getComplianceThreats(complianceSet);
        if (complianceThreats.length === 0)
            return null;
        
        return (
            <table>
                <thead>
                    <tr>
                        <th></th>
                        <th>Compliance Threat</th>
                        <th>Description</th>
                    </tr>
                </thead>
                <tbody>
                    {complianceThreats.map((threat, index) => {
                        let status = getThreatStatus(threat, this.props.model.controlStrategies);
                        let symbol = this.getThreatStatusSymbol(status, threat);
                        
                        return (
                            <tr key={index} style={{verticalAlign: "top"}}>
                                <td>
                                    <span style={{marginRight: "5px"}}>{symbol}</span>
                                </td>
                                <td>{threat.label}</td>
                                <td>{threat.description}</td>
                            </tr>
                        )
                    })}
                </tbody>
            </table>
        );
    }
    
    getComplianceThreats(complianceSet) {
        let complianceThreats = complianceSet.systemThreats;
        return complianceThreats.map((threatUri) => {
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
        let threat = this.props.model.complianceThreats.find((threat) => {
            return (threat.uri === threatUri);
        });
        return threat;
    }
   
    getThreatStatusSymbol(status, threat) {
        let threatColorAndBE = ((status === "BLOCKED") || (status === "MITIGATED")) ?
        getThreatColor(threat, this.props.model.controlStrategies, this.props.model.levels["TrustworthinessLevel"], true) : undefined;

        let symbol = <Label bsStyle="danger"><span className="fa fa-exclamation-triangle"/></Label>;
        if (status === "BLOCKED") {
            symbol = <Label bsStyle="success" style={{backgroundColor: threatColorAndBE.color}}><span className="fa fa-check"/></Label>;
        } else if (status === "MITIGATED") {
            symbol = <Label bsStyle="success" style={{backgroundColor: threatColorAndBE.color}}><span className="fa fa-minus"/></Label>;
        } else if (status === "ACCEPTED") {
            symbol = <Label bsStyle="warning"><span className="fa fa-thumbs-up"/></Label>;
        }
        
        return symbol;
    }
    
    getAssetType(a) {
        let assetType = this.props.getAssetType(a["type"]);
        //console.log("assetType:", assetType);
        return assetType ? assetType.label : "unknown";
    }
    
    getTWAS(asset) {
        //console.log("getTWAS for asset:", asset);

        let twasURIs = asset.trustworthinessAttributeSets;
        //console.log("twasURIs:", twasURIs);
        //console.log("this.props.model.twas:", this.props.model.twas);

        let twas = twasURIs.map((uri) => {
            return this.props.model.twas[uri];
        });

        //console.log("twas:", twas);
        return twas.sort(function (a, b) {
            return (a.attribute.label < b.attribute.label) ? -1 : (a.attribute.label > b.attribute.label) ? 1 : 0;
        });
    }
    
    renderTWAS(a) {
        let modelTwas = this.props.model.twas;
        //console.log("modelTwas:", modelTwas);
        let levels = this.props.model.levels["TrustworthinessLevel"];
        //console.log("levels:", levels);
        
        let assetTwas = this.getTWAS(a);
        
        if (assetTwas.length === 0)
            return "N/A";
        
        
        return (
            <table>
                <thead>
                    <tr>
                        <th>Attribute</th><th>Assumed</th><th>Calculated</th>
                    </tr>
                </thead>
                <tbody>
                    {assetTwas.map((twas, index) => {
                        let label = twas.attribute.label;
                        let assumed = twas.assertedTWLevel ? twas.assertedTWLevel.label : "";
                        let calculated = twas.inferredTWLevel ? twas.inferredTWLevel.label : "";
                        let assumed_colour = getLevelColour(levels, twas.assertedTWLevel, true);
                        let calculated_colour = getLevelColour(levels, twas.inferredTWLevel, true);
                        //console.log("assumed_colour", assumed_colour);

                        return (
                            <tr key={index}>
                                <td>{label}</td>
                                <td style={{backgroundColor: assumed_colour}}>{assumed}</td>
                                <td style={{backgroundColor: calculated_colour}}>{calculated}</td>
                            </tr>
                        )
                    })}
                </tbody>
            </table>
        );
    }
    
    renderControlSets(a) {
        let allControlSets = this.props.model["controlSets"] ? this.props.model["controlSets"] : [];
        let controlSets = allControlSets.filter((controlSet) => controlSet["assetId"] === a["id"]);
        
        if (controlSets === undefined) {
            controlSets = [];
        }
        
        return (
            <div>
        {controlSets.length > 0 ? controlSets.sort((a, b) => (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0)
            .map((controlSet, index) => {
                return (
                    <div key={index}>
                        <Checkbox
                            style={{
                                pointerEvents: controlSet["assertable"] ? "auto" : "none",
                                display: "inline",
                                verticalAlign: "middle"
                            }}
                            disabled={!controlSet["assertable"]}
                            checked={controlSet["proposed"]}
                            readOnly
                        >{controlSet.label}</Checkbox>
                    </div>
                )
            }) : <span>N/A</span>}
            </div>
        );
    }
    
    renderMisbehaviours(a) {
        let impactLevels = this.props.model.levels["ImpactLevel"];
        let likelihoodLevels = this.props.model.levels["ImpactLevel"];
        let riskLevels = this.props.model.levels["ImpactLevel"];
        
        let misbehaviourSets = a.misbehaviourSets.map((uri) => {
            return this.props.model.misbehaviourSets[uri];
        });
        
        if (misbehaviourSets.length === 0)
            return "N/A";
        
        misbehaviourSets.sort(function (a, b) {
            return (a.misbehaviourLabel < b.misbehaviourLabel) ? -1 : (a.misbehaviourLabel > b.misbehaviourLabel) ? 1 : 0;
        });
        
        return (
            <table>
                <thead>
                    <tr>
                        <th>Misbehaviour</th><th>Impact</th><th>Likelihood</th><th>Risk</th>
                    </tr>
                </thead>
                <tbody>
                    {misbehaviourSets.map((ms, index) => {
                        let label = ms.misbehaviourLabel;
                        let impact = ms.impactLevel ? ms.impactLevel.label : "N/A";
                        let impact_colour = ms.impactLevel ? getLevelColour(impactLevels, ms.impactLevel, false) : "";
                        let likelihood = ms.likelihood ? ms.likelihood.label : "N/A";
                        let likelihood_colour = ms.likelihood ? getLevelColour(likelihoodLevels, ms.likelihood, false) : "";
                        let risk = ms.riskLevel ? ms.riskLevel.label : "N/A";
                        let risk_colour = ms.riskLevel ? getLevelColour(riskLevels, ms.riskLevel, false) : "";

                        return (
                            <tr key={index}>
                                <td>{label}</td>
                                <td style={{backgroundColor: impact_colour, whiteSpace: "nowrap"}}>{impact}</td>
                                <td style={{backgroundColor: likelihood_colour, whiteSpace: "nowrap"}}>{likelihood}</td>
                                <td style={{backgroundColor: risk_colour, whiteSpace: "nowrap"}}>{risk}</td>
                            </tr>
                        )
                    })}
                </tbody>
            </table>
        );
        
    }
}

Report.propTypes = {
    model: PropTypes.object,
    getAssetType: PropTypes.func
};

export default Report;
