import React from "react";
import PropTypes from "prop-types";
import ReactDOM from "react-dom";
import * as Input from "../../../../common/input/input.js"
import {
    deleteAssertedAsset,
    postAssertedRelation,
    putAssertedAssetRename,
    putAssertedAssetType,
    putRelationRedefine,
    redrawRelations
} from "../../../actions/ModellerActions";
import RelationSelectionMenu from "../popups/RelationSelectionMenu";
import AddConnectionGlyph from "./glyphs/AddConnectionGlyph";
import CompleteConnectionGlyph from "./glyphs/CompleteConnectionGlyph";
import DeleteTileGlyph from "./glyphs/DeleteAssertedAssetGlyph";
import AssetRole from "./AssetRole";
import AssetCtxMenu from "../popups/AssetCtxMenu"
import {
    _addEndpoints,
    _delEndpoints,
    connectorPaintStyle,
    dragDotOver,
    draggingConnection,
    endpointHoverStyle,
    getCurrentEndpointIds,
    getCurrentEndpoints,
    getPlumbingInstance,
    getStartElementId,
    hoveredAsset,
    isConnStarted,
    outerOver,
    setDragDotOver,
    setDraggingConnection,
    setLinkHover,
    setOuterOver,
    startConnection,
    stopConnection
} from "../../util/TileFactory";
import '../../../css/assertedAsset.scss';
import * as Constants from "../../../../common/constants.js"
import EditAssetTypeModal from "../../panes/details/popups/EditAssetTypeModal";

/**
 * This component represents an individual asserted asset on the canvas.
 */
var editAssetLoading = '';


class AssertedAsset extends React.Component {

    /**
     * This constructor is used to bind methods to this class.
     * @param props Props passed from canvas.
     */
    constructor(props) {
        super(props);

        this.startConnection = this.startConnection.bind(this);
        this.cancelConnection = this.cancelConnection.bind(this);
        this.completeConnection = this.completeConnection.bind(this);
        this.changeRelationType = this.changeRelationType.bind(this);
        this.updateMultiChoiceRelation = this.updateMultiChoiceRelation.bind(this);
        this.closeRelMenu = this.closeRelMenu.bind(this);
        this.contextTrigger = this.contextTrigger.bind(this);
        this.modalTrigger = this.modalTrigger.bind(this);
        this.editAssetType = this.editAssetType.bind(this);
        this.submitEditAssetTypeModal = this.submitEditAssetTypeModal.bind(this);
        this.closeEditAssetTypeModal = this.closeEditAssetTypeModal.bind(this);

        this.showGlyphs = this.showGlyphs.bind(this);
        this.hideGlyphs = this.hideGlyphs.bind(this);
        this.bindEventHandlers = this.bindEventHandlers.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
        this.handleEdit = this.handleEdit.bind(this);
        this.handleContextMenu = this.handleContextMenu.bind(this);
        this.handleMouseOver = this.handleMouseOver.bind(this);
        this.handleMouseOut = this.handleMouseOut.bind(this);

        this.state = {
            loading: false,
            editAssetTypeModal: false,
            timeout: false
        };

        this.contextTriggerVar = null;
        this.modalTriggerVar = null;
    }

    /**
     * React Lifecycle Methods
     */

    /**
     * This lifecycle method is called once the asserted asset has been rendered.
     * Here we are ensuring that the asset renders in the correct position (this must be done post render so that we
     * can know the exact dimensions of the DIV on the document.
     */
    componentDidMount() {
        //console.log("AssertedAsset: componentDidMount asset: " + this.props.asset["id"], this.el);

        this.$el = $(this.el);
        if (!this.$el) return;

        if (this.props.authz.userEdit == true) {
            getPlumbingInstance().draggable("tile-" + this.props.asset["id"],
                {
                    //N.B. setting containment to false below allows asset to be dragged outside of a group (if it is in one)
                    //however this may now cause problems for an asset simply on the canvas, which needs to be prevented
                    //from moving outside of the canvas. TODO: investigate if this is really a problem. If so, we somehow
                    //need to determint if an asset is currently part of a group, or just on the canvas and set the flag accordingly.
                    containment: false,
                    grid: [10, 10],
                    // Want to use the center of the tile, not where the mouse is on the tile.
                    filter: ".glyph, .title-change",
                    stop: (params) => {
                        // console.log("draggable stop params: ", params);
                        const assetId = this.props.asset.id;
                        const newX = params.pos[0];
                        const newY = params.pos[1];
                        const clickedAsset = params.e.target.parentNode;
                        const tileId = clickedAsset.id;

                        // console.log("Calling handleAssetDrag");
                        this.props.handleAssetDrag(params.e, newX, newY, tileId, assetId);
                    },
                });
        }

        // In theory, the following should be used to keep these elements moving together,
        // however it seems to cause glitches with the grouping. Having only the tile draggable works fine.
        /*
        getPlumbingInstance().draggable("asset-cap-" + this.props.asset["id"]);

        // Add both elements to a posse so that they move together
        getPlumbingInstance().draggable(`asset-cap-${this.props.asset["id"]}`);
        getPlumbingInstance().addToPosse("tile-" + this.props.asset["id"], this.props.asset.id);
        getPlumbingInstance().addToPosse("asset-cap-" + this.props.asset["id"], this.props.asset.id);
        */

        this.bindEventHandlers();

        this.sourceEndpoint = {
            endpoint: "Dot",
            paintStyle: {
                stroke: "#7AB02C",
                fill: "transparent",
                radius: 10,
                strokeWidth: 1
            },
            isSource: true,
            connectorStyle: connectorPaintStyle,
            hoverPaintStyle: endpointHoverStyle,
            dragOptions:{
                start: (params) => {
                    params.e.stopPropagation();
                    params.e.preventDefault();

                    this.$el.off("mousedown");
                    this.$el.off("mouseup");
                    this.$el.off("contextmenu");
                    this.$el.off("mouseenter");
                    this.startConnection(params.e, true, false);

                    setDraggingConnection(true);
                },
                drag: (params) => {
                    params.e.stopPropagation();
                    params.e.preventDefault();

                    setDraggingConnection(true);
                },
                stop: (params) => {
                    let clickedAsset = params.e.target.parentNode;
                    let tileId = null;
                    // TODO: what is going on here?!
                    if (clickedAsset && !clickedAsset.id) {
                        clickedAsset = clickedAsset.parentNode;
                        if (clickedAsset && !clickedAsset.id) {
                            clickedAsset = clickedAsset.parentNode;
                            if (clickedAsset && clickedAsset.id) tileId = clickedAsset.id;
                        } else tileId = clickedAsset.id;
                    } else tileId = clickedAsset.id;
                    if (!tileId || !tileId.startsWith('tile-')) {
                        this.cancelConnection();
                    } else {
                        this.props.handleAssetMouseUp(params.e, tileId.replace("tile-", ""), clickedAsset, true);
                    }

                    setOuterOver(false);
                    setDragDotOver(false);
                    setDraggingConnection(false);
                    this.bindEventHandlers();
                },
            },
        };
    }

