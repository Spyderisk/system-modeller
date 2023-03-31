import React, {Fragment} from "react";
import PropTypes from "prop-types";
import SlidingPanel from "../../../../common/components/slidingpanel/SlidingPanel";
import {Panel} from "react-bootstrap";
import AssetList from "./AssetList";
import {Portal} from "react-portal";
import {openDocumentation} from "../../../../common/documentation/documentation"

/**
 * This component handles the entire asset panel container.
 */
class AssetPane extends React.Component {

    /**
     * This renders our asset panel with any asset lists wanted.
     * @returns {XML} Returns the HTML that will be rendered to the Virtual DOM.
     */
    render() {
        let paletteAssets = this.props.paletteAssets;

        // sort the assets alphabetically by label
        paletteAssets.sort(function (a, b) {
                                return (a.label < b.label) ? -1 : (a.label > b.label) ? 1 : 0;
                           });

        //console.log("paletteAssets:", paletteAssets);

        let categories = [];

        for (let i = 0; i < paletteAssets.length; i++) {
            if (categories.indexOf(paletteAssets[i]["category"]) < 0 && paletteAssets[i]["assertable"] && this.props.isAssetDisplayed(paletteAssets[i])) {
                categories.push(paletteAssets[i]["category"]);
            }
        }

        // sort the categories alphabetically
        categories.sort();

        //console.log("categories:");
        //console.log(categories);

        // TODO: the panel titles should be plain English but the domain model currently uses CamelCase. Here we have implemented temporary string conversion but it really should be changed in the domain model.
        let tabs = [];
        for (let x = 0; x < categories.length; x++) {
            tabs.push(
              <Panel key={x} bsStyle="primary" defaultExpanded>
                  <Panel.Heading>
                      <Panel.Title toggle>
                          <span><i className="fa fa-folder"/>
                          {categories[x]
                            .replace(/([a-z])([A-Z])/g, '$1 $2')
                            .replace(/^./, function(str){ 
                              return str.toUpperCase(); 
                         })}</span>
                      </Panel.Title>
                  </Panel.Heading>
                  <Panel.Collapse>
                      <Panel.Body>
                          <AssetList
                              paletteAssets={this.props.paletteAssets.filter((asset) => (asset["category"] === categories[x]) && asset["assertable"])}
                              handleTileCreation={this.props.handleTileCreation}
                              isAssetDisplayed={this.props.isAssetDisplayed}
                              getAssetCount={this.props.getAssetCount}/>
                      </Panel.Body>
                  </Panel.Collapse>
              </Panel>);
        }


        return (
            <Portal isOpened={true}>
                <SlidingPanel isLeft={true} isResizable={true} width={262} title={
                    <Fragment>
                        <h1 class="title">
                            {"Asset Palette"}
                        </h1>
                        <span class="button">
                            <button onClick={e => openDocumentation(e, "redirect/asset-palette")} className={"doc-help-button"}><i className="fa fa-question"/></button>
                        </span>
                    </Fragment>}>
                    <div className="panel-group assets-accordion" id="palette_asset_tabs">
                        {tabs}
                    </div>
                </SlidingPanel>
            </Portal>
        );
    }
}

/**
 * Specifies the prop categories.
 * @category {{handleTileCreation: *, palette: *}} In JSON format.
 */
AssetPane.propTypes = {
    handleTileCreation: PropTypes.func,
    isAssetDisplayed: PropTypes.func,
    getAssetCount: PropTypes.func,
    selectedLayers: PropTypes.array,
    paletteAssets: PropTypes.array,
};

/* Exports the asset panel as required. */
export default AssetPane;
