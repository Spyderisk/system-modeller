import React from "react";
import PropTypes from 'prop-types';
import {OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";

/**
 * This component handles the signal to start a new relation.
 */
class CollapseGroupGlyph extends React.Component {

    /**
     * This renders our React components to the Virtual DOM. In this case we have a button rendered to a tile.
     * @returns {XML} It is returning the XML structure used to render the HTML.
     */
    render() {
        let cssClasses = "glyph collapse-group fa ";
        let tooltipText = "";
        if(this.props.isCollapsed) {
            cssClasses = cssClasses + "fa-expand";
            tooltipText = "Expand this group";
        } else {
            cssClasses = cssClasses + "fa-compress";
            tooltipText = "Collapse this group";
        }
        return (
            // We want to include tooltips in as many appropriate places as reasonable to improve usability.
            <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                            overlay={<Tooltip id="collapse-group-tooltip"><strong>{tooltipText}</strong></Tooltip>}>
                <span id={"group-" + this.props.groupId + "-collapse"} className={cssClasses}
                      onClick={this.props.onClick}></span>
            </OverlayTrigger>
        );
    }
}

/**
 * This is specifying the prop types. This reduces the manual error checking required.
 * @type {{onConnClick: *}} Types specified in JSON format.
 */
CollapseGroupGlyph.propTypes = {
    isCollapsed: PropTypes.bool,
    onClick: PropTypes.func, assetId:
    PropTypes.string
};

/* Export the button as required. */
export default CollapseGroupGlyph;
