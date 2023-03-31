import PropTypes from 'prop-types';
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";

class DeleteDomainModelModal extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const {domainUri, ontology, deleteDomainModel, ...modalProps} = this.props;
        console.log("domainUri: ", domainUri);
        console.log("ontology: ", ontology);
        let domainModelName = ontology.label;

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>Delete Knowledgebase</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>This will delete domain model "{domainModelName}" for all users and cannot be undone</p>
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        onClick={this.props.onHide}
                        autoFocus
                        ref="closeButtonFooter">
                        Cancel
                    </Button>
                    <Button
                        bsStyle="danger"
                        onClick={() => {
                            deleteDomainModel(domainUri)
                        }}>
                        Delete
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

DeleteDomainModelModal.propTypes = {
    domainUri: PropTypes.string,
    ontology: PropTypes.object,
    deleteDomainModel: PropTypes.func
};

export default DeleteDomainModelModal;
