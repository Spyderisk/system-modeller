import PropTypes from "prop-types";
import React, { Component } from "react";
import { Button, Modal } from "react-bootstrap";

class AboutModal extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const {dispatch, info, ...modalProps} = this.props;
        const logo = "spyderisk.svg";
        const logo_path = process.env.config.API_END_POINT + "/images/" + logo;

        const website = "https://github.com/Spyderisk";
        const licenseFile = "LICENSE.md";
        const licenseLink = "https://github.com/Spyderisk/system-modeller/blob/dev/LICENSE.md";
        const contributorsFile = "CONTRIBUTORS.md";
        const contributorsLink = "https://github.com/Spyderisk/system-modeller/blob/dev/CONTRIBUTORS.md";

        if (!info) {
            return null;
        }

        return (
            <Modal {...modalProps}>
                <Modal.Header closeButton>
                    <Modal.Title>About Spyderisk</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <img id="logo" src={logo_path}/>
                    <p><strong>Website: </strong><a href={website} target="_blank">{website}</a></p>
                    <p><strong>Version: </strong><span>{info.spyderiskVersion}</span></p>
                    <p><strong>Commit SHA: </strong><span>{info.spyderiskCommitSha}</span></p>
                    <p><strong>Commit Timestamp: </strong><span>{info.spyderiskCommitTimestamp}</span></p>
                    <p><strong>Adaptor Version: </strong><span>{info.spyderiskAdaptorVersion}</span></p>
                    <p><strong>License: </strong><a href={licenseLink} target="_blank">{licenseFile}</a></p>
                    <p><strong>Contributors: </strong><a href={contributorsLink} target="_blank">{contributorsFile}</a></p>
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
