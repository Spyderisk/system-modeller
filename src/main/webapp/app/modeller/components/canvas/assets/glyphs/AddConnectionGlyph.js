import React from "react";
import PropTypes from 'prop-types';
import {OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";

/**
 * This component handles the signal to start a new relation.
 */
class AddConnectionGlyph extends React.Component {

    /**
     * This renders our React components to the Virtual DOM. In this case we have a button rendered to a tile.
     * @returns {XML} It is returning the XML structure used to render the HTML.
     */
    render() {
        return (
            // We want to include tooltips in as many appropriate places as reasonable to improve usability.
                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                    overlay={<Tooltip id="start-con-tooltip"><strong>Start/Stop a new connection</strong></Tooltip>}>
                    <span id={"tile-" + this.props.assetId + "-add"} className="glyph add-connection fa fa-plus"
                        onClick={this.props.onConnClick}></span>
                </OverlayTrigger>
        );
    }
}

/**
 * This is specifying the prop types. This reduces the manual error checking required.
 * @type {{onConnClick: *}} Types specified in JSON format.
 */
AddConnectionGlyph.propTypes = {onConnClick: PropTypes.func, assetId: PropTypes.string, authz: PropTypes.object};

/* Export the button as required. */
export default AddConnectionGlyph;
