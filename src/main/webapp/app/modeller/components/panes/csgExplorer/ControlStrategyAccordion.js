import React from "react";
import PropTypes from "prop-types";
import {Panel} from "react-bootstrap";
import ThreatsPanel from "../details/accordion/panels/ThreatsPanel";
import renderControlStrategy from "./ControlStrategyRenderer";

//This class is only required if ControlStrategyExplorer needs expandable panels
class ControlStrategyAccordion extends React.Component {

    constructor(props) {
        super(props);
        
        this.toggleExpanded = this.toggleExpanded.bind(this);
        this.getCsgControlSets = this.getCsgControlSets.bind(this);

        this.state = {
            updatingControlSets: {},
            controlSets: this.getCsgControlSets(props),
            expanded: {
                threats: true
            }
        }
    }
    
    shouldComponentUpdate(nextProps, nextState) {
        return true;
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            updatingControlSets: {},
            controlSets: this.getCsgControlSets(nextProps)
        });
    }

    getCsgControlSets(props) {
        let controlSets = {};

        props.selectedControlStrategy.forEach(csg => {
            csg.mandatoryControlSets.forEach(csUri => {
                controlSets[csUri] = props.controlSets[csUri];
            });
            csg.optionalControlSets.forEach(csUri => {
                controlSets[csUri] = props.controlSets[csUri];
            });
        });

        return controlSets;
    }

    render() {
        let {expanded} = this.state;

        let csgArray = this.props.selectedControlStrategy;

        let threatUris = [];

        csgArray.forEach(csg => {
            let threatCsgTypes = csg.threatCsgTypes;
            threatUris = threatUris.concat(Object.keys(threatCsgTypes));
        });

        let threats = threatUris.map((threatUri) => {
            let threat = this.props.threats.find((threat) => threat["uri"] === threatUri);
            return threat;
        });

        return (
            <div className="panel-group accordion">
                <Panel defaultExpanded bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            Instances
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            <div className="container-fluid">
                                {csgArray.map((controlStrategy, csgIndex) => {
                                    let csgName = controlStrategy.label;
                                    let threatUris = Object.keys(controlStrategy.threatCsgTypes);
                                    return threatUris.map((threatUri, threatIndex) => {
                                        let threat = this.props.model.threats.find((threat) => {
                                            return (threat.uri === threatUri);
                                        });
                                        let csgKey = "csg" + csgIndex + "t" + threatIndex;
                                        if (threat) {
                                            return renderControlStrategy(csgName, controlStrategy, csgKey, threat, this.state, this.props, this, "csg-explorer");
                                        }
                                        else {
                                            console.warn("Could not locate threat for uri: ", threatUri);
                                        }
                                    });
                                })}
                            </div>
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel defaultExpanded bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            Threats
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
            </div>
        );
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

ControlStrategyAccordion.propTypes = {
    selectedControlStrategy: PropTypes.array,
    model: PropTypes.object,
    controlSets: PropTypes.object,
    assets: PropTypes.array,
    threats: PropTypes.array,
    updateThreat: PropTypes.func,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
};

export default ControlStrategyAccordion;
