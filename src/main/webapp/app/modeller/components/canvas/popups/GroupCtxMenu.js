import React from "react";
import PropTypes from "prop-types";
import { ContextMenu, MenuItem, ContextMenuTrigger, SubMenu } from "react-contextmenu";
import { wordWrap } from "../../util/wordWrap"

class GroupCtxMenu extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let group = this.props.group;
        return (
            <div className="group-ctx-menu">
                <ContextMenuTrigger ref={ this.props.contextTrigger } id="group-ctx-menu"
                                    disableIfShiftIsPressed={ true }><div></div>
                </ContextMenuTrigger>
                <ContextMenu id="group-ctx-menu">
                    <MenuItem attributes={{style: {userSelect: "all"}}}
                              preventClose={true}>
                        { group.name }
                    </MenuItem>
                    <MenuItem divider />
                    <MenuItem onClick={ (event) => { this.props.deleteGroup(group, false) } }>
                        Delete (group only)
                    </MenuItem>
                    <MenuItem onClick={ (event) => { this.props.deleteGroup(group, true) } }>
                        Delete (all contents)
                    </MenuItem>
                </ContextMenu>
            </div>
        );
    }

}

/**
 * This describes the data types of all of the props for validation.
 */
GroupCtxMenu.propTypes = {
    modelId: PropTypes.string,
    group: PropTypes.object,
    deleteGroup: PropTypes.func,
    contextTrigger: PropTypes.func
};

export default GroupCtxMenu;
