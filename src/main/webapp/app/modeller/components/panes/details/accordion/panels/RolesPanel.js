import React from "react";
import PropTypes from 'prop-types';

class RolesPanel extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="roles detail-list">
                <div className="container-fluid">
                    {this.props.selectedAsset["threats"].map((threat) => {
                        var nodes = threat["nodes"];
                        if (nodes === null) nodes = [];
                        return (
                            <div className="row roles-info">
                                <span className="col-md-12">
									{/*This is probably wrong - asset contains the label, not the id*/}
                                    {nodes.find((node) => node["asset"] === this.props.selectedAsset["id"]) !== undefined ?
                                        nodes.find((node) => node["role"] === this.props.selectedAsset["id"])["role"]
                                        : "No roles found"}
                                    {" in "}
                                    {threat["patternName"]}
                                </span>
                            </div>
                        );
                    })}
                </div>
            </div>
        );
    }

}

RolesPanel.propTypes = {
    selectedAsset: PropTypes.object
};

export default RolesPanel;
