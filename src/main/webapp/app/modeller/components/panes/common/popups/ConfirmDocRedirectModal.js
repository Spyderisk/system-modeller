import PropTypes from 'prop-types';
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";
import {openDomainDoc} from "../../../../../common/documentation/documentation";

class ConfirmDocRedirectModal extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const {model, selectedDocEntity, ...modalProps} = this.props;

        let domainVersion = this.props.model.domainVersion;
        let validatedDomainVersion = this.props.model.validatedDomainVersion;
        let versionWarningText = "Requested knowledgebase version (" + validatedDomainVersion + ") does not match current knowledgebase (" + domainVersion + "). Continue to docs anyway?";

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>Versions Mismatch!</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>{versionWarningText}</p>
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        onClick={this.props.onHide}
                        autoFocus
                        ref="closeButtonFooter">
                        Cancel
                    </Button>
                    <Button
                        bsStyle="primary"
                        onClick={() => {
                            //Close this dialog
                            this.props.onHide();

                            //Open the domain doc tab
                            openDomainDoc(model.id, this.props.selectedDocEntity);
                        }}>
                        Continue
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

ConfirmDocRedirectModal.propTypes = {
    model: PropTypes.object,
    selectedDocEntity: PropTypes.string,
    dispatch: PropTypes.func,
};

export default ConfirmDocRedirectModal;
