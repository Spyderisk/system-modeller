import React, {Component} from "react";
import PropTypes from "prop-types";
import {Form, FormGroup, Checkbox} from "react-bootstrap";
import {getLevelColour, renderPopulationLevel} from "../../util/Levels";

let uniq = (arr) => [...new Set(arr)];

class RiskTreatmentPlan extends Component {

    constructor(props) {
        super(props);

        let coverageLevals = props.model.levels["TrustworthinessLevel"];

        this.state = {
            coverageLevals: coverageLevals,
            coverageLevalsMap: RiskTreatmentPlan.getCoverageLevelsMap(coverageLevals),
            includeInferredAssets: true,
            onlyInferredAssetsWithActiveControls: false,
        };
    }

    static getCoverageLevelsMap(coverageLevals) {
        let levelsMap = new Map();

        coverageLevals.forEach((level) => {
            levelsMap.set(level.uri, level);
        });

        return levelsMap;
    }

    getAssetNameFromId(id) {
        let asset = this.props.model.assets.filter(asset => asset.id === id)[0];
        return asset.label;
    };

    getThreatDescriptions(threats) {
        return uniq(threats.map(t => t.description.split(": ")[0]));
    };

    categoriseThreats(threats) {
        let inPlace = {threats: [], controls: []};
        let ignored = {threats: [], controls: []};
        let accepted = {threats: [], controls: []};
        let workInProgress = {threats: [], controls: []};
        let uncontrolled = {threats: [], controls: []}; //no CSG avaialble on direct cause threat

        threats.forEach(threat => {
            let allControlCombinations = threat.allControlCombinations.flat(2);

            // Get updated control set objects
            let allControls = allControlCombinations.map(controlUri => {
                return this.props.controlSets[controlUri];
            });

            if (threat.acceptanceJustification !== null) {
                accepted = ({threats: accepted.threats.concat(threat), controls: []}); // set controls to [] as we don't want to view controls that haven't been implemented
                return;
            }

            if (allControls.length === 0) { // some threats have no controls, we can either hide these threats or display them
                // here we put them in the "uncontrolled" group
                uncontrolled = ({threats: uncontrolled.threats.concat(threat), controls: []});
                return;
            }

            let proposed = [];
            let wip = [];
            let ignore = [];

            allControls.forEach(control => {
                if (control.assertable) {
                    if (control.proposed && control.workInProgress !== true)
                        proposed.push(control);
                    else if (control.proposed && control.workInProgress)
                        wip.push(control);
                    else
                        ignore.push(control);
                }
            });

            if (ignore.length === allControls.length) { //no controls have been proposed, therefore threat is being ignored as hasn't been accepted or resolved
                ignored = ({threats: ignored.threats.concat(threat), controls: []}); // set controls to [] as we don't want to view controls that haven't been implemented
                return;
            }

            if (proposed.length)
                inPlace = ({
                    threats: inPlace.threats.concat(threat),
                    controls: inPlace.controls.concat(proposed)
                });

            if (wip.length)
                workInProgress = ({
                    threats: workInProgress.threats.concat(threat),
                    controls: workInProgress.controls.concat(wip)
                });
        });

        return {
            inPlace: inPlace,
            workInProgress: workInProgress,
            ignored: ignored,
            accepted: accepted,
            uncontrolled: uncontrolled
        }
    }

    renderImpactLevel(ms, impactLevels) {
        let impact = ms.impactLevel ? ms.impactLevel.label : "N/A";
        let impact_colour = ms.impactLevel ? getLevelColour(impactLevels, ms.impactLevel, false) : "";
        return <td style={{backgroundColor: impact_colour, whiteSpace: "nowrap"}}>{impact}</td>
    }

    renderLikelihoodLevel(ms, likelihoodLevels) {
        let likelihood = ms.likelihood ? ms.likelihood.label : "N/A";
        let likelihood_colour = ms.likelihood ? getLevelColour(likelihoodLevels, ms.likelihood, false) : "";
        return <td style={{backgroundColor: likelihood_colour, whiteSpace: "nowrap"}}>{likelihood}</td>
    }

    renderRiskLevel(ms, riskLevels) {
        let risk = ms.riskLevel ? ms.riskLevel.label : "N/A";
        let risk_colour = ms.riskLevel ? getLevelColour(riskLevels, ms.riskLevel, false) : "";
        return <td style={{backgroundColor: risk_colour, whiteSpace: "nowrap"}}>{risk}</td>
    }

    getTreatmentMethod(splitType) {
        return splitType === "accepted" ? "Accept" : splitType === "ignored" ? "Ignore" : splitType === "uncontrolled" ? "n/a" : "Mitigate";
    }

    getStatus(splitType) {
        return splitType === "workInProgress" ? "Work In Progress" : splitType === "inPlace" ? "In Place" : "n/a";
    }

    sortAssets(assetOne, assetTwo){
        let labelOne = assetOne.label.toUpperCase(); // ignore upper and lowercase
        let labelTwo = assetTwo.label.toUpperCase(); // ignore upper and lowercase

        if (labelOne < labelTwo)
            return -1;

        if (labelOne > labelTwo)
            return 1;

        return 0;
    }

    renderAsset(asset) {
        let assetType = this.props.getAssetType(asset["type"]);
        if (!assetType) assetType = {"label": "unknown", "description": ""};
        let populationLevels = this.props.model.levels["PopulationLevel"];
        let populationLevel = populationLevels.find((level) => level.uri === asset.population);

        return ([<div key={asset.id} className="title">{asset.label}</div>,
        <div key={"pop_"+ asset.id} className="population">
            <p>
                <strong>Type: </strong>{assetType["label"]}
            </p>
            <p>
                <strong>Description: </strong>{assetType["description"]}
            </p>
            <strong>Population:</strong>
            &nbsp;
            {renderPopulationLevel(asset, populationLevel, populationLevels, false, false)}
        </div>]);
    }

