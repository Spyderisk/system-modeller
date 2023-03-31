import React from "react";
import PropTypes from 'prop-types';
import ReactDOM from "react-dom";

import {Well} from "react-bootstrap";

class ScreenshotOption extends React.Component {

    render() {
        let model = this.props.model;
        return (
            <Well className="option-well">
                <h4>{"Model Information"}</h4>
                <hr />
                <div className="information-option">
                    <p><span>Model Name:</span> {model["name"]}</p>
                    <p><span>Knowledgebase:</span> {model["domainGraph"].replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-", "").toUpperCase()}</p>
                    <p><span>Description:</span> {model["description"] !== null ? model["description"] : "Not available"}</p>
                    <p><span>Created:</span> {new Date(model["created"]).toString()}</p>
                    <p><span>Last Modified:</span> {new Date(model["modified"]).toString()}</p>
                </div>
            </Well>
        );
    }

    handleImageSubmit() {
        var data = new FormData();

        data.append("file", ReactDOM.findDOMNode(this.refs["file-upload-image"]).files[0]);

        this.props.dispatch(updateUploadProgress(5));
        this.props.dispatch(uploadScreenshot(this.props.modelId, data));
    }

}

ScreenshotOption.propTypes = {
    modelId: PropTypes.string,
    upload: PropTypes.object,
    dispatch: PropTypes.func,
};

export default ScreenshotOption;
