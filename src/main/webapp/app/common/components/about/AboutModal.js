import PropTypes from "prop-types";
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";

class AboutModal extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const {dispatch, info, ...modalProps} = this.props;

        if (!info) {
            return null;
        }

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>About</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>Spyderisk Version: <span>{info.spyderiskVersion}</span></p>
                    <p>Spyderisk Commit SHA: <span>{info.spyderiskCommitSha}</span></p>
                    <p>Spyderisk Commit Timestamp: <span>{info.spyderiskCommitTimestamp}</span></p>
                    <p>Spyderisk Adaptor Version: <span>{info.spyderiskAdaptorVersion}</span></p>
                </Modal.Body>
                <Modal.Footer>
                    <Button
                        onClick={modalProps.onHide}
                        autoFocus>
                        Cancel
                    </Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

AboutModal.propTypes = {
    info: PropTypes.object,
    dispatch: PropTypes.func,
};

export default AboutModal;
