import React from "react";
import PropTypes from "prop-types";
import axios from "axios";
import {Button} from "react-bootstrap";
import {
    retrieveMetaData,
    updateMetaData,
    refreshAssetTwasAndImpacts
} from "../../../../../actions/ModellerActions";

/*
 * Generic "external asset binding" widget
 *
 * Binding state lives in asset metadata under these reserved generic keys:
 *   externalBinding.system  -> provider name (e.g. the capability's providerName)
 *   externalBinding.id      -> the external identifier the user typed
 *
 * Unbound: a text input for the external id + a "Link" button.
 * Bound:   shows the linked id + "Unlink" and "Refresh" buttons.
 *
 * Link/Refresh call the ADAPTOR's bind endpoint, which fetches the external
 * properties, translates them and applies the impacts/TWAs to the asset. The
 * widget itself records the binding metadata (above keys) on success and clears
 * it on Unlink.
 * Applied impacts/TWAs are left as-is on Unlink.
 */

const META_KEY_SYSTEM = "externalBinding.system";
const META_KEY_ID = "externalBinding.id";

class ExternalBindingPanel extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            inputId: "",
            busy: false,
            message: null,   // {type: "info"|"error", text: string}
        };
        this.onChangeInput = this.onChangeInput.bind(this);
        this.onClickLink = this.onClickLink.bind(this);
        this.onClickUnlink = this.onClickUnlink.bind(this);
        this.onClickRefresh = this.onClickRefresh.bind(this);
        this.callAdaptorBind = this.callAdaptorBind.bind(this);
        this.getMetaData();
    }

    callAdaptorBind(externalId) {
        let base = process.env.config.API_END_POINT;   // e.g. "/system-modeller"
        let provider = this.props.capability.providerName;
        let url = `${base}/adaptor/api/v2/${provider}/models/${this.props.modelId}/bind`;
        this.setState({busy: true, message: {type: "info", text: "Applying " + this.props.capability.providerLabel + " data…"}});
        axios.post(url, {asset_id: this.props.asset.id, external_id: externalId})
            .then((resp) => {
                let applied = (resp.data && resp.data.applied) || {};
                let nTwas = (applied.twas || []).length;
                let nImp = (applied.impacts || []).length;
                // Record the binding as generic asset metadata
                let metaData = (this.props.asset.metaData || [])
                    .filter((m) => m.key !== META_KEY_SYSTEM && m.key !== META_KEY_ID)
                    .concat([
                        {key: META_KEY_SYSTEM, value: provider},
                        {key: META_KEY_ID, value: externalId},
                    ]);
                this.props.dispatch(updateMetaData(this.props.modelId, this.props.asset, metaData));
                // Refresh the panels of affected TWAs/impacts in place
                this.props.dispatch(refreshAssetTwasAndImpacts(this.props.modelId, this.props.asset.id));
                this.setState({busy: false, message: {type: "info",
                    text: `Applied ${nTwas} TWAs and ${nImp} impacts.`}});
            })
            .catch((err) => {
                let detail = (err.response && err.response.data && err.response.data.detail) || err.message;
                this.setState({busy: false, message: {type: "error", text: "Failed: " + detail}});
            });
    }

    componentDidUpdate(prevProps) {
        if (this.props.asset && prevProps.asset && prevProps.asset.id !== this.props.asset.id) {
            this.setState({inputId: ""});
            this.getMetaData();
        }
    }

    getMetaData() {
        if (this.props.asset) {
            this.props.dispatch(retrieveMetaData(this.props.modelId, this.props.asset));
        }
    }

    /* Returns {system, id} from the asset's metadata, or null if unbound. */
    getBinding() {
        let asset = this.props.asset;
        if (!asset || !asset.metaData) return null;
        let system = asset.metaData.find((m) => m.key === META_KEY_SYSTEM);
        let id = asset.metaData.find((m) => m.key === META_KEY_ID);
        if (system && id && id.value) {
            return {system: system.value, id: id.value};
        }
        return null;
    }

    onChangeInput(e) {
        this.setState({inputId: e.target.value});
    }

    onClickLink() {
        let id = this.state.inputId.trim();
        if (!id) return;
        this.callAdaptorBind(id);
    }

    onClickUnlink() {
        let metaData = (this.props.asset.metaData || [])
            .filter((m) => m.key !== META_KEY_SYSTEM && m.key !== META_KEY_ID);
        this.props.dispatch(updateMetaData(this.props.modelId, this.props.asset, metaData));

        // NOTE: applied impacts/TWAs are not reverted
        this.setState({message: {type: "info", text: "Unlinked."}});
    }

    onClickRefresh() {
        let binding = this.getBinding();
        if (binding) {
            this.callAdaptorBind(binding.id);
        }
    }

    renderUnbound(canEdit) {
        let label = this.props.capability.providerLabel;
        return (
            <div>
                <p>Not linked to {label}.</p>
                {canEdit &&
                    <div style={{display: "flex", gap: "0.5em", alignItems: "center"}}>
                        <input type="text"
                               placeholder={`${label} id`}
                               value={this.state.inputId}
                               onChange={this.onChangeInput}
                               style={{flex: 1}}/>
                        <Button bsStyle="primary" bsSize="small"
                                disabled={!this.state.inputId.trim() || this.state.busy}
                                onClick={this.onClickLink}>
                            Link
                        </Button>
                    </div>
                }
            </div>
        );
    }

    renderBound(binding, canEdit) {
        let label = this.props.capability.providerLabel;
        return (
            <div>
                <p>Linked to {label}: <b>{binding.id}</b></p>
                {canEdit &&
                    <div style={{display: "flex", gap: "0.5em"}}>
                        <Button bsStyle="default" bsSize="small" disabled={this.state.busy}
                                onClick={this.onClickRefresh}>
                            Refresh
                        </Button>
                        <Button bsStyle="danger" bsSize="small" disabled={this.state.busy}
                                onClick={this.onClickUnlink}>
                            Unlink
                        </Button>
                    </div>
                }
            </div>
        );
    }

    render() {
        let canEdit = this.props.authz && this.props.authz.userEdit;
        let binding = this.getBinding();
        let msg = this.state.message;
        return (
            <div className="asset-controls detail-list">
                <div className="container-fluid">
                    {binding ? this.renderBound(binding, canEdit) : this.renderUnbound(canEdit)}
                    {msg &&
                        <p style={{marginTop: "0.5em", color: msg.type === "error" ? "#a94442" : "#31708f"}}>
                            {this.state.busy && <i className="fa fa-cog fa-spin" style={{marginRight: "0.4em"}} />}
                            {msg.text}
                        </p>
                    }
                </div>
            </div>
        );
    }
}

ExternalBindingPanel.propTypes = {
    modelId: PropTypes.string,
    asset: PropTypes.object,
    authz: PropTypes.object,
    capability: PropTypes.object,   // {providerName, providerLabel}
    dispatch: PropTypes.func
};

export default ExternalBindingPanel;