    bindEventHandlers() {
        this.$el.on("mousedown", (event) => {
            if (!draggingConnection) _delEndpoints();
            this.props.handleAssetMouseDown(event, this.props.asset["id"], this.el);
        });
        this.$el.on("mouseup", (event) => {
            this.props.handleAssetMouseUp(event, this.props.asset["id"], this.el);
        });
        this.$el.on("contextmenu", this.handleContextMenu); //N.B. comment out this line to disable the right-click context menu (if required for dev purposes)
        this.$el.on("pointerover", (event) => {
            //console.log("pointerover", this.props.asset["id"], this.props.asset["label"]);
            if (!hoveredAsset) {
                //console.log("handle asset mouse out - calling handleAssetMouseOver");
                this.props.handleAssetMouseOver(event, this.props.asset["id"]);
            }

            // this is for when assets are too close together and mouse is hover on them repeatedly
            if (hoveredAsset && this.props.asset["id"] !== hoveredAsset) {
                //console.log("assets are too close together - setTimeout");
                setTimeout(() => {
                    //console.log("timeout complete");
                    if (!draggingConnection && outerOver && this.props.asset["id"] !== hoveredAsset) {
                        //console.log("timeout complete - calling handleAssetMouseOver with changingHoveredAsset true");
                        let changingHoveredAsset = true;
                        this.props.handleAssetMouseOver(event, this.props.asset["id"], changingHoveredAsset);
                    }
                }, 32);
            }
        });
    }

    componentWillUnmount() {
        //console.log("AssertedAsset: componentWillUnmount");
        this.$el.off("mousedown");
        this.$el.off("mouseup");
        this.$el.off("contextmenu");
        this.$el.off("mouseenter");
        
        /* TODO: investigate if any of the following is necessary
         * when an asset unmounts - in particular the deleteObject.
         * Does jsplumb clear elements after re-rendering?
        let tileId = "asset-" + this.props.asset["id"];
        let jsplumb = getPlumbingInstance();
        jsPlumb.detachAllConnections(tileId);
        jsPlumb.removeAllEndpoints(tileId);
        jsPlumb.detach(tileId);
        tileId.remove()
        jsplumb.deleteObject(tileId);
        */
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        //console.log("AssertedAsset: componentDidUpdate: ", this.props.asset);
        if (editAssetLoading === this.props.asset.id && !this.props.loading) {
            editAssetLoading = '';
        }
        if (hoveredAsset === this.props.asset["id"]) {
            //console.log("render hovered asset: " + this.props.asset["id"]);
            this.props.handleAssetMouseOver(null, this.props.asset["id"]);
        }
    }
    
    /* Uncomment if required
    componentWillUnmount() {
        console.log("componentWillUnmount called for asset: " + this.props.asset.label);
        //alert("Unmounted asset: " + this.props.asset.label);
    }
    */

    /* Uncomment if required
    componentDidCatch(error, errorInfo) {
        // You can also log the error to an error reporting service
        //logErrorToMyService(error, errorInfo);
        console.log("AssertedAsset: componentDidCatch for asset: " + this.props.asset.label, error, errorInfo);
    }
    */
    
