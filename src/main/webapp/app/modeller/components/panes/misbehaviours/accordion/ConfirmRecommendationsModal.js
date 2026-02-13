import PropTypes from 'prop-types';
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";

class ConfirmRecommendationsModal extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const {getRecommendations, ...modalProps} = this.props;

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>Confirm Recommendations</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>WARNING: the risk level of this consequence is at (or below) the acceptable level, 
                        and may not produce any results, or even time out.</p>
                    <p>Do you wish to proceed?</p>
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        onClick={this.props.onHide}
                        autoFocus>
                        Cancel
                    </Button>
                    <Button
                        bsStyle="danger"
                        onClick={getRecommendations}>
                        Continue Anyway
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

ConfirmRecommendationsModal.propTypes = {
    getRecommendations: PropTypes.func,
    onHide: PropTypes.func,
};

export default ConfirmRecommendationsModal;
