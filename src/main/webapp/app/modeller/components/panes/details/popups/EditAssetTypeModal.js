import React from "react";
import PropTypes from 'prop-types';
import {Modal, Button, FormControl, FormGroup, ControlLabel} from "react-bootstrap";

class EditAssetTypeModal extends React.Component {

    constructor(props) {
        super(props);

        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleAssetTypeUpdate = this.handleAssetTypeUpdate.bind(this);
        this.handleClose = this.handleClose.bind(this);
        this.handleOpen = this.handleOpen.bind(this);
    }

    componentWillMount() {
        //console.log("EditAssetTypeModal.componentWillMount");
        this.setState({
            originalAssetType: "",
            selectedAssetType: "",
            selectableAssets: [],
            show: false
        })
    }

    componentWillReceiveProps(nextProps) {
        //console.log("EditAssetTypeModal.componentWillReceiveProps");
        //console.log("EditAssetTypeModal: this.props.show: " + this.props.show);
        //console.log("EditAssetTypeModal: nextProps.show: " + nextProps.show);
        
        if (!this.props.show && nextProps.show){
            //console.log("EditAssetTypeModal: updating selectable (asserted) assets");
            let selectableAssets = this.props.palette_assets.filter((asset) => asset["assertable"] === true);
            //console.log(selectableAssets);
            let selectedAssetType = this.props.assetType["id"];
            console.log("EditAssetTypeModal: current type: " + selectedAssetType);
            this.setState({
                originalAssetType: selectedAssetType,
                selectedAssetType: selectedAssetType,
                selectableAssets: selectableAssets,
                show: true
            })
        }
    }

    handleClose() {
        this.setState({
            ...this.state,
            show: false
        })
    }

    handleOpen() {
        let selectableAssets = this.props.palette_assets.filter((asset) => asset["assertable"] === true);
        //console.log(selectableAssets);
        let selectedAssetType = this.props.assetType["id"];
        console.log("EditAssetTypeModal: current type: " + selectedAssetType);
        this.setState({
            originalAssetType: selectedAssetType,
            selectedAssetType: selectedAssetType,
            selectableAssets: selectableAssets,
            show: true
        });
    }

    render() {
        
        //console.log("EditAssetTypeModal.render: current state:");
        //console.log(self.state);
        //console.log("Setting selected value to: " + self.state.selectedAssetType);

        let saveButtonDisabled = (this.state.originalAssetType === this.state.selectedAssetType);


        
        return (
            <div>
                <Modal show={this.state.show} onHide={this.props.onHide} backdrop={true} bsSize="small">
                    <Modal.Header closeButton>
                        <Modal.Title>
                            Edit Asset Type
                        </Modal.Title>
                    </Modal.Header>

                    <Modal.Body>
                        <FormGroup controlId="editAssetType">
                            <ControlLabel>Asset Type</ControlLabel>
                            <FormControl componentClass="select"
                                onChange={this.handleAssetTypeUpdate}
                                value={this.state.selectedAssetType}
                                ref="select-asset-type">
                                {this.state.selectableAssets.map((asset, index) => {
                                    return <option key={index + 1} value={asset["id"]}>
                                        {asset["label"]}
                                    </option>
                                })};
                            </FormControl>
                        </FormGroup>
                        <p>Note: changing the asset's type may mean that some of the asset's relations will be deleted (those that are no longer valid).</p>
                    </Modal.Body>

                    <Modal.Footer>
                        <Button onClick={this.props.onHide}>Cancel</Button>
                        {
                            <Button bsStyle="primary" disabled={saveButtonDisabled} onClick={this.handleSubmit}>Save</Button>
                        }
                    </Modal.Footer>

                </Modal>
            </div>
        );
    }

    handleAssetTypeUpdate(e) {
        let selectedAssetType = e.nativeEvent.target.value;
        console.log("EditAssetTypeModal: handleAssetTypeUpdate: original asset type: " + this.state.originalAssetType);
        console.log("EditAssetTypeModal: handleAssetTypeUpdate: new selected asset type: " + selectedAssetType);
        
        this.setState({
            ...this.state,
            selectedAssetType: selectedAssetType
        });
    }

    handleSubmit() {
        console.log("EditAssetTypeModal: handleSubmit: new asset type: " + this.state.selectedAssetType);
        this.props.submit(this.state.selectedAssetType);
        this.props.onHide();
    }

}

EditAssetTypeModal.propTypes = {
    assetType: PropTypes.object,
    palette_assets: PropTypes.array,
    show: PropTypes.bool,
    submit: PropTypes.func,
    onHide: PropTypes.func
};

export default EditAssetTypeModal;
