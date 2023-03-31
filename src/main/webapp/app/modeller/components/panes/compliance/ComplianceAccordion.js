import React from "react";
import PropTypes from "prop-types";
import {Panel, OverlayTrigger, Tooltip} from "react-bootstrap";
import ThreatsPanel from "../details/accordion/panels/ThreatsPanel";
import {getThreatStatus} from "../../util/ThreatUtils";
import * as Constants from "../../../../common/constants.js";

class ComplianceAccordion extends React.Component {

    constructor(props) {
        super(props);

        this.toggleExpanded = this.toggleExpanded.bind(this);
        this.renderHeader = this.renderHeader.bind(this);
        this.renderComplianceHeader = this.renderComplianceHeader.bind(this);

        this.state = {
            expanded: {
            }
        }
    }

    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps");

        let expanded = this.state.expanded;

        let complianceSets = nextProps.model.complianceSets;
        complianceSets.map((complianceSet, index) => {
            if ( !(complianceSet.label in expanded) )
                expanded[complianceSet.label] = false; // set to false only first time, to keep user settings
        });

        this.setState({...this.state,
            expanded: expanded
        });

        //console.log("componentWillReceiveProps done");
    }

    render() {
        let {expanded} = this.state;

        //console.log("render: MODELLING_ERRORS_URI: ", Constants.MODELLING_ERRORS_URI);
        let complianceSets = this.props.complianceSetsData["complianceSets"].filter(cs => cs.uri !== Constants.MODELLING_ERRORS_URI);
        //console.log("Compliance sets:", complianceSets);

        let selectedAsset = {
            loadingCausesAndEffects: false,
            loadingControlsAndThreats: false
        };

        return (
            <div className="panel-group accordion">
                {complianceSets.length === 0 &&
                    <div className="desc">
                        <p>No compliance sets found</p>
                    </div>
                }
                {complianceSets.map((complianceSet, index) => {
                    let nCompThreats = complianceSet["nCompThreats"];
                    let nTreatedThreats = complianceSet["nTreatedThreats"];
                    let complianceLabel = complianceSet.label;
                    let compliant = complianceSet.compliant;
                    
                    let panelExpanded = this.state.expanded[complianceLabel];
                    let bsStyle = compliant ? "success" : "danger";

                    return (
                        <Panel bsStyle={bsStyle} key={index + 1}>
                            <Panel.Heading>
                                <Panel.Title toggle>
                                    {this.renderComplianceHeader(complianceSet, nTreatedThreats, nCompThreats)}
                                </Panel.Title>
                            </Panel.Heading>
                            <Panel.Collapse>
                                <Panel.Body>
                                    <div className="desc" style={{paddingLeft: 0, paddingRight: 0}}>
                                        <div className="descriptor">
                                            <p>
                                                <strong>Description: </strong>
                                                <span>{complianceSet.description}</span>
                                            </p>
                                            <p>
                                                <strong>Compliant: </strong>
                                                <span>{compliant ? "true" : "false"}</span>
                                            </p>
                                        </div>
                                    </div>
                                    <ThreatsPanel dispatch={this.props.dispatch}
                                                  name={"compliance-threats"}
                                                  index={index}
                                                  model={this.props.model}
                                                  selectedAsset={null}
                                                  selectedThreat={this.props.selectedThreat}
                                                  displayRootThreats={true} //technically false, but allows us to use complianceSet in ThreatsPanel
                                                  hoverThreat={this.props.hoverThreat}
                                                  getRootThreats={null} //not required
                                                  complianceSet={complianceSet}
                                                  threatFiltersActive={this.props.threatFiltersActive}
                                                  loading={this.props.loading}
                                                  authz={this.props.authz}
                                                  />
                                </Panel.Body>
                            </Panel.Collapse>
                        </Panel>
                    );
                })}
            </div>
        );
    }

    renderHeader(title, iconName, n1, n2, tt_text) {
        let overlay = "";
        let tt_id = title.toLowerCase().replace(" ", "-") + "-tooltip";
        
        if (tt_text) {
            let overlayProps = {
                delayShow: Constants.TOOLTIP_DELAY, placement: "top", trigger: ["hover"],
                overlay: <Tooltip id={tt_id} className={"tooltip-overlay"}>{tt_text}</Tooltip>
            };
            overlay = <OverlayTrigger {...overlayProps}>
                <span>{ ((n1 > -1) && (n2 > 0)) ? "(" + n1 + "/" + n2 + ")" : "(" + n2 + ")"}
                </span>
            </OverlayTrigger>;
        }

        return (
            <div>
                <span>
                    <i className={iconName}/>{title}{" "}{overlay}
                </span>
            </div>
        );
    }
    
    renderComplianceHeader(complianceSet, nCompliant, nCompThreats) {
        let complianceLabel = complianceSet.label;
        if (nCompThreats === 0) {
            //complianceLabel += ` (0)`;
            complianceLabel = complianceLabel.replace("Issues", "Issue");
            complianceLabel = complianceLabel.replace("Errors", "Error");
        } else if (nCompThreats === 1) {
            //complianceLabel += ` (${nCompliant}/${nCompThreats})`;
            complianceLabel = complianceLabel.replace("Issues", "Issue");
            complianceLabel = complianceLabel.replace("Errors", "Error");
        } else {
            //complianceLabel += ` (${nCompliant}/${nCompThreats})`;
        }
                    
        let icon = complianceSet.compliant ? "fa fa-check" : "fa fa-warning";
        let tt_text = (nCompThreats > 0) ? nCompliant + " out of " + nCompThreats + " addressed" : "no compliance issues";
        return this.renderHeader(complianceLabel, icon, nCompliant, nCompThreats, tt_text);
    }

    toggleExpanded(label) {
        let expanded = this.state.expanded;
        expanded[label] = !expanded[label];
        this.setState({
            ...this.state,
            expanded: expanded
        })
    }

}

ComplianceAccordion.propTypes = {
    model: PropTypes.object,
    complianceSetsData: PropTypes.object,
    selectedThreat: PropTypes.object,
    loading: PropTypes.object,
    threatFiltersActive: PropTypes.object,
    hoverThreat: PropTypes.func,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
};

export default ComplianceAccordion;
