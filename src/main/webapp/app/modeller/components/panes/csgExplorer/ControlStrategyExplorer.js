import React from "react";
import PropTypes from 'prop-types';
//import ControlStrategyAccordion from "./ControlStrategyAccordion";
import {Rnd} from "../../../../../node_modules/react-rnd/lib/index";
import {bringToFrontWindow, closeWindow} from "../../../actions/ViewActions";
import renderControlStrategy from "./ControlStrategyRenderer";
import {
    updateControlOnAsset,
    updateThreat
} from "../../../../modeller/actions/ModellerActions";
import {connect} from "react-redux";
import {openDocumentation} from "../../../../common/documentation/documentation"

class ControlStrategyExplorer extends React.Component {

    constructor(props) {
        super(props);

        this.updateThreat = this.updateThreat.bind(this);
        this.getCsgControlSets = this.getCsgControlSets.bind(this);

        this.rnd = null;

        this.state = {
            updatingControlSets: {},
            controlSets: this.getCsgControlSets(props)
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;

        if ((!this.props.show) && (!nextProps.show)) {
            return false;
        }

        if (nextProps.loading.model) {
            return false;
        }

        if (this.props.isActive != nextProps.isActive) {
            return true;
        }

        if(this.props.selectedAsset != nextProps.selectedAsset) {
            return true;
        }

        return shouldComponentUpdate;
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
        const {model, hoverThreat, dispatch, loading, ...modalProps} = this.props;

        //TODO: for now we take the overall CSG label/desc from the first element in the CSG array,
        //however the desc will be specific for a particular CSG (threat at asset)
        var csg = this.props.selectedControlStrategy.length > 0 ? this.props.selectedControlStrategy[0] : null;
        var label = (csg != null ? csg.label : "");
        let context = this.props.csgExplorerContext;

        if (!this.props.show) {
            return null;
        }

        return (
          <Rnd ref={ c => {this.rnd = c;} }
               bounds={ '#view-boundary' }
               default={{
                   x: window.outerWidth * 0.20,
                   y: (100 / window.innerHeight) * window.devicePixelRatio,
                   width: 500,
                   height: 600,
               }}
               style={{ zIndex: this.props.windowOrder }}
               minWidth={150}
               minHeight={200}
               cancel={".content, .text-primary, strong, span"}
               onResize={(e) => {
                   if (e.stopPropagation) e.stopPropagation();
                   if (e.preventDefault) e.preventDefault();
                   e.cancelBubble = true;
                   e.returnValue = false;
               }}
               onDrag={(e) => {
                   if (e.stopPropagation) e.stopPropagation();
                   if (e.preventDefault) e.preventDefault();
                   e.cancelBubble = true;
                   e.returnValue = false;
               }}
               className={!this.props.show ? "hidden" : null}>
               <div className="control-strategy-explorer">

                   <div className="header" onMouseDown={() => {
                       this.props.dispatch(bringToFrontWindow("controlStrategyExplorer"));
                   }}>
                       <h1>
                           <div className={"doc-help-explorer"}>
                               <div>
                                   {"Control Strategy Explorer"}
                               </div>
                           </div>
                       </h1>
                       <span classID="rel-delete"
                             className="menu-close fa fa-times"
                             onClick={() => {
                                 this.props.dispatch(closeWindow("controlStrategyExplorer"));
                                 this.props.onHide();
                             }}>
                       </span>
                       <span className="menu-close fa fa-question" onClick={e => openDocumentation(e, "redirect/control-strategy-explorer")} />
                   </div>

                    <div className="content">
                        <div className="desc">
                            <div className="descriptor">
                                {this.renderDescription(csg, context)}
                            </div>
                        </div>

                        {/* TODO: remove this if we are sure we don't need expandable panels */}
                        {/* <ControlStrategyAccordion dispatch={dispatch}
                                              selectedControlStrategy={this.props.selectedControlStrategy}
                                              model={model}
                                              controlSets={propControlSets}
                                              assets={model.assets}
                                              threats={model.threats}
                                              hoverThreat={this.props.hoverThreat}
                                              updateThreat={this.updateThreat}
                                              authz={this.props.authz}
                            /> */}

                        {this.renderCsgs()}
                   </div>
               </div>

          </Rnd>
        );
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
}

ControlStrategyExplorer.propTypes = {
    selectedAsset: PropTypes.object,
    selectedControlStrategy: PropTypes.object,
    isActive: PropTypes.bool, // is in front of other panels
    model: PropTypes.object,
    controlSets: PropTypes.object,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    hoverThreat: PropTypes.func,
    getAssetType: PropTypes.func,
    authz: PropTypes.object,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["controlStrategyExplorer"]
    }
};

export default connect(mapStateToProps)(ControlStrategyExplorer);
