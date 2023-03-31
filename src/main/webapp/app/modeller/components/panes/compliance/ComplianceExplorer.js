import React from "react";
import PropTypes from 'prop-types';

import ComplianceAccordion from "./ComplianceAccordion";
import {Rnd} from "react-rnd";
import {bringToFrontWindow, closeWindow} from "../../../actions/ViewActions";
import {connect} from "react-redux";
import {openDocumentation} from "../../../../common/documentation/documentation";

class ComplianceExplorer extends React.Component {

    constructor(props) {
        super(props);

        this.rnd = null;
    }

    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;

        if ((!this.props.show) && (!nextProps.show)) {
            //console.log("ComplianceExplorer.shouldComponentUpdate: false: (not visible)");
            return false;
        }

        if (nextProps.loading.model) {
            //console.log("ComplianceExplorer.shouldComponentUpdate: false: (model loading)");
            return false;
        }

        if (this.props.isActive !== nextProps.isActive) {
            //console.log("ComplianceExplorer.shouldComponentUpdate: true: (isActive changed)");
            return true;
        }

        //console.log("ComplianceExplorer.shouldComponentUpdate: " + shouldComponentUpdate);
        return shouldComponentUpdate;
    }

    /*
    componentWillReceiveProps(nextProps) {
        //console.log("ComplianceExplorer: componentWillReceiveProps:", nextProps);
        this.setState({
            ...this.state,
            impact: nextProps.selectedMisbehaviour.misbehaviour.impactLevel,
            updating: false
        });
    }
    */

    render() {
        //console.log("ComplianceExplorer: render this.props:");
        //console.log(this.props);
        const {model, hoverThreat, dispatch, loading, ...modalProps} = this.props;

        if(!this.props.show){
            return null;
        }

        return (
            <Rnd ref={ c => {this.rnd = c;} }
                 bounds={ '#view-boundary' }
                 default={{
                     x: window.outerWidth * 0.1,
                     y: (100 / window.innerHeight) * window.devicePixelRatio,
                     width: 560,
                     height: 600,
                 }}
                 style={{ zIndex: this.props.windowOrder }}
                 minWidth={150}
                 maxWidth={1000}
                 minHeight={200}
                 maxHeight={800}
                 onResize={(e) => {
                     if (e.stopPropagation) e.stopPropagation();
                     if (e.preventDefault) e.preventDefault();
                     e.cancelBubble = true;
                     e.returnValue = false;
                 }}
                 disableDragging={this.props.threatFiltersActive["compliance-threats"] !== false} //disable window dragging if any threat filter has focus
                 cancel={".content, .text-primary, strong, span"}
                 // onDragStart={(e) => {
                 //     let elName = $(e.target)[0].localName;
                     //console.log("onDragStart: element: " + elName);

                     /*
                     if (elName === "input") {
                         return false;
                     }
                     */


                 // }}
                 onDrag={(e) => {
                     if (e.stopPropagation) e.stopPropagation();
                     if (e.preventDefault) e.preventDefault();
                     e.cancelBubble = true;
                     e.returnValue = false;
                 }}
                 onDragStop={(e) => {
                     /*
                     let el = $(e.target)[0];
                     let elName = el.localName;
                     let elID = el.id
                     console.log("onDragStop: element: " + elName + ": " + elID);
                     //console.log(el);
                     if (elName === "input") {
                         //console.log("onDragStop: setting focus for input textbox");
                         //el.focus();
                         console.log("onDragStop: calling activateThreatFilter");
                         this.props.dispatch(activateThreatFilter(elID, true));
                     }
                     */
                 }}
                 className={!this.props.show ? "hidden" : null}>
                <div className="compliance-explorer">

                    <div className="header" onMouseDown={() => {
                        this.props.dispatch(bringToFrontWindow("complianceExplorer"))
                    }}>
                        <h1>
                            <div className={"doc-help-explorer"}>
                                <div>
                                    {"Compliance Explorer"}
                                </div>
                            </div>
                        </h1>
                        <span classID="rel-delete"
                              className="menu-close fa fa-times"
                              onClick={() => {
                                  this.props.dispatch(closeWindow("complianceExplorer"));
                                  this.props.onHide();
                              }}>
                        </span>
                        <span className="menu-close fa fa-question" onClick={e => openDocumentation(e, "redirect/compliance-explorer")} />
                    </div>

                    <div className="content">
                        <div className="desc">
                        </div>

                        <ComplianceAccordion dispatch={dispatch}
                                               model={model}
                                               complianceSetsData={this.props.complianceSetsData}
                                               selectedThreat={this.props.selectedThreat}
                                               threatFiltersActive={this.props.threatFiltersActive}
                                               hoverThreat={hoverThreat}
                                               loading={loading}
                                               authz={this.props.authz}
                        />
                    </div>
                </div>

            </Rnd>
        );
    }
}

ComplianceExplorer.propTypes = {
    isActive: PropTypes.bool, // is in front of other panels
    threatFiltersActive: PropTypes.object,
    model: PropTypes.object,
    complianceSetsData: PropTypes.object,
    selectedThreat: PropTypes.object,
    onHide: PropTypes.func,
    loading: PropTypes.object,
    hoverThreat: PropTypes.func,
    authz: PropTypes.object,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["complianceExplorer"]
    }
};

export default connect(mapStateToProps)(ComplianceExplorer);
