import React, {Component} from "react";
import PropTypes from "prop-types";
import {Button} from "react-bootstrap";
import {controlReference, controlSetMapping} from "../../../../common/constants";
import {getLevelColour, getThreatColor} from "../../util/Levels";

let uniq = (arr) => [...new Set(arr)];

class RiskTreatmentPlan extends Component {

    constructor(props) {
        super(props);
    }

    getAssetNameFromId(id, assetName) {
        let asset = this.props.model.assets.filter(asset => asset.id === id)[0];

        if (asset.asserted)
            return asset.label;
        else
            return assetName;
    };

    getAssetFromUri(uri) {
        return this.props.model.assets.filter(asset => asset.uri === uri)[0]
    };

    getProposedControlsForThreats(threats) {
        return threats.map(t => t.allControlCombinations).flat(2).filter(control => control.proposed);
    };

    getMisbehavioursForAsset(assetUri) {
        return this.getAssetFromUri(assetUri).misbehaviourSets.map(ms => this.props.model.misbehaviourSets[ms]);
    }

    getThreatDescriptions(threats) {
        return uniq(threats.map(t => t.description.split(":")[0]));
    };

    categoriseThreats(threats) {
        let inPlace = {threats: [], controls: []};
        let ignored = {threats: [], controls: []};
        let accepted = {threats: [], controls: []};
        let workInProgress = {threats: [], controls: []};

        threats.forEach(threat => {
            let allControls = threat.allControlCombinations.flat(2);

            if (threat.acceptanceJustification !== null) {
                accepted = ({threats: accepted.threats.concat(threat), controls: []}); // set controls to [] as we don't want to view controls that haven't been implemented
                return;
            }

            if (allControls.length === 0) { // some threats have no controls, we can either hide these threats or display them
                return; // here I deciding to hide them
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
            accepted: accepted
        }
    }

    renderImpactLevel(ms, impactLevels) {
        let impact = ms.impactLevel ? ms.impactLevel.label : "N/A";
        let impact_colour = ms.impactLevel ? getLevelColour(impactLevels, ms.impactLevel, false) : "";
        return <td style={{backgroundColor: impact_colour, whiteSpace: "nowrap"}}>{impact}</td>
    }

    getTreatmentMethod(splitType) {
        return splitType === "accepted" ? "Accept" : splitType === "ignored" ? "Ignore" : "Mitigate";
    }

    getStatus(splitType) {
        return splitType === "workInProgress" ? "Work In Progress" : splitType === "inPlace" ? "In Place" : "n/a";
    }

    getTargetDate(splitType) {
        return splitType === "workInProgress" ? "?" : "n/a";
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

    render() {
        let model = this.props.model;
        let assets = model.assets ? model.assets.filter(a => a.asserted) : [];
        let content = [];
        let impactLevels = this.props.model.levels["ImpactLevel"];

        assets.sort(this.sortAssets).forEach(asset => {
            let rows = [];

            let misbehaviours = asset.misbehaviourSets.map(ms => model.misbehaviourSets[ms]);
            let inferredMisbehaviours = asset.inferredAssets.map(ia => this.getMisbehavioursForAsset(ia)).flat(2);

            misbehaviours.concat(inferredMisbehaviours).forEach(misbehaviour => {
                let threatsForMisbehaviour = model.threats.filter(threat => threat.misbehaviours.includes(misbehaviour.uri));

                if (!threatsForMisbehaviour.length) // we dont want to display this misbehaviour as 0 threats fall cause it = can't be treated
                    return;

                if(asset.label === "WiFi")
                    console.log(threatsForMisbehaviour);

                let categorisedThreats = this.categoriseThreats(threatsForMisbehaviour);
                let categories = ["inPlace", "workInProgress", "ignored", "accepted"];

                if(asset.label === "WiFi")
                    console.log(categorisedThreats);

                categories.forEach(category => {
                    let {threats, controls} = categorisedThreats[category];

                    if (!threats.length)
                        return; // we dont want to display this split type as 0 threats fall into this category

                    let controlDescriptions = uniq(controls.map(c => (c.label + " at " + this.getAssetNameFromId(c.assetId, asset.label))));

                    rows.push(<tr key={"m-" + misbehaviour.id + "-" + category}>
                        <td>{misbehaviour.misbehaviourLabel}</td>
                        {this.renderImpactLevel(misbehaviour, impactLevels)}
                        <td className="bullet-pt-list">
                            <ul>{this.getThreatDescriptions(threats).map((v, i) => <li key={i}>{v}</li>)}</ul>
                        </td>
                        <td>{this.getTreatmentMethod(category)}</td>
                        <td>{this.getStatus(category)}</td>
                        <td className="bullet-pt-list">
                            <ul>{controlDescriptions.map((v, i) => <li key={i}>{v}</li>)}</ul>
                        </td>
                    </tr>);
                });
            });

            if (rows.length === 0)
                return;

            content.push(<div key={asset.id} className="title">{asset.label}</div>);

            content.push(<table key={"t-" + asset.id}>
                <thead>
                <tr>
                    <th className="col-1">Consequence</th>
                    <th className="col-2">Impact</th>
                    <th className="col-3">Direct Causes</th>
                    <th className="col-4">Treatment Method</th>
                    <th className="col-5">Status</th>
                    <th className="col-6">Controls</th>
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
                {content}
            </div>
        );

    }
}

RiskTreatmentPlan.propTypes = {
    model: PropTypes.object,
    getAssetType: PropTypes.func
};

export default RiskTreatmentPlan;
