import React from "react";
import PropTypes from "prop-types";
import { ContextMenu, MenuItem, ContextMenuTrigger, SubMenu } from "react-contextmenu";
import { wordWrap } from "../../util/wordWrap"

class AssetCtxMenu extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="asset-ctx-menu">
                <ContextMenuTrigger ref={ this.props.contextTrigger } id="asset-ctx-menu"
                                    disableIfShiftIsPressed={ true }><div></div>
                </ContextMenuTrigger>
                <ContextMenu id="asset-ctx-menu">
                    <MenuItem attributes={{style: {userSelect: "all"}}}
                              preventClose={true}>
                        { this.props.assetLabel }
                    </MenuItem>
                    <MenuItem divider />

                    <SubMenu title="Details">

                        <b style={{userSelect: "all"}}>{ this.props.assetTypeLabel } Asset</b> <br/>
                        <span id="asset-ctx-description" style={{
                            overflowWrap: "break-word", maxWidth:"240px", userSelect: "all"
                        }}>
                            {wordWrap(this.props.assetDescription || " ", 30, "\n").split("\n")
                                .map((i, key) => {
                                  return <div key={key}>{ i }<br/></div>;
                                })}
                        </span>

                    </SubMenu>
                    {this.props.authz.userEdit ?
                        <div>
                            <MenuItem onClick={(event) => { this.props.startConnection(event, true) }}>
                                Add Relation
                            </MenuItem>
                            <MenuItem onClick={this.props.editAssetType}>
                                Edit Type
                            </MenuItem>
                            <MenuItem onClick={this.props.deleteAsset}>
                                Delete
                            </MenuItem>
                        </div>
                        :
                        <div></div>
                    }
                </ContextMenu>
            </div>
        );
    }

}

/**
 * This describes the data types of all of the props for validation.
 * @type {{onRelationCreation: *, asset: *, assetType: *, modelId: *, dispatch: *}}
 */
AssetCtxMenu.propTypes = {
    modelId: PropTypes.string,
    assetId: PropTypes.string,
    assetLabel: PropTypes.string,
    assetTypeLabel: PropTypes.string,
    assetDescription: PropTypes.string,
    dispatch: PropTypes.func,
    startConnection: PropTypes.func,
    deleteAsset: PropTypes.func,
    contextTrigger: PropTypes.func,
    editAssetType: PropTypes.func
};

/* This exports the AssertedAsset class as required. */
export default AssetCtxMenu;
