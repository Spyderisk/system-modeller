import PropTypes from 'prop-types';
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";

class AbortRecommendationsModal extends Component {

    render() {
        const {modelId, abortRecommendations, ...modalProps} = this.props;

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>Abort Recommendations</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>Abort current recommendations calculation?</p>
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        bsStyle="primary"
                        onClick={this.props.onHide}
                        autoFocus
                        ref="closeButtonFooter">
                        Continue
                    </Button>
                    <Button
                        bsStyle="danger"
                        onClick={() => {
                            abortRecommendations(modelId)
                        }}>
                        Abort Recommendations
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

AbortRecommendationsModal.propTypes = {
    modelId: PropTypes.string,
    abortRecommendations: PropTypes.func,
    onHide: PropTypes.func,
};

export default AbortRecommendationsModal;
