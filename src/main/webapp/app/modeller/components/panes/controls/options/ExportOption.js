import React from "react";
import PropTypes from 'prop-types';

import {Button, Well} from "react-bootstrap";
import {saveDownload} from "../../../../../common/actions/api";

class ExportOption extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <Well className="option-well">
                <h4>{"Export Model"}</h4>
                <hr />
                <Button bsStyle="primary" onClick={() => {
                    this.props.dispatch(saveDownload("./models/" + this.props.modelId + "/export"));
                }}>Download Exported Model</Button>
                <br/>
                <Button bsStyle="primary" style={{marginTop:"5px"}} onClick={() => {
                    this.props.dispatch(saveDownload("./models/" + this.props.modelId + "/exportAsserted"));
                }}>Download Exported Model (Asserted Assets Only)</Button>
            </Well>
        );
    }
}

ExportOption.propTypes = {
    modelId: PropTypes.string,
};

export default ExportOption;
