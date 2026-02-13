import PropTypes from 'prop-types';
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";

class ConfirmAttackPathModal extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const {getAttackPath, ...modalProps} = this.props;

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>Confirm Attack Path</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>WARNING: the risk level of this consequence is at (or below) the acceptable level, 
                        and may not produce any results, or even time out.</p>
                    <p>Do you wish to proceed?</p>
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        onClick={this.props.onHide}
                        autoFocus
                        ref="cancelButton">
                        Cancel
                    </Button>
                    <Button
                        bsStyle="danger"
                        onClick={getAttackPath}>
                        Continue Anyway
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

ConfirmAttackPathModal.propTypes = {
    getAttackPath: PropTypes.func,
    onHide: PropTypes.func,
};

export default ConfirmAttackPathModal;
