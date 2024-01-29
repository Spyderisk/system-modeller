import React from "react";
import PropTypes from 'prop-types';
import renderControlStrategy from "./ControlStrategyRenderer";
import {
    updateControlOnAsset,
    updateThreat
} from "../../../../modeller/actions/ModellerActions";
import {connect} from "react-redux";
import Explorer from "../common/Explorer"

class ControlStrategyExplorer extends React.Component {

    constructor(props) {
        super(props);

        this.renderContent = this.renderContent.bind(this);
        this.updateThreat = this.updateThreat.bind(this);
        this.getCsgControlSets = this.getCsgControlSets.bind(this);

        this.state = {
            updatingControlSets: {},
            controlSets: this.getCsgControlSets(props)
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            updatingControlSets: {},
            controlSets: this.getCsgControlSets(nextProps)
        });
    }

    updateThreat(arg) {
        var updatedThreat = this.props.threat;

        //this is to enable an entire control strategy. I think 're not currently doing this
        if (arg.hasOwnProperty("controlStrategy")) {
            var updatedControlStrategiesA = updatedThreat["controlStrategies"]
                .filter((controlStrategy) => controlStrategy["id"] !== arg.controlStrategy["id"]);
            updatedControlStrategiesA.push({
                ...arg.controlStrategy,
                enabled: !arg.controlStrategy["enabled"]
            });
            updatedThreat = {
                ...updatedThreat,
                controlStrategies: updatedControlStrategiesA
            };
        }

        //this is to enable a single control in a control strategy
        if (arg.hasOwnProperty("control")) {
            //updating control strategy that this control belongs to
            if (!arg.control["controlStrategy"]) {
                console.warn("WARNING: cannot update control strategy: arg.control.controlStrategy is null");
                return;
            }

            //Here we still want to keep the currently selected asset, not change to the asset referred to in the updatedControl
            this.props.dispatch(updateControlOnAsset(this.props.model.id, arg.control.assetId, arg.control));

            return;
        }

        if (arg.hasOwnProperty("threatLevel")) {
            updatedThreat = {
                ...updatedThreat,
                threatLevel: {
                    priorLikelihood: arg.threatLevel["priorLikelihood"],
                    potentialImpact: arg.threatLevel["potentialImpact"]
                }
            };
        }

        this.props.dispatch(updateThreat(this.props.model.id, this.props.asset["id"], updatedThreat["id"], updatedThreat));
    }

    getCsgControlSets(props) {
        let controlSets = {};

        props.selectedControlStrategy.forEach(csg => {
            csg.mandatoryControlSets.forEach(csUri => {
                controlSets[csUri] = props.controlSets[csUri];
            });
            csg.optionalControlSets.forEach(csUri => {
                controlSets[csUri] = props.controlSets[csUri];
            });
        });

        return controlSets;
    }

    renderDescription(csg, context) {
        if (!context) 
            return null;

        let selection = context.selection;

        if (selection === "csg") {
            let asset = context.asset;
            return (<h4>{csg.label} at "{asset.label}"</h4>);
        }
        else if (selection === "csgType") {
            return (<h4>{csg.label} (all occurrences)</h4>);
        }
        else if (selection === "controlSet") {
            let cs = context.controlSet;
            let asset = this.getAssetByUri(cs.assetUri);
            return (<h4>Control Strategies including: {cs.label} at "{asset.label}"</h4>);
        }
        else {
            return null;
        }
    }

    getAssetByUri(assetUri) {
        let asset = this.props.model.assets.find((asset) => {
            return (asset.uri === assetUri);
        });
        return asset;
    }

    render() {
        if (!this.props.show) {
            return null;
        }

        let rndParams = {
            xScale: 0.20,
            width: 500,
            height: 600
        }
        
        return (
            <Explorer
                title={"Control Strategy Explorer"}
                windowName={"controlStrategyExplorer"}
                documentationLink={"redirect/control-strategy-explorer"}
                rndParams={rndParams}
                selectedAsset={this.props.selectedAsset}
                isActive={this.props.isActive}
                show={this.props.show}
                onHide={this.props.onHide}
                loading={this.props.loading}
                dispatch={this.props.dispatch}
                renderContent={this.renderContent}
                windowOrder={this.props.windowOrder}
            />
        )
    }

    renderCsgs() {
        let csgArray = this.props.selectedControlStrategy;

        return (
            <div className="container-fluid csg-list">
            {csgArray.map((csg, csgIndex) => {
                return this.renderCsg(csg, csgIndex);
            })}
        </div>
        );
    }

    renderCsg(csg, csgKey) {
        let csgName = csg.label;
        let threat = null; //list all threats
        return renderControlStrategy(csgName, csg, csgKey, threat, this.state, this.props, this, "csg-explorer");
    }

    renderContent() {
        const {model, hoverThreat, dispatch, loading, ...modalProps} = this.props;

        //TODO: for now we take the overall CSG label/desc from the first element in the CSG array,
        //however the desc will be specific for a particular CSG (threat at asset)
        var csg = this.props.selectedControlStrategy.length > 0 ? this.props.selectedControlStrategy[0] : null;
        var label = (csg != null ? csg.label : "");
        let context = this.props.csgExplorerContext;

        return (
            <div className="content">
                <div className="desc">
                    <div className="descriptor">
                        {this.renderDescription(csg, context)}
                    </div>
                </div>

                {this.renderCsgs()}
            </div>
        )
    }
}

ControlStrategyExplorer.propTypes = {
    selectedAsset: PropTypes.object,
    selectedControlStrategy: PropTypes.array,
    isActive: PropTypes.bool, // is in front of other panels
    model: PropTypes.object,
    controlSets: PropTypes.object,
    show: PropTypes.bool,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    hoverThreat: PropTypes.func,
    getAssetType: PropTypes.func,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
    windowOrder: PropTypes.number,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["controlStrategyExplorer"]
    }
};

export default connect(mapStateToProps)(ControlStrategyExplorer);
