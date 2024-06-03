import React from "react";
import ReactDOM from 'react-dom';
import {act} from 'react-dom/test-utils';
import PropTypes from "prop-types";
import {Rnd} from "react-rnd";
import {Button} from "react-bootstrap";
import RiskTreatmentPlan from "./RiskTreatmentPlan" ;
import Report from "./Report";
import {exportHTML} from "./ReportDownloader";
import {bringToFrontWindow, closeWindow} from "../../../actions/ViewActions";
import {connect} from "react-redux";


class ReportDialog extends React.Component {

    constructor(props) {
        super(props);

        this.rnd = null;
        this.exportHTML = this.exportHTML.bind(this);
    }
    
    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;

        //If it not visible and is still not visible, it should not update
        if ((!this.props.show) && (!nextProps.show)) {
            //console.log("ReportDialog.shouldComponentUpdate: false: (not visible)");
            shouldComponentUpdate = false;
        }
        else if (this.props.model.validating || nextProps.model.validating) {
            //console.log("ReportDialog.shouldComponentUpdate: false: (validating)");
            shouldComponentUpdate = false;
        }
        else if (this.props.model.calculatingRisks || nextProps.model.calculatingRisks) {
            //console.log("ReportDialog.shouldComponentUpdate: false: (calculating risks)");
            shouldComponentUpdate = false;
        }
        else if (nextProps.model.loadingId !== null) {
            //console.log("ReportDialog.shouldComponentUpdate: false: (model loading)");
            shouldComponentUpdate = false;
        }
        else {
            //console.log("ReportDialog.shouldComponentUpdate: " + shouldComponentUpdate);
        }

        return shouldComponentUpdate;
    }


    render() {
        let model = this.props.model;
        console.log("render ReportDialog");
        //console.log(model);
        
        if (model === undefined || model.id === "")
            return null;
        
        let isVisible = this.props.show;
        let isActive = true;
        let clazzStyle = {display: "none"};
        
        //console.log("isVisible:", isVisible);

        return (
            <Rnd ref={ c => { this.rnd = c; } }
                 bounds={ '#view-boundary' }
                 default={{
                     x: window.outerWidth * 0.125,
                     y: (100 / window.innerHeight) * window.devicePixelRatio,
                     width: "70vw",
                     height: "90vh"
                 }}
                 style={{ zIndex: this.props.windowOrder }}
                 minWidth={150}
                 minHeight={200}
                 onResize={(e) => {
                     if (e.stopPropagation) e.stopPropagation();
                     if (e.preventDefault) e.preventDefault();
                     e.cancelBubble = true;
                     e.returnValue = false;
                 }}
                 cancel={".report-content, .text-primary, strong, span, p"}
                 className={!isVisible ? "hidden" : null}>
                <div ref="threat-editor"
                     classID="threatEditor"
                     className="report-dialog"
                     style={!isVisible ? clazzStyle : null}>
                    <div className="header" onMouseDown={() => {
                        this.props.dispatch(bringToFrontWindow("reportDialog"))
                    }}>
                        <h1>
                            {"Report"}
                        </h1>
                        <span classID="rel-delete"
                              className="menu-close fa fa-times"
                              onClick={() => {
                                  this.props.dispatch(closeWindow("reportDialog"));
                                  this.props.onHide();
                              }}>
                        </span>
                    </div>

                    {/* <Button bsClass="btn btn-primary" 
                            onClick={() => {downloadWordDoc2()}}
                        >Download Word Doc2</Button> */}

                    {isVisible ?
                        this.props.reportType === "technicalReport" ?
                            <Report model={ model }
                                dispatch={this.props.dispatch}
                                getAssetType={this.props.getAssetType}
                                exportHTML={this.exportHTML}/> :
                            <RiskTreatmentPlan model={model}
                                controlSets={this.props.controlSets}
                                dispatch={this.props.dispatch}
                                getAssetType={this.props.getAssetType}
                                exportHTML={this.exportHTML}/>
                        : null}
                    </div>

                    <Button bsClass="btn btn-primary" style={{
                        position: "absolute", right: "20px", top: "35px", zIndex: "11"
                    }}
                            onClick={() => {this.exportHTML(model, this.props.getAssetType)}}
                    >Export</Button>

            </Rnd>
        );
    }



    exportHTML(model, assetType) {
        let container = document.createElement('div');
        act(() => {
            ReactDOM.render( this.props.reportType === "technicalReport" ?
                    <Report model={ model }
                                              getAssetType={ assetType }/> :
                    <RiskTreatmentPlan model={ model }
                                            getAssetType={ assetType }/>
                                            , container);
        });
        let report = container.innerHTML;
        exportHTML(report);
        setTimeout(() => {container.remove()}, 150);
    }

}

ReportDialog.propTypes = {
    reportType: PropTypes.string,
    model: PropTypes.object,
    controlSets: PropTypes.object,
    show: PropTypes.bool,
    onHide: PropTypes.func,
    getAssetType: PropTypes.func
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["reportDialog"]
    }
};


export default connect(mapStateToProps)(ReportDialog);
