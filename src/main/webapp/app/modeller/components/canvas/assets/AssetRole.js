import React from "react";
import PropTypes from 'prop-types';
import ReactDOM from "react-dom";
import {
    putAssertedAssetRename,
    putAssertedAssetRelocate,
    deleteAssertedAsset,
    postAssertedRelation,
    changeSelectedAsset
} from "../../../actions/ModellerActions";
import RelationSelectionMenu from "../popups/RelationSelectionMenu";
import AddConnectionGlyph from "./glyphs/AddConnectionGlyph";
import CompleteConnectionGlyph from "./glyphs/CompleteConnectionGlyph";
import DeleteTileGlyph from "./glyphs/DeleteAssertedAssetGlyph";

/**
 * This component represents an individual asserted asset on the canvas.
 */
class AssetRole extends React.Component {

    /**
     * This constructor is used to bind methods to this class.
     * @param props Props passed from canvas.
     */
    constructor(props) {
        super(props);

        /*
        this.startConnection = this.startConnection.bind(this);
        this.cancelConnection = this.cancelConnection.bind(this);
        this.completeConnection = this.completeConnection.bind(this);
        this.updateMultiChoiceRelation = this.updateMultiChoiceRelation.bind(this);
        this.closeRelMenu = this.closeRelMenu.bind(this);

        this.showGlyphs = this.showGlyphs.bind(this);
        this.hideGlyphs = this.hideGlyphs.bind(this);
        this.handleClick = this.handleClick.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
        */
    }

    /**
     * React Lifecycle Methods
     */

    /**
     * This lifecycle method is called once the asserted asset has been rendered.
     * Here we are ensuring that the asset renders in the correct position (this must be done post render so that we
     * can know the exact dimensions of the DIV on the document.
     */
    /*
    componentDidMount() {
        $(ReactDOM.findDOMNode(this)).attr("id", "tile-" + this.props.asset["id"]);
        TileFactory.getPlumbingInstance().draggable($(ReactDOM.findDOMNode(this)),
            {
                containment: "tile-canvas",
                // Want to use the center of the tile, not where the mouse is on the tile.
                stop: event => {
                    var newX = event.finalPos[0] + $(event.el).width() / 2,
                        newY = event.finalPos[1] + $(event.el).height() / 2,
                        jTile = $(ReactDOM.findDOMNode(this));
                    this.props.dispatch(putAssertedAssetRelocate(this.props.modelId, this.props.asset["id"], {
                        ...this.props.asset, iconX: newX - jTile.width() / 2, iconY: newY - jTile.height() / 2
                    }));
                }
            });
    }
    */

    /*
    componentDidUpdate () {
        //console.log("AssertedAsset.componentDidUpdate");
        if (this.props.isSelectedAsset(this.props.asset["id"])) {
            var jTile = $(ReactDOM.findDOMNode(this));
            //console.log("AssertedAsset.componentDidUpdate: selecting tile");
            jTile.addClass("active-tile");
        }
    }
    */

    /**
     * This method renders the asserted asset, with all of its sub components, to the tile canvas.
     * @returns {XML}
     */
    render() {

        //Check assetType. If this is null, display default icon (this can happen if we are using the wrong palette, for example)
        var defaultIcon = "fallback.svg";
        var icon = this.props.assetType ? this.props.assetType["icon"] : defaultIcon;
        const icon_path = process.env.config.API_END_POINT + "/images/" + icon;

        var styles = {
            //left: this.props.asset["iconX"] + 25,
            //top: this.props.asset["iconY"] - 15
            //left: 10,
            top: -15
        };
        var divStyles = {
            //backgroundImage: 'url(' + icon + ')',
            //backgroundSize: 'contain',
            //backgroundRepeat: 'no-repeat',
            //backgroundPosition: 'center center'
        };

        return this.props.asset["asserted"] ?
            <div className="tile role-tile"
                 classID={"role-tile-" + this.props.asset["id"]}
                 style={styles}>
               <label className="text-primary role">blah</label>
            </div>
            :
            <div></div>;
    }

