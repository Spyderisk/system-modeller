import React, {Fragment} from "react";
import PropTypes from 'prop-types';
import {Modal, Button, FormControl, FormGroup, ControlLabel} from "react-bootstrap";

class AddRelationModal extends React.Component {

    constructor(props) {
        super(props);

        this.getLinkTypesMap = this.getLinkTypesMap.bind(this);
        this.getConnectableAssets = this.getConnectableAssets.bind(this);
        this.getAvaialableLinksForAsset = this.getAvaialableLinksForAsset.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleRelUpdate = this.handleRelUpdate.bind(this);
        this.handleAssetUpdate = this.handleAssetUpdate.bind(this);
    }

    componentWillMount() {
        this.setState({
            selectedAsset: "",
            selectedRel: "",
            selectableAssets: [],
            selectableLinks: [],
            linkTypesMap: {}
        })
    }

    componentWillReceiveProps(nextProps) {
        let linkTypesMap = this.getLinkTypesMap(nextProps);
        let assetIds = this.getConnectableAssets(nextProps, linkTypesMap);
        let selectableAssets = this.props.assets.filter((asset) => {
            return assetIds.has(asset['id']);
        });

        if (nextProps.show && !this.props.show){
            this.setState({
                selectedAsset: "",
                selectedRel: "",
                selectableAssets: selectableAssets,
                linkTypesMap: linkTypesMap
            })
        }
    }

    getLinkTypesMap(props) {
        let linkTypesMap = props.isIncoming ? props.linkFromTypes(props.host['type']) : props.linkToTypes(props.host['type']);
        return linkTypesMap;
    }

    getConnectableAssets(props, linkTypesMap) {
        let linkTypes = Object.values(linkTypesMap);

        let assetsSet = new Set();

        linkTypes.forEach((linkData, i) => {
            let assets = linkData.assets;
            assets.forEach((assetid, i) => {
                if (assetid !== props.host.id) {
                    assetsSet.add(assetid);
                }
            });
        });

        return assetsSet;
    }

    getAvaialableLinksForAsset(assetId) {
        let linkTypesMap = this.state.linkTypesMap;
        let linksSet = new Set();

        Object.keys(linkTypesMap).forEach((linkTypeUri, i) => {
            let linkType = linkTypesMap[linkTypeUri];
            let assets = linkType.assets;
            if (assets.includes(assetId)) {
                linksSet.add(linkTypeUri);
            }
        });

        let selectableLinkUris = Array.from(linksSet);

        let selectableLinks = selectableLinkUris.map((uri, i) => {
            let linkType = linkTypesMap[uri];
            linkType.uri = uri;
            return linkType;
        });

        return selectableLinks;
    }

    render() {
        let self = this;

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
                                            {self.state.selectableAssets.sort((assetA, assetB) => assetA["label"].localeCompare(assetB["label"])).map((asset, index) => {
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
                                            {self.state.selectableAssets.sort((assetA, assetB) => assetA["label"].localeCompare(assetB["label"])).map((asset, index) => {
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
                            <FormGroup controlId="relation">
                                <ControlLabel>Relation</ControlLabel>
                                <FormControl componentClass="select"
                                    placeholder="Select..."
                                    onChange={this.handleRelUpdate}
                                    value={self.state.selectedRel}
                                    ref="select-rel"
                                    disabled={this.props.links === null}>
                                    <option key={0} disabled value="">Select relation type...</option>
                                    {this.state.selectableLinks.sort((linkA, linkB) => {
                                        return linkA.label.localeCompare(linkB.label)
                                    }).map((link, index) => {
                                        return <option key={index + 1} value={link["uri"]}>
                                            {link["label"]}
                                        </option>
                                    })};
                                </FormControl>
                            </FormGroup>
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
        let assetTo = this.props.isIncoming ? this.props.host["id"] : this.state.selectedAsset,
            assetFrom = this.props.isIncoming ? this.state.selectedAsset : this.props.host["id"],
            relType = this.state.selectedRel;
            
        let rel = this.props.links.find((link) => link['type'] === relType);

        this.props.submit(assetFrom, assetTo, rel);
    }

    handleRelUpdate(e) {
        let selectedRel = e.nativeEvent.target.value;

        this.setState({
            ...this.state,
            selectedRel: selectedRel
        });
    }

    handleAssetUpdate(e) {
        let selectedAsset = e.nativeEvent.target.value;
        let selectableLinks = this.getAvaialableLinksForAsset(selectedAsset);

        this.setState({
            ...this.state,
            selectedAsset: selectedAsset,
            selectedRel: "", //clear relations selection
            selectableLinks: selectableLinks
        })
    }

}

AddRelationModal.propTypes = {
    isIncoming: PropTypes.bool,
    assets: PropTypes.array,
    linkFromTypes: PropTypes.func,
    linkToTypes: PropTypes.func,
    links: PropTypes.array,
    host: PropTypes.object,
    show: PropTypes.bool,
    submit: PropTypes.func,
    onHide: PropTypes.func
};

export default AddRelationModal;