    /**
     * This method renders the asserted asset, with all of its sub components, to the tile canvas.
     * @returns {XML}
     */
    render() {
        //console.log("AssertedAsset render props: ", this.props);
        //Check assetType. If this is null, display default icon (this can happen if we are using the wrong palette, for example)
        const defaultIcon = "fallback.svg";
        const icon = this.props.assetType ? this.props.assetType["icon"] : defaultIcon;
        const icon_path = process.env.config.API_END_POINT + "/images/" + icon;

        const assetTileStyles = {
            left: this.props.asset["iconX"],
            top: this.props.asset["iconY"],
            position: "absolute"
        };
        const assetWindowStyles = {
            // 130px for asset boundary, 98px for asset
            left: this.props.asset.iconX - ((130 - 98) / 2),
            // 160px for asset boundary, 127px for asset
            top: this.props.asset.iconY - ((160 - 127) / 2),
            width: 130,
            height: 160,
        };
        const divStyles = {
            backgroundImage: "url(" + icon_path + ")",
            backgroundSize: "contain",
            backgroundRepeat: "no-repeat",
            backgroundPosition: "center center"
        };

        let loading = false;
        if (editAssetLoading === this.props.asset.id) {
            if (this.props.loading) {
                loading = true;
            } else {
                loading = (this.props.loading && this.props.isSelectedAsset(this.props.asset.id)) || this.state.loading;
            }
        } else if (!editAssetLoading.length) {
            loading = (this.props.loading && this.props.isSelectedAsset(this.props.asset.id)) || this.state.loading;
        }

        //if (this.props.asset["asserted"]) console.log("render Asset " + this.props.asset["label"]);
        
        return this.props.asset["asserted"] && 
            [<div id={"tile-" + this.props.asset["id"]}
                key={`asset-${this.props.asset.id}`} ref={el => {this.el = el}}
                className={"tile asserted-tile" +
                (this.props.asset.grouped ? " grouped-asset" : "") +
                (this.props.asset.inferredAssets.length > 0 ? " has-inferred-asset" : "") +
                (this.props.selectedInferred ? " has-selected-inferred-asset" : "")}
                style={assetTileStyles}>
                <div className="glyph-bar">
                {this.props.authz.userEdit ?
                    <div className="glyphs">
                        <AddConnectionGlyph
                            assetId={this.props.asset.id}
                            onConnClick={this.startConnection}
                        />
                        <CompleteConnectionGlyph
                            assetId={this.props.asset.id}
                            onConnClick={this.completeConnection} />
                        <span style={{ width: '100%' }}></span>
                        <DeleteTileGlyph
                            assetId={this.props.asset.id}
                            onDelClick={this.handleDelete} />
                    </div>
                    :
                    <div></div>
                }
                </div>
                <div className="bg-img" style={divStyles}><img/></div>
                {this.props.asset.inferredAssets.length > 0 &&
                <span className="text-primary fa fa-leaf tiny"/>}
                {/* <label onDoubleClick={this.handleEdit} className="text-primary title-change">{this.props.asset.label}</label> */}
                {this.props.authz.userEdit ?
                <label onClick={this.handleEdit} className="tile-label text-primary title-change">{this.props.asset.label}</label>
                :
                <label className="tile-label text-primary title-change">{this.props.asset.label}</label>
                }
                <AssetRole
                    asset={this.props.asset}
                />
                {loading && <div className="loading-overlay visible"><span
                    className="fa fa-refresh fa-spin fa-2x fa-fw"/></div>}
            </div>,
            <div key={`asset-cap-${this.props.asset["id"]}`}
                style={assetWindowStyles} className={"asset-window"}
                onPointerOver={ (e) => {this.props.handleAssetMouseOver && this.props.handleAssetMouseOver(e, this.props.asset.id)} }
                onPointerOut={ (e) => {this.props.handleAssetMouseOut && this.props.handleAssetMouseOut(e, this.props.asset.id)} }
                id={`asset-cap-${this.props.asset["id"]}`}>
            </div>];
    }

    contextTrigger(c) {
        this.contextTriggerVar = c;
    }

    modalTrigger(c) {
        this.modalTriggerVar = c;
    }

    handleMouseOver() {
        //console.log("AssertedAsset: handleMouseOver for asset: " + this.props.asset["label"]);
        //console.log("asserted asset mouseover outerover: " + outerOver);
        this.showGlyphs();

        setTimeout(() => {
            getPlumbingInstance().select({scope: "hovers"}).delete();
            setLinkHover(false);
        }, 15);

        setDragDotOver(false);
        if (!outerOver) {
            setOuterOver(true);
            _addEndpoints(`asset-cap-${this.props.asset["id"]}`, this.props.asset["id"], this.sourceEndpoint);
        }
    }

    handleMouseOut(changingHoveredAsset=false) {
        //console.log("handleMouseOut: changingHoveredAsset = " + changingHoveredAsset);
        //console.log("handleMouseOut:" + this.props.asset["id"]);
        //console.log("handleMouseOut: outerOver: " + outerOver);
        if (changingHoveredAsset) {
            //console.log("calling hideGlyphs 1");
            this.hideGlyphs();
            if (!draggingConnection) {
                setDragDotOver(false);
                _delEndpoints();
            }
        } else if (!outerOver) {
            setTimeout(() => {
                if (!dragDotOver) {
                    //console.log("calling hideGlyphs 2");
                    this.hideGlyphs();
                    if (!draggingConnection) _delEndpoints();
                }
            }, 8)
        }
    }