    /**
     * Connections
     */

    /**
     * This method starts the creation of a new relation from this asserted asset.
     */
    /*
    startConnection() {
        var tile = ReactDOM.findDOMNode(this);
        if ($(tile).find("span[classid='tile-" + this.props.asset["id"] + "-add']").hasClass("add-connection")) {
            TileFactory.startConnection(this.props.asset["id"], this.props.linkToTypes(this.props.assetType["name"]));
            var addGlyph = $(tile).find("span[classid='tile-" + this.props.asset["id"] + "-add']");
            $(".tile").removeClass("connecting-tile");
            $(tile).addClass("connecting-tile");
            addGlyph.removeClass("fa-plus-circle add-connection");
            addGlyph.addClass("fa-stop-circle cancel-connection");
            addGlyph.css({});
        } else {
            this.cancelConnection();
        }
    }
    */

    /**
     * This method cancels the creation of a new relation from this asserted asset.
     */
    /*
    cancelConnection() {
        var jTile = $(ReactDOM.findDOMNode(this));
        TileFactory.stopConnection();
        jTile.removeClass("connecting-tile");
        var cancelGlyph = jTile.find("span[classid='tile-" + this.props.asset["id"] + "-add']");
        cancelGlyph.removeClass("fa-stop-circle cancel-connection");
        cancelGlyph.addClass("fa-plus-circle add-connection");
    }
    */

    /**
     * This method completes the creation of a new relation to this asserted asset.
     */
    /*
    completeConnection() {
        // Reset the initial tile back to its initial state.
        var assetFromId = TileFactory.getStartElementId();
        var startingAsset = $(document).find("div[classid='tile-" + assetFromId + "']");
        var startingAssetAddGlyph = startingAsset.find("span[classid='tile-" + assetFromId + "-add']");
        startingAsset.removeClass("connecting-tile");
        startingAssetAddGlyph.removeClass("fa-stop-circle cancel-connection");
        startingAssetAddGlyph.addClass("fa-plus-circle add-connection");
        startingAssetAddGlyph.hide();
        $(".tile").each((element) => {
            // Hide complete-connection glyph.
            $(element).find("span[classid='" + $(element).attr("classid") + "-complete']").hide();
        });

        // Get connection types possible
        var currentEndpoints = TileFactory.getCurrentEndpoints();
        var relTypes = [];
        for (var type in currentEndpoints) {
            if (currentEndpoints.hasOwnProperty(type)) {
                if (currentEndpoints[type].indexOf(this.props.asset["id"]) >= 0) {
                    relTypes.push(type);
                }
            }
        }

        // Dispatch creation or offer selection of type
		if (relTypes.length < 1) {
			console.warn("Error: relation type not found");
		} else if (relTypes.length === 1) {
            // Dispatch the new relation to the backend to have it checked.
            this.props.dispatch(postAssertedRelation(
                this.props.modelId,
                assetFromId,
                this.props.asset["id"],
                relTypes[0]));
            TileFactory.stopConnection();
        } else {
            var menu = document.createElement("div");
            document.body.appendChild(menu);
            $(menu).attr("id", "conn-selection-menu");

            var options = [];
            relTypes.map((relType) => {
                var relName = relType;

                if (relType && relType.indexOf('#') > -1) {
                    relName = relType.split('#')[1];
                }

                var option = {type: relType, name: relName};
                options.push(option);
            });

            ReactDOM.render(<RelationSelectionMenu
                relationOptions={options}
                selectOption={this.updateMultiChoiceRelation}
                closeMenu={this.closeRelMenu}/>, document.getElementById("conn-selection-menu"));
            var jPopupMenu = $(".popup-menu");
            jPopupMenu.css({
                left: event.pageX - jPopupMenu[0].clientWidth / 2,
                top: event.pageY - jPopupMenu[0].clientHeight / 2
            });
            //jPopupMenu.on("mouseleave", () => this.closeRelMenu());
        }
    }
    */

