import React from "react";
import PropTypes from "prop-types";
import { ContextMenu, MenuItem, ContextMenuTrigger, SubMenu } from "react-contextmenu";
import { wordWrap } from "../../util/wordWrap"

class CanvasCtxMenu extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let x = this.props.x;
        let y = this.props.y;
        console.log("CanvasCtxMenu: render menu at pos: ", x, y);
        return (
            <div className="canvas-ctx-menu">
                <ContextMenuTrigger ref={ this.props.contextTrigger } id="canvas-ctx-menu"
                                    disableIfShiftIsPressed={ true }><div></div>
                </ContextMenuTrigger>
                <ContextMenu id="canvas-ctx-menu">
                    <MenuItem onClick={ (event) => { this.props.addGroup(x, y) } }>
                        Add Group
                    </MenuItem>
                </ContextMenu>
            </div>
        );
    }

}

/**
 * This describes the data types of all of the props for validation.
 */
CanvasCtxMenu.propTypes = {
    x: PropTypes.number,
    y: PropTypes.number,
    addGroup: PropTypes.func,
    contextTrigger: PropTypes.func
};

export default CanvasCtxMenu;
