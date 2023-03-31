import React from "react";
import PropTypes from 'prop-types';
import {OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../common/constants.js";

/**
 * This component handles the signal to delete an asserted asset.
 */
class DeleteAssertedAssetGlyph extends React.Component {

    /**
     * This renders our React components to the Virtual DOM. In this case we have a button rendered to a tile.
     * @returns {XML} It is returning the XML structure used to render the HTML.
     */
    render() {
        return (
            // We want to include tooltips in as many appropriate places as is reasonable to improve usability.
                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
                    overlay={<Tooltip id="delete-asset-tooltip"><strong>Delete this tile</strong></Tooltip>}>
                    <span id={"tile-" + this.props.assetId + "-delete"} className="glyph delete-asserted-asset fa fa-trash"
                        onClick={this.props.onDelClick}></span>
                </OverlayTrigger>
        );
    }

}

/**
 * This is specifying the prop types. This reduces the manual error checking required.
 * @type {{onConnClick: *}} Types specified in JSON format.
 */
DeleteAssertedAssetGlyph.propTypes = {onDelClick: PropTypes.func, assetId: PropTypes.string, authz: PropTypes.object};

/* Export the button as required. */
export default DeleteAssertedAssetGlyph;