    editAssetType() {
        this.setState({
            ...this.state,
            editAssetTypeModal: true,
        });

        let palette = this.props.getPalette();
        if (!document.getElementById("edit-asset-type")) {
            let newDiv = document.createElement("div");
            document.body.appendChild(newDiv);
            $(newDiv).attr("id", "edit-asset-type");
        }

        ReactDOM.render(<EditAssetTypeModal
                ref={this.modalTrigger}
                show={this.state.editAssetTypeModal}
                onHide={this.closeEditAssetTypeModal}
                assetType={this.props.assetType}
                palette_assets={palette.assets}
                submit={this.submitEditAssetTypeModal}
            />,
            document.getElementById("edit-asset-type"));

        setTimeout(() => {
            console.log(this.modalTriggerVar);
            if (this.modalTriggerVar) {
                this.modalTriggerVar.handleOpen();
            }
        }, 200);


    }

    submitEditAssetTypeModal(assetType) {
        editAssetLoading = this.props.asset.id;
        //console.log("DetailPane: submitEditAssetTypeModal: " + assetType);
        let assetId = this.props.asset.id;
        let updatedAsset = {
            ...this.props.asset,
            type: assetType
        };
        //console.log("assetId: " + assetId);
        //console.log("updatedAsset: ");
        //console.log(updatedAsset);
        this.props.dispatch(putAssertedAssetType(this.props.modelId, assetId, updatedAsset));
    }

    closeEditAssetTypeModal() {
        console.log("closeEditAssetTypeModal");
        this.setState({
            ...this.state,
            editAssetTypeModal: false
        });

        if (this.modalTriggerVar) {
            this.modalTriggerVar.handleClose();
        }
    }

    /**
     * Connections
     */

    /**
     * This method starts the creation of a new relation from this asserted asset.
     */
    startConnection(event, contextMenu=true, update=false) {
        // console.log("startConnection from asset, type:", this.props.asset, this.props.assetType);
        if (update) {
            stopConnection();
            startConnection(this.props.asset["id"], this.props.linkToTypes(this.props.assetType.id),
                this.props.linkFromTypes(this.props.assetType.id), update=true);
        } else {
            let addTiles = document.getElementsByClassName("fa-stop-circle cancel-connection");
            if (addTiles.length) {
                if (addTiles[0].parentElement.parentElement.parentElement.id !== 'tile-' + this.props.asset.id) {
                    stopConnection();
                }
            }
            if (this.$el.find("span[id='tile-" + this.props.asset["id"] + "-add']").hasClass("add-connection")) {
                //console.log("startConnection from asset: " + this.props.asset["id"] + ", type: " + this.props.assetType.id);
                startConnection(this.props.asset["id"], this.props.linkToTypes(this.props.assetType.id),
                    this.props.linkFromTypes(this.props.assetType.id));
                const addGlyph = this.$el.find("span[id='tile-" + this.props.asset["id"] + "-add']");
                $(".tile").removeClass("connecting-tile");
                this.$el.addClass("connecting-tile");
                this.$el.addClass("valid-source");
                addGlyph.removeClass("fa-plus add-connection");
                addGlyph.addClass("fa-stop-circle cancel-connection");
                addGlyph.css({});
                //this.closeRelMenu();

                this.$el.find("span[id='tile-" + this.props.asset["id"] + "-delete']").hide();

            } else {
                this.cancelConnection();
                this.closeRelMenu();
            }
        }

        if (contextMenu) {
            this.$el.find("span[id='" + this.props.asset["id"] + "-add']").show();
            this.$el.find(".glyph-bar").show();
        }
    }

    /**
     * This method cancels the creation of a new relation from this asserted asset.
     */
    cancelConnection() {
        stopConnection();
        this.$el.removeClass("connecting-tile");
        const cancelGlyph = this.$el.find("span[id='tile-" + this.props.asset["id"] + "-add']");
        cancelGlyph.removeClass("fa-stop-circle cancel-connection");
        cancelGlyph.addClass("fa-plus add-connection");
    }

