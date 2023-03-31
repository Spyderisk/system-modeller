import React from "react";
import PropTypes from 'prop-types';
import ReactDOM from "react-dom";

import {Modal, FormControl, Button} from "react-bootstrap"

class ChangeRelationModal extends React.Component {

    constructor(props) {
        super(props);

        this.handleSubmit = this.handleSubmit.bind(this);
    }

    render() {
        return (
            <div>
                <Modal show={this.props.show} onHide={this.props.onHide} backdrop={true} bsSize="small">
                    <Modal.Header closeButton>
                        <Modal.Title>Change Relation</Modal.Title>
                    </Modal.Header>

                    <Modal.Body>
                        {this.props.assets.length === 0 ?
                            <span className="text-bold text-danger">No valid assets!</span>
                            :
                            (this.props.isFrom ?
                                <div className="change-relation">
                                    <div className="select-from">
                                        <FormControl componentClass="select"
                                                     placeholder="Select..."
                                                     ref="select-from">
                                            {this.props.assets.sort((assetA, assetB) => assetA["label"] > assetB["label"]).map((asset, index) => {
                                                return <option key={index} value={asset["id"]}>
                                                    {asset["label"]}
                                                </option>
                                            })};
                                        </FormControl>
                                    </div>
                                    <div className="relation-type">
                                    <span
                                        className="text-primary">{this.props.formatRelationLabel(this.props.relation["label"])}</span>
                                    </div>
                                    <div className="asset-name">
                                        <span className="text-primary">{this.props.host["label"]}</span>
                                    </div>
                                </div>
                                :
                                <div className="change-relation">
                                    <div className="asset-name">
                                        <span className="text-primary">{this.props.host["label"]}</span>
                                    </div>
                                    <div className="relation-type">
                                    <span
                                        className="text-primary">{this.props.formatRelationLabel(this.props.relation["label"])}</span>
                                    </div>
                                    <div className="select-from">
                                        <FormControl componentClass="select"
                                                     placeholder="Select..."
                                                     ref="select-from">
                                            {this.props.assets.sort((assetA, assetB) => assetA["label"] > assetB["label"]).map((asset) => {
                                                return <option value={asset["id"]}>
                                                    {asset["label"]}
                                                </option>
                                            })};
                                        </FormControl>
                                    </div>
                                </div>)}
                    </Modal.Body>

                    <Modal.Footer>
                        <Button onClick={this.props.onHide}>Cancel</Button>
                        <Button bsStyle="primary" onClick={this.handleSubmit} disabled={this.props.assets.length === 0}>
                            Save changes</Button>
                    </Modal.Footer>

                </Modal>
            </div>
        );
    }

    handleSubmit() {
        //console.log("handleSubmit");
        var relation = this.props.relation;
        //console.log("relation:", relation);

        if (this.props.isFrom) {
            relation["fromID"] = ReactDOM.findDOMNode(this.refs["select-from"]).value;
            relation["toID"] = this.props.host["id"];
        } else {
            relation["toID"] = ReactDOM.findDOMNode(this.refs["select-from"]).value;
            relation["fromID"] = this.props.host["id"];
        }

        this.props.submit(relation);
        this.props.onHide();
    }

}

ChangeRelationModal.propTypes = {
    isFrom: PropTypes.bool,
    assets: PropTypes.array,
    relation: PropTypes.object,
    host: PropTypes.object,
    submit: PropTypes.func,
    show: PropTypes.bool,
    onHide: PropTypes.func,
    issue: PropTypes.object,
    formatRelationLabel: PropTypes.func
};

export default ChangeRelationModal;