    renderDirectCauseThreats(threats) {
        let sortedThreats = this.getThreatDescriptions(threats).sort((a, b) => a.localeCompare(b));
        return (
            <ul>
                {sortedThreats.map((threat) => <li key={threat}>{threat}</li>)}
            </ul>
        );
    }

    getControlDesc(c) {
        return c.label + " at " + this.getAssetNameFromId(c.assetId);
    }

    getCoverageLevel(cs) {
        return this.state.coverageLevalsMap.get(cs.coverageLevel)
    }

    renderCoverageLevel(cs) {
        let coverageLevel = this.getCoverageLevel(cs);
        return (
            <span style={{ backgroundColor: getLevelColour(this.state.coverageLevals, coverageLevel, true) }}>
                {coverageLevel.label}
            </span>
        )
    }

    renderControlsList(controls) {
        return (
            <ul>
                {controls.map((cs) => <li key={cs.id}>{this.getControlDesc(cs)} ({this.renderCoverageLevel(cs)})</li>)}
            </ul>
        );
    }

    render() {
        let model = this.props.model;
        let assertedOnly = !this.state.includeInferredAssets;
        let assets = model.assets ? (assertedOnly ? model.assets.filter(a => a.asserted) : model.assets) : [];
        let content = [];
        let impactLevels = this.props.model.levels["ImpactLevel"];
        let likelihoodLevels = this.props.model.levels["Likelihood"];
        let riskLevels = this.props.model.levels["RiskLevel"];

        assets.sort(this.sortAssets).forEach(asset => {
            let assetHasActiveControl = false;
            let rows = [];

            // Get asset misbehaviours and filter out non-visible ones
            let misbehaviours = asset.misbehaviourSets.map(ms => model.misbehaviourSets[ms]).filter((misbehaviour) => {
                let visible = misbehaviour["visible"];
                let invisible = visible !== undefined && !visible;
                return !invisible;
            });

            // Sort alphabetically
            misbehaviours.sort((a, b) => a["misbehaviourLabel"].localeCompare(b["misbehaviourLabel"]))

            misbehaviours.forEach(misbehaviour => {
                let threatsForMisbehaviour = model.threats.filter(threat => threat.misbehaviours.includes(misbehaviour.uri));

                if (!threatsForMisbehaviour.length) {// we don't want to display this misbehaviour as 0 threats fall cause it = can't be treated
                    return;
                }

                let categorisedThreats = this.categoriseThreats(threatsForMisbehaviour);
                let categories = ["inPlace", "workInProgress", "ignored", "accepted", "uncontrolled"];

                categories.forEach(category => {
                    let {threats, controls} = categorisedThreats[category];

                    if (!threats.length) {
                        return; // we don't want to display this split type as 0 threats fall into this category
                    }

                    // If we are checking an inferred asset for active controls...
                    if (!asset.asserted && this.state.onlyInferredAssetsWithActiveControls) {
                        // For any category other than "ignored", we have an acitvei control, if list is not empty
                        // N.B. Here, we are also counting "accepted" as a "control"
                        if (category !=="ignored" && controls.length > 0) {
                            assetHasActiveControl = true;
                        }
                    }

                    rows.push(<tr key={"m-" + misbehaviour.id + "-" + category}>
                        <td>{misbehaviour.misbehaviourLabel}</td>
                        {this.renderImpactLevel(misbehaviour, impactLevels)}
                        {this.renderLikelihoodLevel(misbehaviour, likelihoodLevels)}
                        {this.renderRiskLevel(misbehaviour, riskLevels)}
                        <td className="bullet-pt-list">{this.renderDirectCauseThreats(threats)}</td>
                        <td style={{whiteSpace: "nowrap"}}>{this.getTreatmentMethod(category)}</td>
                        <td style={{whiteSpace: "nowrap"}}>{this.getStatus(category)}</td>
                        <td className="bullet-pt-list">{this.renderControlsList(uniq(controls))}</td>
                    </tr>);
                });
            });

            if (rows.length === 0)
                return; // No content to display for asset 

            if (!asset.asserted && this.state.onlyInferredAssetsWithActiveControls && !assetHasActiveControl) {
                return // Inferred asset has no active control
            }

            content.push(this.renderAsset(asset));

            content.push(<table key={"t-" + asset.id}>
                <thead>
                <tr>
                    <th className="col-1">Consequence</th>
                    <th className="col-2">Impact</th>
                    <th className="col-3">Likelihood</th>
                    <th className="col-4">Risk</th>
                    <th className="col-5">Direct Causes</th>
                    <th className="col-6">Treatment Method</th>
                    <th className="col-7">Status</th>
                    <th className="col-8">Controls</th>
                </tr>
                </thead>
                <tbody>{rows}</tbody>
            </table>);
        });

        return (
            <div className="report-content" id="risk-treatment-plan">
                <div className="heading">
                    Risk Treatment Plan
                </div>
                {assets.length > 0 &&
                    <Form>
                        <FormGroup>
                            <Checkbox
                                checked={this.state.includeInferredAssets}
                                onChange={(e) => {
                                    this.setState({
                                        ...this.state,
                                        includeInferredAssets: e.nativeEvent.target.checked
                                    })
                                }}>
                                Include inferred assets
                            </Checkbox>
                        </FormGroup>
                    </Form>
                }
                {content}
            </div>
        );

    }
}

RiskTreatmentPlan.propTypes = {
    model: PropTypes.object,
    controlSets: PropTypes.object,
    getAssetType: PropTypes.func,
};

export default RiskTreatmentPlan;