    /**
     * This method controls which glyphs to show on mouse over.
     */
    /*
    showGlyphs() {
        var tile = ReactDOM.findDOMNode(this);
        if (TileFactory.isConnStarted()) {
            if ("tile-" + TileFactory.getStartElementId() !== $(tile).attr("classid")) {
                if (TileFactory.getCurrentEndpointIds().indexOf(this.props.asset["id"]) >= 0) {
                    $(tile).find("span[classid='tile-" + this.props.asset["id"] + "-complete']").show();
                }
            }
        } else {
            $(tile).find("span[classid='tile-" + this.props.asset["id"] + "-add']").show();
        }
        $(tile).find("span[classid='tile-" + this.props.asset["id"] + "-delete']").show();
    }
    */

    /**
     * Visuals
     */

    /**
     * This method controls which glyphs to hide on mouse leave.
     */
    /*
    hideGlyphs() {
        var tile = ReactDOM.findDOMNode(this);
        if (!TileFactory.isConnStarted()) {
            if (TileFactory.getStartElementId() !== tile) {
                $(tile).find("span[classid='tile-" + this.props.asset["id"] + "-complete']").hide();
            }
            $(tile).find("span[classid='tile-" + this.props.asset["id"] + "-add']").hide();
        }
        $(tile).find("span[classid='tile-" + this.props.asset["id"] + "-delete']").hide();
    }
    */

    /**
     * Called when an asserted asset is clicked on. It updates the classes used on the tile.
     */
    /*
    handleClick() {
        var jTile = $(ReactDOM.findDOMNode(this));
        $("#tile-canvas").find(".tile").removeClass("active-tile");
        TileFactory.getPlumbingInstance().select().each((rel) => rel.setType("basic"));

        jTile.addClass("active-tile");
        if (!this.props.isSelectedAsset(this.props.asset["id"])) {
            console.log("Clicked asserted asset " + this.props.asset["id"]);
            this.props.dispatch(changeSelectedAsset(this.props.asset["id"]));
        }
    }
    */

    /**
     * This method is called when the asserted asset name is double clicked.
     * @param event The "double click" event that is fired.
     */
    /*
    handleEdit(event) {
        var self = this;
        var label = $(event.target);
        label.after("<input id='asset-rename' type='text' class='text-primary'/>");
        var textBox = label.next();
        label.hide();
        textBox.show();
        textBox.val(label.html());
        textBox.on("blur", () => {
            if (self.props.isAssetNameTaken(this.props.asset["name"], textBox.val())) {
                this.props.dispatch(putAssertedAssetRename(
                    this.props.modelId,
                    this.props.asset["id"],
                    {
                        ...this.props.asset,
                        label: textBox.val()
                    }
                ));
                label.html(textBox.val());
            } else {
                alert("That name has already been taken!");
            }
            label.show();
            textBox.remove();
        });
        textBox.on("keyup",
            (e) => {
                if (e.keyCode === 13) {
                    textBox.blur();
                }
            });
        $(document).on("click",
            (e) => {
                if (e.target.id !== textBox.attr("id") && textBox.is(":focus")) {
                    textBox.blur();
                }
            });
    }
    */

    /*
    updateMultiChoiceRelation(option) {
        this.props.dispatch(postAssertedRelation(
            this.props.modelId,
            TileFactory.getStartElementId(),
            this.props.asset["id"],
            option));
        this.closeRelMenu();
    }

    closeRelMenu() {
        var domNode = document.getElementById("conn-selection-menu");
        ReactDOM.unmountComponentAtNode(domNode);
        $(domNode).remove();
        TileFactory.stopConnection();
    }
    */
}

AssetRole.propTypes = {
    asset: PropTypes.object,
    //assetType: PropTypes.object,
    //isSelectedAsset: PropTypes.func,
    //isAssetNameTaken: PropTypes.func,
    //linkToTypes: PropTypes.array,
    //modelId: PropTypes.string,
    dispatch: PropTypes.func
};

/* This exports the AssetRole class as required. */
export default AssetRole;