    /**
     * This method completes the creation of a new relation to this asserted asset.
     */
    completeConnection(event) {
        // Reset the initial tile back to its initial state.
        const assetFromId = getStartElementId();
        //console.log("completeConnection: assetFromId = ", assetFromId);
        const startingAsset = $(document).find("div[id='tile-" + assetFromId + "']");
        //console.log("completeConnection: startingAsset = ", startingAsset);
        const assetToId = this.props.asset["id"];
        //console.log("completeConnection: assetToId = ", assetToId);

        $(".tile").each((element) => {
            // Hide complete-connection glyph.
            $(element).find("span[id='" + $(element).attr("id") + "-complete']").hide();
        });

        // Get connection types possible
        const currentEndpoints = getCurrentEndpoints();
        //console.log("currentEndpoints: ", currentEndpoints);
        const relTypes = [];

        let outgoingEndpoints = currentEndpoints["outgoing"];
        let fromAssetLabel = this.props.getAssetLabelByID(assetFromId);
        let toAssetLabel = this.props.getAssetLabelByID(assetToId);

        for (let type in outgoingEndpoints) {
            //console.log("checking type: ", type);
            if (outgoingEndpoints.hasOwnProperty(type)) {
                let outgoingEndpoint = outgoingEndpoints[type];
                //console.log("checking list: ", outgoingEndpoint["assets"]);
                if (outgoingEndpoint["assets"].indexOf(assetToId) >= 0) {
                    //console.log("found asset " + assetToId + " in endpoints list for type: " + type);
                    //console.log("adding asset type to possible relTypes: ", type);
                    relTypes.push( {type: type, label: outgoingEndpoint["label"], comment: outgoingEndpoint["comment"], direction: "outgoing", from: fromAssetLabel, to: toAssetLabel} );
                }
            }
        }

        let incomingEndpoints = currentEndpoints["incoming"];
        fromAssetLabel = this.props.getAssetLabelByID(assetToId);
        toAssetLabel = this.props.getAssetLabelByID(assetFromId);

        for (let type in incomingEndpoints) {
            //console.log("checking type: ", type);
            if (incomingEndpoints.hasOwnProperty(type)) {
                let incomingEndpoint = incomingEndpoints[type];
                //console.log("checking list: ", incomingEndpoint["assets"]);
                if (incomingEndpoint["assets"].indexOf(assetToId) >= 0) {
                    //console.log("found asset " + assetToId + " in endpoints list for type: " + type);
                    //console.log("adding asset type to possible relTypes: ", type);
                    relTypes.push( {type: type, label: incomingEndpoint["label"], comment: incomingEndpoint["comment"], direction: "incoming", from: fromAssetLabel, to: toAssetLabel} );
                }
            }
        }

        //console.log("relTypes: ", relTypes);

        // Dispatch creation or offer selection of type
        if (relTypes.length < 1) {
            console.warn("Error: relation type not found");
            const startingAssetAddGlyph = startingAsset.find("span[id='tile-" + assetFromId + "-add']");
            startingAsset.removeClass("connecting-tile");
            startingAssetAddGlyph.removeClass("fa-stop-circle cancel-connection");
            startingAssetAddGlyph.addClass("fa-plus add-connection");
            startingAssetAddGlyph.hide();
        } else if (relTypes.length === 1) {
            // Dispatch the new relation to the backend to have it checked.
            let relType = relTypes[0];

            if (relType["direction"] === "outgoing") {
                this.props.dispatch(postAssertedRelation(
                    this.props.modelId,
                    assetFromId,
                    assetToId,
                    relType
                ));
            }
            else if (relType["direction"] === "incoming") {
                this.props.dispatch(postAssertedRelation(
                    this.props.modelId,
                    assetToId, //asset order reversed, compared to above
                    assetFromId,
                    relType
                ));
            }
            else {
                alert("Unknown direction: " + relType["direction"]);
            }

            const startingAssetAddGlyph = startingAsset.find("span[id='tile-" + assetFromId + "-add']");
            startingAsset.removeClass("connecting-tile");
            startingAssetAddGlyph.removeClass("fa-stop-circle cancel-connection");
            startingAssetAddGlyph.addClass("fa-plus add-connection");
            startingAssetAddGlyph.hide();
            stopConnection();

            this.props.dispatch(redrawRelations());
        } else {
            if (!document.getElementById("conn-selection-menu")) {
                let menu = document.createElement("div");
                document.body.appendChild(menu);
                $(menu).attr("id", "conn-selection-menu");
            }

            const options = [];
            relTypes.map((rt) => {
                /*
                let relType = rt["type"];
                let relDirection = rt["direction"];
                let relName = relType;

                if (relType && relType.indexOf("#") > -1) {
                    relName = relType.split("#")[1];
                }

                const option = {type: relType, label: relName, direction: relDirection};
                options.push(option);
                */
                options.push(rt);
            });

            ReactDOM.render(<RelationSelectionMenu
                updating={false}
                relation={{}}
                relationOptions={options}
                selectOption={this.updateMultiChoiceRelation}
                closeMenu={this.closeRelMenu}/>, document.getElementById("conn-selection-menu"));
            const jPopupMenu = $(".popup-menu");
            let x = event.pageX - jPopupMenu[0].clientWidth / 2, y = event.pageY - jPopupMenu[0].clientHeight / 2;
            if (x < 0) x = 0;
            if (y < 0) y = 0;

            let winX = $(document).outerWidth() - jPopupMenu[0].offsetWidth,
                winY = $(document).innerHeight() - jPopupMenu[0].offsetHeight;
            if (x > winX) {
                jPopupMenu.css({left: winX});
            } else {
                jPopupMenu.css({left: x});
            }
            if (y > winY) {
                jPopupMenu.css({top: winY});
            } else {
                jPopupMenu.css({top: y});
            }
            //jPopupMenu.on("mouseleave", () => this.closeRelMenu());
        }
    }

