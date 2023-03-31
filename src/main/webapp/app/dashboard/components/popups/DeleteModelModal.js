import PropTypes from 'prop-types';
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";

class DeleteModelModal extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const {model, deleteModel, ...modalProps} = this.props;
        let modelName = model.name;

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>Delete Model</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>This will delete model "{modelName}" for all users and cannot be undone</p>
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
                            deleteModel(model["id"])
                        }}>
                        Delete
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

DeleteModelModal.propTypes = {
    model: PropTypes.object,
    deleteModel: PropTypes.func
};

export default DeleteModelModal;
