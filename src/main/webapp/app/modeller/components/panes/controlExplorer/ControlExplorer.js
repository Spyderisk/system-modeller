import React from "react";
import PropTypes from 'prop-types';
import ControlAccordion from "./ControlAccordion";
import {connect} from "react-redux";
import Explorer from "../common/Explorer"

class ControlExplorer extends React.Component {

    constructor(props) {
        super(props);

        this.renderContent = this.renderContent.bind(this);
    }

    render() {
        if (!this.props.show) {
            return null;
        }

        let rndParams = {
            xScale: 0.15,
            width: 360,
            height: 600
        }
        
        return (
            <Explorer
                title={"Control Explorer"}
                windowName={"controlExplorer"}
                documentationLink={"redirect/control-explorer"}
                rndParams={rndParams}
                selectedAsset={this.props.selectedAsset}
                //isActive={this.props.isActive}
                show={this.props.show}
                onHide={this.props.onHide}
                loading={this.props.loading}
                dispatch={this.props.dispatch}
                renderContent={this.renderContent}
                windowOrder={this.props.windowOrder}
            />
        )
    }

    renderContent() {
        const {model, hoverThreat, dispatch, loading, ...modalProps} = this.props;

        let controlSetsMap = {};
        this.props.model.controlSets.forEach(cs => controlSetsMap[cs.uri] = cs);

        var controlUri = this.props.selectedAsset.selectedControl;
        var controlSet = Array.from(this.props.model.controlSets).find(c => c["control"] == controlUri);
        var label = (controlSet!=null ? controlSet.label : "");
        var description = (controlSet!=null ? controlSet.description : "");

        return (
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

                <ControlAccordion
                    model={model}
                    controlSets={controlSetsMap}
                    hoverThreat={hoverThreat}
                    loading={loading}
                    selectedAsset={this.props.selectedAsset}
                    getAssetType={this.props.getAssetType}
                    authz={this.props.authz}
                    dispatch={dispatch}
                />
            </div>
        )
    }
}

ControlExplorer.propTypes = {
    selectedAsset: PropTypes.object,
    //isActive: PropTypes.bool, // is in front of other panels
    model: PropTypes.object,
    show: PropTypes.bool,
    onHide: PropTypes.func,
    hoverThreat: PropTypes.func,
    getAssetType: PropTypes.func,
    loading: PropTypes.object,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
    windowOrder: PropTypes.number,
};

let mapStateToProps = function (state) {
    return {
        windowOrder: state.view["controlExplorer"]
    }
};

export default connect(mapStateToProps)(ControlExplorer);
