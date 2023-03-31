import React from "react";
import PropTypes from "prop-types";
import ReactDOM from "react-dom";
import {Image, OverlayTrigger, Popover} from "react-bootstrap";
import * as Constants from "../../../../common/constants.js";
import {
    endPlacingTile,
    getPlumbingInstance,
    isPlacingTile,
    startPlacingTile,
    stopConnection
} from "../../util/TileFactory";

class PaletteAsset extends React.Component {

    constructor(props) {
        super(props);

        this.handleMouseDown = this.handleMouseDown.bind(this);
        this.canPlace = this.canPlace.bind(this);

        this.state = {
            loading: false
        }
    }

    /**
     * React Lifecycle Methods
     */

    componentDidMount() {
        ReactDOM.findDOMNode(this).addEventListener("mousedown", this.handleMouseDown, false);
    }

    componentWillUnmount() {
        ReactDOM.findDOMNode(this).removeEventListener("mousedown", this.handleMouseDown, false);
    }

    render() {
        const title = <strong>{this.props.asset["label"]}</strong>;
        const icon_path = process.env.config.API_END_POINT + "/images/" + this.props.asset["icon"];
        // TODO: the asset titles should be plain English but the domain model currently uses CamelCase. Here we have implemented temporary string conversion but it really should be changed in the domain model.
        return (
            <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
                            overlay={<Popover id="asset-label-popover"
                                              title={title}
                                              className={"tooltip-overlay"}>
                                {/* <span><strong>Type: </strong>{this.props.asset["type"]}</span><br /> */}
                                {/* <span><strong>Sub-Type: </strong>{this.props.asset["subtype"] === "" ? "None" : this.props.asset["subtype"]}</span><br /> */}
                                <span><strong>Description: </strong>{this.props.asset["description"]}</span><br/>
                            </Popover>}>
                <div className={"panel-icon"  + (this.canPlace() ? "" : " invalid")}>
                    <Image responsive src={icon_path}/>
                    <p>
                        {this.props.asset["label"]
                            .replace(/([a-z])([A-Z])/g, '$1 $2')
                            .replace(/^./, function(str){ 
                                return str.toUpperCase(); 
                         })}
                     </p>
                    {this.state.loading && <div className="loading-overlay visible"><span
                        className="fa fa-refresh fa-spin fa-2x fa-fw"/></div>}
                </div>
            </OverlayTrigger>
        );
    }

    /**
     * Visuals
     */

    canPlace(){
        let {asset, assetCount} = this.props;
        if(asset.maxCardinality === -1) {
            return true;
        }
        return assetCount >= asset.minCardinality && assetCount < asset.maxCardinality;
    }

    handleMouseDown(event) {
        //console.log("PaletteAsset: handleMouseDown: ", event);
        const self = this;
        event.preventDefault();
        if(!this.canPlace()){
            return;
        }
        const zoomRatio = 1 / getPlumbingInstance().getZoom();

        const dragIcon = document.createElement("div");
        const icon = this.props.asset.icon;
        const icon_path = process.env.config.API_END_POINT + "/images/" + icon;
        const iconStyles = {
            backgroundImage: "url(" + icon_path + ")",
            backgroundSize: "contain",
            backgroundRepeat: "no-repeat",
            backgroundPosition: "center center",
        };
        $(dragIcon).css(iconStyles);
        $(dragIcon).attr("id", "drag-icon");
        document.body.appendChild(dragIcon);

        $(dragIcon).css({left: event.pageX - $(dragIcon).width() / 2, top: event.pageY - $(dragIcon).height() / 2});

        $(document).on("mousemove", (event) => {
            event.preventDefault();
            if (isPlacingTile()) {
                const img = $("#drag-icon");
                img.css({left: event.pageX - img.width() / 2, top: event.pageY - img.height() / 2});
            }
        });
        $(dragIcon).on("mouseup", (event) => {

            if (isPlacingTile()) {
                event.preventDefault();
                const jDragIcon = $("#drag-icon");
                const jTileCanvas = $("#canvas-container");

                //jh17: Offset using the size of the users window (hardcoded 50 is approx height of top bar which needs to be accounted for)
                let offsetX = (event.pageX - window.outerWidth / 2) * (1-zoomRatio);
                let offsetY = ((event.pageY - 50) - window.innerHeight / 2) * (1-zoomRatio);

                let x = (event.pageX + jTileCanvas.scrollLeft() - offsetX) - jDragIcon.width() / 2; //
                let y = (event.pageY + jTileCanvas.scrollTop() - offsetY) - jDragIcon.height(); //

                if (x < 0) x = 0;
                if (y < 0) y = 0;

                const jSlidingPanel = $(".ssm-sliding-panel-container .left");
                if (event.pageX < jSlidingPanel[1].offsetLeft) {
                    jDragIcon.off("mouseup");
                    $(document).off("mousemove");
                    jDragIcon.remove();
                    endPlacingTile(x, y);


                    this.setState({
                        loading: false
                    });
                    return;
                }

                jDragIcon.off("mouseup");
                $(document).off("mousemove");
                jDragIcon.remove();
                endPlacingTile(x, y);
                stopConnection();
                const selectionDomNode = document.getElementById("conn-selection-menu");
                if (selectionDomNode !== null) {
                    ReactDOM.unmountComponentAtNode(selectionDomNode);
                    $(selectionDomNode).remove();
                }
                
                //console.log("calling handleTileCreation with asset:", self.props.asset);
                self.props.handleTileCreation(self.props.asset, x, y);
                this.setState({
                    loading: false
                })
            }
        });
        startPlacingTile(this);
        this.setState({
            loading: true
        })
    }
}

PaletteAsset.propTypes = {
    asset: PropTypes.object
};

export default PaletteAsset;