    changeRelationType(event, assetFromId, relationId) {

        const assetToId = this.props.asset["id"];

        const currentEndpoints = getCurrentEndpoints();
        //console.log("currentEndpoints: ", currentEndpoints);
        const relTypes = [];

        let outgoingEndpoints = currentEndpoints["outgoing"];
        let fromAssetLabel = this.props.getAssetLabelByID(assetFromId);
        let toAssetLabel = this.props.getAssetLabelByID(assetToId);

        for (let type in outgoingEndpoints) {
            //console.log("checking type: ", type);
            if (outgoingEndpoints.hasOwnProperty(type)) {
                let outgoingEndpoint = outgoingEndpoints[type];
                //console.log("checking list: ", outgoingEndpoint["assets"]);
                if (outgoingEndpoint["assets"].indexOf(assetToId) >= 0) {
                    relTypes.push({
                        type: type, label: outgoingEndpoint["label"],
                        comment: outgoingEndpoint["comment"], direction: "outgoing",
                        from: fromAssetLabel, to: toAssetLabel
                    });
                }
            }
        }

        let incomingEndpoints = currentEndpoints["incoming"];
        fromAssetLabel = this.props.getAssetLabelByID(assetToId);
        toAssetLabel = this.props.getAssetLabelByID(assetFromId);

        for (let type in incomingEndpoints) {
            //console.log("checking type: ", type);
            if (incomingEndpoints.hasOwnProperty(type)) {
                let incomingEndpoint = incomingEndpoints[type];
                //console.log("checking list: ", incomingEndpoint["assets"]);
                if (incomingEndpoint["assets"].indexOf(assetToId) >= 0) {
                    relTypes.push({
                        type: type, label: incomingEndpoint["label"],
                        comment: incomingEndpoint["comment"], direction: "incoming",
                        from: fromAssetLabel, to: toAssetLabel
                    });
                }
            }
        }

        //console.log("relTypes: ", relTypes);

        // Dispatch creation or offer selection of type
        if (relTypes.length < 1) {
            console.warn("Error: relation type not found");
        } else if (relTypes.length === 1) {
            // only one connection type exists
            alert("Only one connection type allowed.");
            stopConnection();

            this.props.dispatch(redrawRelations());
        } else {
            if (!document.getElementById("conn-selection-menu")) {
                let menu = document.createElement("div");
                document.body.appendChild(menu);
                $(menu).attr("id", "conn-selection-menu");
            }

            const options = [];
            relTypes.map((rt) => {
                options.push(rt);
            });

            let relation = {
                assetIdFrom: assetFromId,
                assetIdTo: assetToId,
                relationId: relationId
            };

            ReactDOM.render(<RelationSelectionMenu
                updating={true}
                relation={relation}
                relationOptions={options}
                selectOption={this.updateMultiChoiceRelation}
                closeMenu={this.closeRelMenu}/>, document.getElementById("conn-selection-menu"));
            const jPopupMenu = $(".popup-menu");
            let x = event.pageX - jPopupMenu[0].clientWidth / 2, y = event.pageY - jPopupMenu[0].clientHeight / 2;
            if (x < 0) x = 0;
            if (y < 0) y = 0;

            let winX = $(document).outerWidth() - jPopupMenu[0].offsetWidth,
                winY = $(document).innerHeight() - jPopupMenu[0].offsetHeight;
            if (x > winX) {
                jPopupMenu.css({left: winX});
            } else {
                jPopupMenu.css({left: x});
            }
            if (y > winY) {
                jPopupMenu.css({top: winY});
            } else {
                jPopupMenu.css({top: y});
            }
            //jPopupMenu.on("mouseleave", () => this.closeRelMenu());
        }
    }

    handleDelete() {
        //console.log("handleDelete");
        
        this.setState({
            ...this.state,
            loading: true
        });
        
        // close relation context and selection menus here otherwise they can be used to refer to null relations
        const contextDomNode = document.getElementById("conn-context-menu");
        if (contextDomNode !== null) {
            ReactDOM.unmountComponentAtNode(contextDomNode);
            $(contextDomNode).remove();
        }
        const selectionDomNode = document.getElementById("conn-selection-menu");
        if (selectionDomNode !== null) {
            ReactDOM.unmountComponentAtNode(selectionDomNode);
            $(selectionDomNode).remove();
        }
        this.props.dispatch(deleteAssertedAsset(this.props.modelId, this.props.asset.id));
        //If the asset is being connected from, then stop that connection
        if (getStartElementId() !== null && this.props.asset.id === getStartElementId()){
            stopConnection();
        }
    }

