import React, {Fragment} from "react";
import PropTypes from 'prop-types';
import {Modal, Button, FormControl, FormGroup, ControlLabel} from "react-bootstrap";

class AddRelationModal extends React.Component {

    constructor(props) {
        super(props);

        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleRelUpdate = this.handleRelUpdate.bind(this);
        this.handleAssetUpdate = this.handleAssetUpdate.bind(this);
    }

    componentWillMount() {
        this.setState({
            selectedRel: "",
            selectedAsset: "",
            selectableAssets: []
        })
    }

    componentWillReceiveProps(nextProps) {
        if(nextProps.show && !this.props.show){
            this.setState({
                selectedRel: "",
                selectedAsset: "",
                selectableAssets: []
            })
        }
    }

    render() {
        var self = this;
        return (
            <div>
                <Modal show={this.props.show} onHide={this.props.onHide} backdrop={true} bsSize="small">
                    <Modal.Header closeButton>
                        <Modal.Title>
                            Add {this.props.isIncoming ? "Incoming" : "Outgoing"} Relation
                        </Modal.Title>
                    </Modal.Header>
                    {this.props.links !== null ?
                        <Modal.Body>
                            <FormGroup controlId="relation">
                                <ControlLabel>Relation</ControlLabel>
                                <FormControl componentClass="select"
                                    placeholder="Select..."
                                    onChange={this.handleRelUpdate}
                                    value={self.state.selectedRel}
                                    ref="select-rel"
                                    disabled={this.props.links === null}>
                                    <option key={0} disabled value="">Select relation type...</option>
                                    {this.props.links.sort((linkA, linkB) => {
                                        return linkA.label > linkB.label
                                    }).map((link, index) => {
                                        return <option key={index + 1} value={link["type"]}>
                                            {link["label"]}
                                        </option>
                                    })};
                                </FormControl>
                            </FormGroup>
                            {!this.props.isIncoming ?
                                <Fragment>
                                    <FormGroup>
                                        <span><b>{'From: '}</b></span>
                                        <span style={{ wordBreak: 'break-all' }}>{this.props.host["label"]}</span>
                                    </FormGroup>
                                    <FormGroup controlId="to-asset">
                                        <ControlLabel>
                                            To
                                        </ControlLabel>
                                        <FormControl componentClass="select"
                                            placeholder="Select..."
                                            onChange={this.handleAssetUpdate}
                                            ref="select-to">
                                            <option key={0} disabled selected value="">Select asset...</option>
                                            {self.state.selectableAssets.sort((assetA, assetB) => assetA["label"] > assetB["label"]).map((asset, index) => {
                                                return <option key={index + 1} value={asset["id"]}>
                                                    {asset["label"]}
                                                </option>
                                            })};
                                        </FormControl>
                                    </FormGroup>
                                </Fragment>
                            :
                                <Fragment>
                                    <FormGroup controlId="from-asset">
                                        <ControlLabel>
                                            From
                                        </ControlLabel>
                                        <FormControl componentClass="select"
                                            placeholder="Select..."
                                            onChange={this.handleAssetUpdate}
                                            value={self.state.selectedAsset}
                                            ref="select-from">
                                            <option key={0} disabled selected value="">Select asset...</option>
                                            {self.state.selectableAssets.sort((assetA, assetB) => assetA["label"] > assetB["label"]).map((asset, index) => {
                                                return <option key={index + 1} value={asset["id"]}>
                                                    {asset["label"]}
                                                </option>
                                            })};
                                        </FormControl>
                                    </FormGroup>
                                    <FormGroup>
                                        <span><b>{'To: '}</b></span>
                                        <span style={{ wordBreak: 'break-all' }}>{this.props.host["label"]}</span>
                                    </FormGroup>
                                </Fragment>
                            }
                        </Modal.Body>
                    :
                        <Modal.Body>
                            <p>No relations available</p>
                        </Modal.Body>
                    }
                    <Modal.Footer>
                        <Button onClick={this.props.onHide} ref="closeButtonFooter">Cancel</Button>
                        {
                            this.state.selectedRel === "" || this.state.selectedAsset === "" ?
                                <Button bsStyle="primary" disabled onClick={this.handleSubmit}>Create Relation</Button>
                                :
                                <Button bsStyle="primary" onClick={this.handleSubmit}>Create Relation</Button>
                        }
                    </Modal.Footer>

                </Modal>
            </div>
        );
    }

    handleSubmit() {
        var assetTo = this.props.isIncoming ? this.props.host["id"] : this.state.selectedAsset,
            assetFrom = this.props.isIncoming ? this.state.selectedAsset : this.props.host["id"],
            relType = this.state.selectedRel;
            
        let rel = this.props.links.find((link) => link['type'] === relType);

        this.props.submit(assetFrom, assetTo, rel);
    }

    handleRelUpdate(e) {
        var options = this.props.links.find((link) => link['type'] == [e.nativeEvent.target.value])['options'];
        var outAssets = this.props.assets.filter((asset) => {
            return options.includes(asset['type']) && asset['id'] !== this.props.host['id'];
        });

        this.setState({
            ...this.state,
            selectedRel: e.nativeEvent.target.value,
            selectedAsset: "",
            selectableAssets: outAssets
        });
    }

    handleAssetUpdate(e) {
        this.setState({
            ...this.state,
            selectedAsset: e.nativeEvent.target.value
        })
    }

}

AddRelationModal.propTypes = {
    isIncoming: PropTypes.bool,
    assets: PropTypes.array,
    links: PropTypes.array,
    host: PropTypes.object,
    show: PropTypes.bool,
    submit: PropTypes.func,
    onHide: PropTypes.func
};

export default AddRelationModal;
