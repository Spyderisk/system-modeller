import React from "react";
import PropTypes from 'prop-types';
import { OverlayTrigger, Tooltip } from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";

class OverviewPane extends React.Component {

    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;

        if (nextProps.loading.model) {
            //console.log("OverviewPane.shouldComponentUpdate: false: (model loading)");
            return false;
        }

        if (nextProps.validating) {
            //console.log("OverviewPane.shouldComponentUpdate: false: (model validating)");
            return false;
        }

        return shouldComponentUpdate;
    }

    /**
     * This renders our overview panel
     * @returns {XML} Returns the HTML that will be rendered to the Virtual DOM.
     */
    render() {
        //get all resolved threats
        if (this.props.threats !== undefined) {
            let highestRisk = this.props.risk ? this.props.risk.label : "N/A";

            return (
                <div ref="overview-panel" classID="overviewPanel" className="overview-panel">
                    <OverlayTrigger
                        delayShow={Constants.TOOLTIP_DELAY}
                        placement="bottom"
                        trigger={["hover"]}
                        rootClose
                        overlay={
                            <Tooltip id="overview-panel-tooltip2">{
                                (!this.props.risk ? "Risks have not been calculated yet" : "Highest risk level")}
                            </Tooltip>
                        }>
                        <div>
                            <p>Highest risk</p>
                            <p>{highestRisk}</p>
                        </div>
                    </OverlayTrigger>
                </div>
            );
        } else {
            console.log("OverviewPane render(): this.props.threats is undefined");
            return (
                <div ref="overview-panel" classID="overviewPanel" className="overview-panel"></div>
            );
        }
    }
}

OverviewPane.propTypes = {
    threats: PropTypes.array,
    //misbehaviourSets: PropTypes.object,
    risk: PropTypes.object,
    loading: PropTypes.object,
    validating: PropTypes.bool
};

/* Export the overview panel as required. */
export default OverviewPane;