    /**
     * This method controls which glyphs to show on mouse over.
     */
    showGlyphs() {
        //console.log("showGlyphs for asset: " + this.props.asset["label"]);
        let root = document.documentElement;
        if (this.props.canvasZoom > 1.149) {
            //console.log("showGlyphs: " + this.props.asset["id"]);
            root.style.setProperty("--transform", `scale(${this.props.canvasZoom})`);
            this.$el.removeClass("glyph-hover-out");
            this.$el.addClass("glyph-hover");
            //console.log("added class: glyph-hover");
            //console.log(this.$el);
            //console.log("className: " + this.$el[0].className);
            let $assetHoverEl = $(`#asset-cap-${this.props.asset["id"]}`);
            //console.log($assetHoverEl);

            let styles = {
                width: `${130 * this.props.canvasZoom}px`,
                height: `${160 * this.props.canvasZoom}px`,
                // 130px for asset boundary, 98px for asset
                left: `${Math.ceil(this.props.asset.iconX - (((130 * this.props.canvasZoom) - 98) / 2))}px`,
                // 160px for asset boundary, 127px for asset
                top: `${Math.ceil(this.props.asset.iconY - (((160 * this.props.canvasZoom) - 127)  / 2))}px`,
            };
            Object.entries(styles).map((entry) => {
                $assetHoverEl[0].style[entry[0]] = entry[1]
            });
            let style = {...$assetHoverEl[0].style};
            const endpoints = getPlumbingInstance().select({scope: "connection-source"});
            endpoints.each((ep) => { ep.hide() });
            setTimeout(() => {
                getPlumbingInstance().revalidate(`asset-cap-${this.props.asset["id"]}`);
                endpoints.each((ep) => { ep.show() });
            }, 190);
        }

        if (isConnStarted()) {
            if ("tile-" + getStartElementId() !== this.$el.attr("id")) {
                if (getCurrentEndpointIds().indexOf(this.props.asset["id"]) >= 0) {
                    this.$el.find("span[id='tile-" + this.props.asset["id"] + "-complete']").show();
                }
            }
        } else {
            this.$el.find("span[id='tile-" + this.props.asset["id"] + "-add']").show();
            this.$el.find("span[id='tile-" + this.props.asset["id"] + "-delete']").show();
        }

        this.$el.find(".glyph-bar").show();
    }

    /**
     * Visuals
     */

    /**
     * This method controls which glyphs to hide on mouse leave.
     */
    hideGlyphs() {
        if (this.props.canvasZoom > 1.149) {
            //console.log("hideGlyphs: " + this.props.asset["id"]);
            this.$el.removeClass("glyph-hover");
            this.$el.addClass("glyph-hover-out");
            //console.log("className: " + this.$el[0].className);

            let $assetHoverEl = $(`#asset-cap-${this.props.asset["id"]}`);

            let styles = {
                width: `${130}px`,
                height: `${160}px`,
                // 130px for asset boundary, 98px for asset
                left: `${Math.ceil(this.props.asset.iconX - ((130 - 98) / 2))}px`,
                // 160px for asset boundary, 127px for asset
                top: `${Math.ceil(this.props.asset.iconY - ((160 - 127)  / 2))}px`,
            };
            Object.entries(styles).map((entry) => {
                $assetHoverEl[0].style[entry[0]] = entry[1]
            });
            const endpoints = getPlumbingInstance().select({scope: "connection-source"});
            endpoints.each((ep) => { ep.hide() });
            setTimeout(() => {
                getPlumbingInstance().revalidate(`asset-cap-${this.props.asset["id"]}`);
                endpoints.each((ep) => { ep.show() });
            }, 190);
        }
        if (!isConnStarted()) {
            if (getStartElementId() !== this.el) {
                this.$el.find("span[id='tile-" + this.props.asset["id"] + "-complete']").hide();
            }
            this.$el.find("span[id='tile-" + this.props.asset["id"] + "-add']").hide();
            this.$el.find(".glyph-bar").hide();
        }
        //jh17 - This line of code was commented out ages ago for some reason in commit 29b4e7c5
        // I have uncommented it to fix #502 but there could be something else broken by uncommenting??
        this.$el.find("span[id='tile-" + this.props.asset["id"] + "-delete']").hide();
    }

    handleContextMenu(event) {
        event.preventDefault();

        if (!document.getElementById("asset-context-menu")) {
            let menu = document.createElement("div");
            document.body.appendChild(menu);
            $(menu).attr("id", "asset-context-menu");
        }
        ReactDOM.render(<AssetCtxMenu
                modelId={ this.props.modelId }
                assetId={ this.props.asset.id }
                assetLabel={ this.props.asset.label}
                assetDescription={ this.props.assetType.description }
                assetTypeLabel={ this.props.assetType.label }
                deleteAsset={ this.handleDelete }
                startConnection={ this.startConnection }
                contextTrigger={ this.contextTrigger }
                editAssetType={ this.editAssetType }
                dispatch={ this.props.dispatch }
                authz={this.props.authz}
                />,
            document.getElementById("asset-context-menu"));

        if (this.contextTriggerVar) {
            this.contextTriggerVar.handleContextClick(event);
        }
    }


