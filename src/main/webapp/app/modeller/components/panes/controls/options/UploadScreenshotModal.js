import React from "react";
import PropTypes from 'prop-types';

import {Modal, Button} from "react-bootstrap"

import {uploadScreenshot} from "../../../../actions/ModellerActions";

class UploadScreenshotModal extends React.Component {

    constructor (props) {
        super(props);

        this.handleSubmit = this.handleSubmit.bind(this);
    }

    render () {
        return (
            <div>
                <Modal {...this.props} backdrop={true} bsSize="small">
                    <Modal.Header closeButton>
                        <Modal.Title>Upload Screenshot</Modal.Title>
                    </Modal.Header>

                    <Modal.Body>
                        <Button bsStyle="primary" onClick={this.handleSubmit}>Generate screenshot</Button>
                        <span>Warning: Doing this will overwrite previous screenshots!</span>
                    </Modal.Body>

                    <Modal.Footer>
                        <Button onClick={this.props.onHide}>Close</Button>
                    </Modal.Footer>
                </Modal>
            </div>
        );
    }

    handleSubmit () {
        this.props.dispatch(uploadScreenshot(this.props.modelId));
    }

}

UploadScreenshotModal.propTypes = {
    modelId: PropTypes.string,
    dispatch: PropTypes.func,
    onHide: PropTypes.func
};

export default UploadScreenshotModal;
