import React, {Fragment} from "react";
import PropTypes from 'prop-types';
import {Button, Checkbox, FormControl, FormGroup} from "react-bootstrap";
import renderControlStrategy from "../../../csgExplorer/ControlStrategyRenderer";
import {connect} from "react-redux";

class ControlStrategiesPanel extends React.Component {

    constructor(props) {
        super(props);

        this.getCsgsMap = this.getCsgsMap.bind(this);
        this.getCsgControlSets = this.getCsgControlSets.bind(this);

        this.state = {
            controlStrategies: this.getCsgsMap(props),
            updatingControlSets: {},
            controlSets: this.getCsgControlSets(props)
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            controlStrategies: this.getCsgsMap(nextProps),
            updatingControlSets: {},
            controlSets: this.getCsgControlSets(nextProps)
        });
    }

    getCsgsMap(props) {
        //Get pre-filtered array of CSGs
        let csgsAsArray = props.filteredCsgs;

        var csgsAsMap = csgsAsArray.reduce(function(map, csg) {
            map[csg.uri] = csg;
            return map;
        }, {});

        return csgsAsMap;
    }

    getCsgControlSets(props) {
        let controlSets = {};

        props.filteredCsgs.forEach(csg => {
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
        let threat = this.props.threat;
        let isComplianceThreat = this.props.threat.isComplianceThreat;

        const csgDict = {}, propCsgs = {};

        let csgsAsArray = Object.values(this.state.controlStrategies);

        csgsAsArray.forEach(controlStrategy => {
            const csgFullName = controlStrategy.label;

            //Only use "main" (=generic) CSG name
            let csgName = csgFullName;
            if (csgFullName && csgFullName.indexOf('-') > -1) {
                const frags = csgFullName.split('-');
                csgName = frags[frags.length - 1];
            }

            //Add to CSG dict
            csgDict[csgName] = controlStrategy;
            propCsgs[csgName] = controlStrategy;
        });

        // Get sorted CSG names
        const csgNames = Object.keys(csgDict).sort();

        return (
            <div className="container-fluid">

                <span>{((csgNames === null) || (csgNames.length === 0)) ? "No control strategies found" : ""}</span>
                {csgNames.sort().map((csgName, index) => {
                    const controlStrategy = csgDict[csgName];
                    return renderControlStrategy(csgName, controlStrategy, index, threat, this.state, this.props, this, "threat-explorer");
                })}
                {!isComplianceThreat && this.props.authz.userEdit ? <AcceptancePanel acceptance={this.props.threat["acceptanceJustification"]}
                    threat={this.props.threat}
                    activateAcceptancePanel={this.props.activateAcceptancePanel}
                    submit={(a, b) => this.props.toggleAcceptThreat({ acceptThreat: a, reason: b })} />
                    :
                    <div></div>
                }
            </div>
        );
    }
}

ControlStrategiesPanel.propTypes = {
    threat: PropTypes.object,
    asset: PropTypes.object,
    filteredCsgs: PropTypes.array,
    controlStrategies: PropTypes.object,
    controlSets: PropTypes.object,
    getControl: PropTypes.func,
    updateThreat: PropTypes.func,
    activateAcceptancePanel: PropTypes.func,
    toggleAcceptThreat: PropTypes.func,
    developerMode: PropTypes.bool,
};

let mapStateToProps = function (state) {
    return {
        assets: state.modeller.model.assets,
    }
};

export default connect(mapStateToProps)(ControlStrategiesPanel);

class AcceptancePanel extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            acceptThreat: props.acceptance !== null,
            reason: props.acceptance === null ? null : props.acceptance,
            isSaving: false
        };
        this.submitForm = this.submitForm.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.threat.id !== nextProps.threat.id) {
            this.setState({
                acceptThreat: nextProps.acceptance !== null,
                reason: nextProps.acceptance === null ? null : nextProps.acceptance,
                isSaving: false
            });
        }
        else {
            if (this.props.acceptance != nextProps.acceptance) {
                this.setState({
                    acceptThreat: nextProps.acceptance !== null,
                    reason: nextProps.acceptance === null ? null : nextProps.acceptance,
                    isSaving: false
                });
            }
        }
    }

    submitForm() {
        let {acceptThreat, reason} = this.state;
        
        if (reason !== null)
            reason = reason.trim();
            
        //do not permit an accepted threat without a reason
        if (acceptThreat && (reason === null || reason === "")) {
            alert("Please enter a reason");
            return;
        }
        
        if (reason === "") {
            acceptThreat = false;
        }
        if (!acceptThreat) {
            reason = null;
        }
        this.props.submit(acceptThreat, reason);
        this.setState({
            ...this.state,
            acceptThreat: acceptThreat,
            reason: acceptThreat ? reason : "",
            isSaving: true
        })
    }

    render() {
        let {acceptThreat, reason} = this.state;
        let submitDisabled = (reason === null || reason.trim() === "" || (reason.trim() === this.props.acceptance));
        
        return (
            <Fragment>
                <FormGroup>
                    <Checkbox
                        checked={this.state.acceptThreat}
                        onChange={(event) => {
                            let checked = event.nativeEvent.target.checked;
                            let original_reason = this.props.acceptance;
                            if (!checked) {
                                if (original_reason !== null) {
                                    this.props.submit(false, null);
                                }
                                else {
                                    console.log("acceptance value not changed (ignoring)");
                                }
                            }
                            this.setState({
                                ...this.state,
                                acceptThreat: event.nativeEvent.target.checked,
                                reason: null,
                            });
                        }}>
                        Accept threat
                        {reason !== null && reason.trim() !== this.props.acceptance
                            ? <span> (Unsaved)</span>
                            : null}
                    </Checkbox>
                </FormGroup>
                <FormGroup>
                    <FormControl componentClass="textarea" id="acceptanceJustification" rows="3"
                        style={{'display': acceptThreat ? 'block' : 'none', 'width': '100%'}}
                        placeholder="Please explain why you think this threat is acceptable"
                        value={reason || ""}
                        onMouseOver={(e) => {
                            //console.log("AcceptancePanel: onMouseOver ");
                            this.props.activateAcceptancePanel(true);
                        }}
                        onMouseOut={(e) => {
                            //console.log("AcceptancePanel: onMouseOut ");
                            this.props.activateAcceptancePanel(false);
                        }}
                        onFocus={(e) => {
                            //console.log("AcceptancePanel: onFocus");
                            //this.props.activateAcceptancePanel(true);
                        }}
                        onChange={(event) => this.setState({
                            ...this.state,
                            reason: event.nativeEvent.target.value
                        })}
                        onBlur={(e) => {
                            /*
                            console.log("Threat filter: onBlur");
                            this.props.activateAcceptancePanel(false);
                            */
                        }}
                    />
                </FormGroup>
                <FormGroup>
                    <Button bsStyle="primary" bsSize="xsmall"
                        style={{'display': acceptThreat ? 'inline-block' : 'none'}}
                        disabled={submitDisabled}
                        onClick={this.submitForm}>
                    Save
                    {this.state.isSaving
                        ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw"/>
                        : null}
                    </Button>
                </FormGroup>
            </Fragment>
        )
    }
}

AcceptancePanel.defaultProps = {
    acceptance: "",
    threat: {},
    activateAcceptancePanel: () => {
    },
    submit: () => {
    }
};

AcceptancePanel.propTypes = {
    acceptance: PropTypes.string,
    threat: PropTypes.object,
    levels: PropTypes.array,
    activateAcceptancePanel: PropTypes.func,
    submit: PropTypes.func
};
