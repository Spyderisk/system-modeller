import React from "react";
import PropTypes from "prop-types";
import {Row, OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../../common/constants.js";
import {
    openControlStrategyExplorer
} from "../../../../../actions/ModellerActions";
import {bringToFrontWindow, closeWindow} from "../../../../../actions/ViewActions";

class ControlStrategiesPanel extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let csgs = this.props.assetCsgs;
        return (
            <div className="asset-control-strategies detail-list">
                <div className="container-fluid">
                    {csgs.length > 0 ? csgs.map((csgEntry, index) => {
                        let name = csgEntry[0];
                        let csg = csgEntry[1];
                        let context = {"selection": "csg", "asset": this.props.asset};

                        let csgOverlayProps = {
                            delayShow: Constants.TOOLTIP_DELAY, placement: "left",
                            overlay: <Tooltip id={"csg-" + 1 + "-error-tooltip"}
                                              className={"tooltip-overlay"}>
                                {csg.description ? csg.description : "" }
                            </Tooltip>
                        };            

                        return (
                            <Row
                            key={index}
                            className="row-hover bare-list"
                            >
                            <OverlayTrigger {...csgOverlayProps}>
                                <div>
                                    <span 
                                        className="clickable"
                                        onClick={() => {
                                            this.props.dispatch(openControlStrategyExplorer([csg], context));
                                            this.props.dispatch(bringToFrontWindow("controlStrategyExplorer"));
                                        }}>
                                        {name}
                                    </span>
                                    {csg.enabled ? 
                                            <i class="fa fa-check" aria-hidden="true"></i>
                                            : null}
                                </div>
                            </OverlayTrigger>
                            </Row>
                        );
                    })
                    : <span>No control strategies found</span>}
                </div>
            </div>
        );
    }
    
}


ControlStrategiesPanel.propTypes = {
    modelId: PropTypes.string,
    asset: PropTypes.object,
    assetCsgs: PropTypes.array,
    dispatch: PropTypes.func,
    authz: PropTypes.object
};

export default ControlStrategiesPanel;
