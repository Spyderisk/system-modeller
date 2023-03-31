import React from "react";
import PropTypes from "prop-types";
import {Modal, FormGroup, FormControl, Col, Form, Button, OverlayTrigger, Tooltip, ControlLabel} from "react-bootstrap";
import {addNewModel} from "../../actions/api";
import * as Constants from "../../../common/constants.js";
import {MODEL_NAME_LIMIT} from '../../../common/constants';
import './NewModelModal.scss';

class NewModelModal extends React.Component {

    constructor(props) {
        super(props);

        this.submit = this.submit.bind(this);
        this.getOntologies = this.getOntologies.bind(this);
    }

    componentWillMount() {
        this.setState({
            name: "",
            ontology: {}
        });
        //console.log("componentWillMount: this.state = ", this.state);
    }

    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps: nextProps = ", nextProps);
        if (jQuery.isEmptyObject(nextProps.ontologies) || nextProps.ontologies.length === 0) {
            //console.warn("WARNING: componentWillReceiveProps: no knowledgebases are currently available");
        }
        else {
            if (!this.state.ontology.graph) {
                let ontologies = this.getOntologies(nextProps.ontologies);
                //console.log("Setting default ontology: ", ontologies[0]);
                this.setState({...self.state,
                    ontology: ontologies[0]
                })
            }
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        //don't update if modal remains hidden
        if (!this.props.show && !nextProps.show) {
            //console.log("shouldComponentUpdate: false (modal hidded)", nextProps, nextState);
            return false;
        }

        //console.log("shouldComponentUpdate: true", nextProps, nextState);
        return true;
    }

    getOntologies(ontologies) {
        if (jQuery.isEmptyObject(ontologies)) {
            //console.warn("WARNING: no knowledgebases are currently available");
            return [];
        }

        return ontologies;
    }

    render() {
        var self = this;
        let ontologies = this.getOntologies(this.props.ontologies);
        var hasOntologies = ontologies.length > 0;

        return (
            <Modal show={this.props.show} onHide={this.props.onHide} aria-labelledby="contained-modal-title-lg">
                <Modal.Header bsStyle="primary" closeButton>
                    <Modal.Title id="contained-modal-title-lg">Create New Model</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Form horizontal onSubmit={(e) => {
                        this.submit();
                        e.preventDefault()
                    }}>
                        <FormGroup controlId="formHorizontalText" className={"form-group-modal"}>
                            <Col componentClass={ControlLabel} sm={3}>
                                {"Model Name:"}
                            </Col>
                            <Col sm={9}>
                                <FormControl type="text" placeholder="Enter new model name" maxLength={MODEL_NAME_LIMIT.toString()}
                                             onChange={(e) => this.setState({
                                                 ...self.state,
                                                 name: e.nativeEvent.target.value.trim()
                                             })}
                                />
                            </Col>
                        </FormGroup>
                        {ontologies.length > 1 ?
                            <FormGroup controlId="formControlsSelect" className={"form-group-modal"}>
                            <Col componentClass={ControlLabel} sm={3}>
                                {"Knowledgebase:"}
                            </Col>
                            {hasOntologies ?
                                <Col sm={9}>
                                    <FormControl componentClass="select" placeholder="select"
                                        value={this.state.ontology.graph}
                                        onChange={(e) =>
                                            this.setState({ ...self.state, ontology: self.props.ontologies.find(o => o.graph === e.nativeEvent.target.value) })}>
                                        {ontologies.map(
                                            (ontology) => {
                                                return <option key={ontology.graph} value={ontology.graph}>{ontology.name}</option>
                                            }
                                        )};
                                    </FormControl>
                                </Col> :
                                <Col sm={9}>
                                    <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                        overlay={<Tooltip id="tooltip">{"Contact Administrator for access to knowledgebases"}</Tooltip>}>
                                        <span style={{ display: "inline-block", paddingTop: "7px", cursor: "default" }}>None currently available</span>
                                    </OverlayTrigger>
                                </Col>
                            }
                        </FormGroup>
                        :
                        <div>
                            {() => this.setState({ ...self.state, ontology: ontologies[0]})}
                        </div>
                        }
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={this.props.onHide}>Cancel</Button>
                    <Button bsStyle="primary" type="submit" onClick={this.submit} disabled={!hasOntologies || (this.state.name.trim().length == 0)}>Create New Model</Button>
                </Modal.Footer>
            </Modal>
        );
    }

    /**
     * Visual
     */

    submit() {
        console.log("submit");
        console.log(this.state);
        this.props.dispatch(addNewModel(this.state.name, this.state.ontology));
        this.props.onHide();
    }
}

NewModelModal.propTypes = {
    onHide: PropTypes.func,
    dispatch: PropTypes.func
};

export default NewModelModal;