    /**
     * This method is called when the asserted asset name is double clicked.
     * @param event The "double click" event that is fired.
     */
    handleEdit(event) {
        //#824 This fixes the issue whereby pressing delete would delete selected assets. Now editing a name removes selection, using the method below.
        this.props.clearAssetSelection();

        const self = this;
        const label = $(event.target);
        //console.log(label);
        label.after("<input id='asset-rename' type='text' class='text-primary' maxLength=" + Constants.MAX_ASSET_NAME_LENGTH + "/>");
        const textBox = label.next();
        label.hide();
        textBox.show();
        textBox.val(Input.unescapeString(label.html()));
        //textBox.focus(() => $(this).select()); //KEM - not sure why this was here
        textBox.mouseup(() => false);
        /*
        textBox.on("focus", () => {
            console.log("focus:", document.activeElement);
            console.log("selection: ", document.getSelection());
            //document.getSelection().removeAllRanges();
        });
        */
        textBox.focus();
        textBox.on("blur", () => {
            //console.log("blur");
            let name = this.props.asset.label;
            let newName = textBox.val();

            if (newName === "") {
                console.log("WARNING: empty name");
            }
            else if (name === newName) {
                console.log("WARNING: name has not changed");
            }
            else if (self.props.isAssetNameTaken(name, newName)) {
                console.log("Renaming asset to:", newName);
                this.props.dispatch(putAssertedAssetRename(
                    this.props.modelId,
                    this.props.asset["id"],
                    {
                        ...this.props.asset,
                        label: newName
                    }
                ));
                label.html(Input.escapeString(newName));
            } else {
                alert("That name has already been taken!");
            }

            //console.log("show label");
            label.show();
            //console.log("remove click handler for textBox");
            $(document).off("click");
            //console.log("remove textBox");
            textBox.remove();
        });
        textBox.on("keyup",
            (e) => {
                //console.log("keyup: ", e.keyCode);
                if (e.keyCode === 13) {
                    textBox.blur();
                }
            });
        $(document).on("click",
            (e) => {
                //console.log("click: ", e.target);
                //console.log("e.target.id: ", e.target.id);
                //console.log("textBox.attr(\"id\"): ", textBox.attr("id"));
                //console.log("textBox.is(\":focus\"): ", textBox.is(":focus"));
                if (e.target.id !== textBox.attr("id") && textBox.is(":focus")) {
                    //console.log("calling blur()");
                    textBox.blur();
                }
            });
    }

    updateMultiChoiceRelation(option, direction, updating=false, relation={}) {
        console.log("relation (orig):", relation);
        console.log("selected option:", option);
        console.log("direction:", direction);

        if (direction === "outgoing") {
            if (updating) {
                console.log("calling putRelationRedefine (outgoing)");
                let updatedRelation = {...relation,
                    relType: option
                };
                console.log("updatedRelation:", updatedRelation);
                this.props.dispatch(putRelationRedefine(this.props.modelId, updatedRelation));
            } else {
                this.props.dispatch(postAssertedRelation(
                    this.props.modelId,
                    getStartElementId(),
                    this.props.asset["id"],
                    option));
            }
        }
        else if (direction === "incoming") {
            if (updating) {
                console.log("calling putRelationRedefine (incoming)");
                //switch to/from for incoming relation
                let updatedRelation = {...relation,
                    assetIdFrom: relation["assetIdTo"],
                    assetIdTo: relation["assetIdFrom"],
                    relType: option
                };
                console.log("updatedRelation:", updatedRelation);
                this.props.dispatch(putRelationRedefine(this.props.modelId, updatedRelation));
            } else {
                this.props.dispatch(postAssertedRelation(
                    this.props.modelId,
                    this.props.asset["id"],
                    getStartElementId(),
                    option));
            }
        }
        else {
            alert("Unknown direction: " + direction);
        }

        if (!updating) {
            const startingAsset = $(document).find("div[id='tile-" + getStartElementId() + "']");
            const startingAssetAddGlyph = startingAsset.find("span[id='tile-" + getStartElementId() + "-add']");
            startingAsset.removeClass("connecting-tile");
            startingAssetAddGlyph.removeClass("fa-stop-circle cancel-connection");
            startingAssetAddGlyph.addClass("fa-plus add-connection");
            startingAssetAddGlyph.hide();
        }

        this.closeRelMenu();
    }

    closeRelMenu() {
        const assetId = getStartElementId();
        const domNode = document.getElementById("conn-selection-menu");
        const startingAsset = $(document).find("div[id='tile-" + assetId + "']");

        //console.log(TileFactory.getStartElementId());

        if (domNode !== null) {
            ReactDOM.unmountComponentAtNode(domNode);
            $(domNode).remove();
            stopConnection();
        }

        //This points to the tile that was connecting TO not from

        stopConnection();
        startingAsset.removeClass("connecting-tile");
        const cancelGlyph = startingAsset.find("span[id='tile-" + assetId + "-add']");
        //console.log(cancelGlyph);
        cancelGlyph.removeClass("fa-stop-circle cancel-connection");
        cancelGlyph.addClass("fa-plus add-connection");
        cancelGlyph.hide();
    }
}

AssertedAsset.propTypes = {
    asset: PropTypes.object,
    assetType: PropTypes.object,
    getPalette: PropTypes.func,
    canvasZoom: PropTypes.number,
    isSelectedAsset: PropTypes.func,
    isAssetNameTaken: PropTypes.func,
    getAssetLabelByID: PropTypes.func,
    linkFromTypes: PropTypes.func,
    linkToTypes: PropTypes.func,
    handleAssetDrag: PropTypes.func,
    handleAssetMouseDown: PropTypes.func,
    handleAssetMouseUp: PropTypes.func,
    handleAssetMouseOver: PropTypes.func,
    modelId: PropTypes.string,
    selectedInferred: PropTypes.bool,
    loading: PropTypes.bool,
    dispatch: PropTypes.func,
    authz: PropTypes.object,
};

/* This exports the AssertedAsset class as required. */
export default AssertedAsset;
