import React from "react";
import PropTypes from 'prop-types';
import {changeSelectedAsset} from "../../../../../actions/ModellerActions";
import {Row} from "react-bootstrap";
class InferredAssetPanel extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="roles detail-list">
                <div className="container-fluid">
                    {this.props.inferredAssets.sort((a, b) => a.label.localeCompare(b.label)).map((asset, index) => {
                        return (
                            <Row key={index} className="row-hover bare-list">
                                <span
                                    className="clickable"
                                    onClick={() => this.props.dispatch(changeSelectedAsset(asset["id"]))}
                                >
                                    {asset["label"]}
                                </span>
                            </Row>
                        );
                    })}
                </div>
            </div>
        );
    }

}

InferredAssetPanel.propTypes = {
    selectedAsset: PropTypes.object,
    inferredAssets: PropTypes.array
};

export default InferredAssetPanel;
