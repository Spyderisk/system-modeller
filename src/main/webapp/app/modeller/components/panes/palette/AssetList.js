import React from "react";
import PropTypes from "prop-types";
import PaletteAsset from "./PaletteAsset";

/**
 * This component is used to divide the palette up into sections if required.
 */
class AssetList extends React.Component {

    /**
     * This renders our React components to the Virtual DOM. In this case we have an
     * @returns {XML} It is returning the XML structure used to render the HTML.
     */
    render() {
        var self = this;
        return (
            <div className="asset-list">
                {this.props.paletteAssets.map((asset) => {
                        if (this.props.isAssetDisplayed(asset)) {
                            return <PaletteAsset
                                key={asset.id}
                                asset={asset}
                                handleTileCreation={self.props.handleTileCreation}
                                assetCount={self.props.getAssetCount(asset.id)}/>
                        }
                    }
                )}
            </div>
        );
    }

}

/**
 * This is specifying the prop types. This reduces the manual error checking required.
 * @type {{palette: *}} #Types specified in JSON format.
 */
AssetList.propTypes = {
    paletteAssets: PropTypes.array,
    isAssetDisplayed: PropTypes.func,
    getAssetCount: PropTypes.func
};

/* Export the asset list as required. */
export default AssetList;
