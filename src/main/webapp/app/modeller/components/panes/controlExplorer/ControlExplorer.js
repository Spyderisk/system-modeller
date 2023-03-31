import React from "react";
import PropTypes from 'prop-types';
import ControlAccordion from "./ControlAccordion";
import {Rnd} from "../../../../../node_modules/react-rnd/lib/index";
import {bringToFrontWindow, closeWindow} from "../../../actions/ViewActions";
import {connect} from "react-redux";
import {openDocumentation} from "../../../../common/documentation/documentation"

class ControlExplorer extends React.Component {

    constructor(props) {
        super(props);

        this.rnd = null;
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

    render() {
        const {model, hoverThreat, dispatch, loading, ...modalProps} = this.props;
        var controlUri = this.props.selectedAsset.selectedControl;
        var controlSet = Array.from(this.props.model.controlSets).find(c => c["control"] == controlUri);
        var label = (controlSet!=null ? controlSet.label : "");
        var description = (controlSet!=null ? controlSet.description : "");

        let controlSetsMap = {};
        this.props.model.controlSets.forEach(cs => controlSetsMap[cs.uri] = cs);

        if (!this.props.show) {
            return null;
        }

        return (
          <Rnd ref={ c => {this.rnd = c;} }
               bounds={ '#view-boundary' }
               default={{
                   x: window.outerWidth * 0.15,
                   y: (100 / window.innerHeight) * window.devicePixelRatio,
                   width: 360,
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
               // onDragStart={(e) => {
               //     let elName = $(e.target)[0].localName;
               //     //console.log("onDragStart: element: " + elName);
               //
               //     /*
               //     if (elName === "input") {
               //         return false;
               //     }
               //     */
               //     if (this.props.isActive) {
               //         return;
               //     }
               //
               //     this.bringWindowToFront(true);
               //     this.props.dispatch(activateControlExplorer(this.props.selectedControl));
               // }}
               onDrag={(e) => {
                   if (e.stopPropagation) e.stopPropagation();
                   if (e.preventDefault) e.preventDefault();
                   e.cancelBubble = true;
                   e.returnValue = false;
               }}
               className={!this.props.show ? "hidden" : null}>
               <div className="control-explorer">

                   <div className="header" onMouseDown={() => {
                       this.props.dispatch(bringToFrontWindow("controlExplorer"));
                   }}>
                       <h1>
                           <div className={"doc-help-explorer"}>
                               <div>
                                   {"Control Explorer"}
                               </div>
                           </div>
                       </h1>
                       <span classID="rel-delete"
                             className="menu-close fa fa-times"
                             onClick={() => {
                                 this.props.dispatch(closeWindow("controlExplorer"));
                                 this.props.onHide();
                             }}>
                       </span>
                       <span className="menu-close fa fa-question" onClick={e => openDocumentation(e, "redirect/control-explorer")} />
                   </div>

                    <div className="content">
                        <div className="desc">
                            <div className="descriptor">
                                <h4>
                                    {label}
                                </h4>
                                <p>
                                    {description}
                                </p>
                            </div>
                        </div>

                       <ControlAccordion dispatch={dispatch}
                                              model={model}
                                              controlSets={controlSetsMap}
                                              hoverThreat={hoverThreat}
                                              loading={loading}
                                              selectedAsset={this.props.selectedAsset}
                                              getAssetType={this.props.getAssetType}
                                              authz={this.props.authz}
                       />
                   </div>
               </div>

          </Rnd>
        );
    }
}

ControlExplorer.propTypes = {
    selectedAsset: PropTypes.object,
    isActive: PropTypes.bool, // is in front of other panels
    model: PropTypes.object,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    hoverThreat: PropTypes.func,
    getAssetType: PropTypes.func,
    authz: PropTypes.object,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["controlExplorer"]
    }
};

export default connect(mapStateToProps)(ControlExplorer);
