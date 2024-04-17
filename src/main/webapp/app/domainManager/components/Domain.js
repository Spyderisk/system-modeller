import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Button, Col, Panel, Row } from "react-bootstrap";
import { saveRestrictedDownload } from "../../common/actions/api";
import { deleteDomainModel } from "../actions/api";
import DeleteDomainModelModal from "./DeleteDomainModelModal";

class Domain extends Component {

    constructor(props) {
        super(props);

        this.state = {
            accessEditor: false,
            deleteDomainModelModal: false
        }
        
        this.deleteDomainModel = this.deleteDomainModel.bind(this);
    }

    render() {
        let { domainUri, ontology, toggleModal, toggleAccessListModal, dispatch } = this.props;

        return (
            <Row>
                <div className="ontology">
                    <Panel>
                        <Row>
                            <Col xs={8}>
                                <h4>
                                    {ontology.label}
                                </h4>
                                <p>Version: {ontology.version}</p>
                                <p>Graph: {domainUri}</p>
                            </Col>
                            <Col xs={4}>
                                <span style={{ float: "right" }}>
                                    {/* <Button bsStyle="primary" onClick={() => toggleAccessListModal(true, domainUri, ontology)}
                                    >Access List</Button> */}
                                    <Button bsStyle="primary"
                                        disabled={ontology.loaded === false}
                                        onClick={() => {
                                            let d = domainUri.substr(domainUri.lastIndexOf("/") + 1);
                                            dispatch(saveRestrictedDownload("./domains/" + d + "/export"));
                                        }}>Download</Button>
                                    <Button bsStyle="primary"
                                        onClick={() => toggleModal(true, domainUri, ontology, false)}
                                    >Upload new version</Button>
                                    <Button bsStyle="danger"
                                        onClick={() => this.setState({...this.state, deleteDomainModelModal: true})}
                                    >Delete</Button>
                                </span>
                            </Col>
                        </Row>
                        <Row>
                            <Col xs={12}>
                                {ontology.comment}
                            </Col>
                        </Row>
                    </Panel>
                </div>

                <DeleteDomainModelModal 
                    show={this.state.deleteDomainModelModal} 
                    domainUri={domainUri} 
                    ontology={ontology} 
                    deleteDomainModel={this.deleteDomainModel}
                    onHide={() => this.setState({...this.state, deleteDomainModelModal: false})}/>
            </Row>
        )
    }

    deleteDomainModel(domainUri) {
        this.setState({...this.state, deleteDomainModelModal: false});
        this.props.dispatch(deleteDomainModel(domainUri));
    }

}

Domain.propTypes = {
    domainUri: PropTypes.string,
    ontology: PropTypes.object,
    toggleModal: PropTypes.func,
    toggleAccessListModal: PropTypes.func,
    dispatch: PropTypes.func,
};

export default Domain;
