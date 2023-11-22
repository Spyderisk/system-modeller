import React from "react";
import PropTypes from "prop-types";
import ReactDOM from "react-dom";
import {
    _delEndpoints,
    addConn,
    draggingConnection,
    getPlumbingInstance,
    getStartElementId,
    getZoom,
    hoveredAsset,
    linkHover,
    outerOver,
    setDragDotOver,
    setHoveredAsset,
    setLinkHover,
    setOuterOver,
    stopConnection,
    hoveringThreat
} from "../util/TileFactory";
import {
    changeSelectedAsset,
    changeSelectedInferredAsset,
    postAssertedAsset,
    postAssertedAssetGroup,
    putAssertedGroupRelocate,
    deleteAssertedAssetGroup,
    postAssertedRelation,
    putAssertedAssetRelocate,
    putGroupAddAssertedAsset,
    putGroupRemoveAssertedAsset,
    setGroupResizable,
    moveAssertedAssetGroup,
    printModelState,
    redrawRelations,
    relocateAssets,
    sidePanelDeactivated,
    toggleDeveloperMode,
} from "../../../modeller/actions/ModellerActions";
import AssertedAsset from "./assets/AssertedAsset";
import Group from "./groups/Group";
import CanvasCtxMenu from "./popups/CanvasCtxMenu"
import RelationCtxMenu from "./popups/RelationCtxMenu";
import InferredAssetSelectionMenu from "./popups/InferredAssetSelectionMenu";
import {connect} from "react-redux";
import {addAsset, addRelation, clearClipboard} from "../../actions/InterActions";
import * as Constants from "../../../common/constants";

const { fromJS, List, Map } = require('immutable')
var _ = require('lodash');

let assetSelection = [];
let relationsMap = Map();
var assetPosse = {};
var relationsImmutable = null;
var lastConnId = null;
var hoverCapture = null;
var hoverCaptureId = null;

var windowWidth = window.outerWidth;
var windowHeight = window.innerHeight;
var currentPixelRatio;

/**
 * This is a sub component to Modeller which renders the individual tiles.
 */
class Canvas extends React.Component {

    /**
     * Constructor is used to bind methods to this class.
     * @param props Props passed from Modeller smart component.
     */
    constructor(props) {
        super(props);

        this.state = {
            transformOrigin: [5000, 5000],
            suppressCanvasRefresh: false
        };

        this.renderAsset = this.renderAsset.bind(this);
        this.renderGroupAsset = this.renderGroupAsset.bind(this);
        this.addGroup = this.addGroup.bind(this);
        this.deleteGroup = this.deleteGroup.bind(this);
        this.getNewGroupName = this.getNewGroupName.bind(this);
        this.assetInGroup = this.assetInGroup.bind(this);
        this.assetInAnyGroup = this.assetInAnyGroup.bind(this);
        this.getAssetGroup = this.getAssetGroup.bind(this);
        this.getAssetRef = this.getAssetRef.bind(this);
        this.handleCanvasContextMenu = this.handleCanvasContextMenu.bind(this);
        this.handleClick = this.handleClick.bind(this);
        this.handleMouseDown = this.handleMouseDown.bind(this);
        this.handleMouseUp = this.handleMouseUp.bind(this);
        this.handleMouseMove = this.handleMouseMove.bind(this);
        this.handleMouseOver = this.handleMouseOver.bind(this);
        this.handleConnMouseOver = this.handleConnMouseOver.bind(this);
        this.handleConnMouseOut = this.handleConnMouseOut.bind(this);
        this.unhoverConn = this.unhoverConn.bind(this);
        this.addHoveredConn = this.addHoveredConn.bind(this);
        this.handleScroll = this.handleScroll.bind(this);
        this.handleKeyDown = this.handleKeyDown.bind(this);
        this.handleKeyUp = this.handleKeyUp.bind(this);
        this.handleAssetDrag = this.handleAssetDrag.bind(this);
        this.handleGroupDrag = this.handleGroupDrag.bind(this);
        this.expandGroup = this.expandGroup.bind(this);
        this.collapseGroup = this.collapseGroup.bind(this);
        this.hideGroupConnectionSpinners = this.hideGroupConnectionSpinners.bind(this);
        this.handleAssetMouseUp = this.handleAssetMouseUp.bind(this);
        this.handleAssetMouseDown = this.handleAssetMouseDown.bind(this);
        this.handleAssetMouseOver = this.handleAssetMouseOver.bind(this);
        this.handleAssetMouseOut = this.handleAssetMouseOut.bind(this);
        this.handleRelationClick = this.handleRelationClick.bind(this);
        this.getValidStartpoints = this.getValidStartpoints.bind(this);
        this.getValidEndpoints = this.getValidEndpoints.bind(this);
        this.getPaletteLink = this.getPaletteLink.bind(this);
        this.getPalette = this.getPalette.bind(this);
        this.isSelectedAsset = this.isSelectedAsset.bind(this);
        this.getAssetLabel = this.getAssetLabel.bind(this);
        this.getAssetLabelByID = this.getAssetLabelByID.bind(this);
        this.getInferredAssetOptions = this.getInferredAssetOptions.bind(this);
        this.updateMultiChoiceInferredAsset = this.updateMultiChoiceInferredAsset.bind(this);
        this.closeInferredAssetsMenu = this.closeInferredAssetsMenu.bind(this);
        this.contextTrigger = this.contextTrigger.bind(this);
        this.canvasContextTrigger = this.canvasContextTrigger.bind(this);
        this.reCentreModel = this.reCentreModel.bind(this);
        this.reCentreCanvas = this.reCentreCanvas.bind(this);
        this.updateViewBoundary = this.updateViewBoundary.bind(this);

        this.contextTriggerVar = null;
        this.canvasContextTriggerVar = null;
        this.firstTime = true;
        this.firstDrawing = true;
        this.recentring = false;

        this.tox_delta = 0;
        this.toy_delta = 0;
    }

    /**
     * React Lifecycle Methods
     */

    /**
     * Called after the component is rendered.
     * Used to render all the relations once the asserted assets are in place.
     */
    componentDidMount() {
        this.el = this.refs["tile-canvas"];
        this.$el = $(this.el);
        //console.log("Canvas.componentDidMount");
        //console.log("this.el: ", this.el);

        this.mouseDown = false;

        //console.log("Canvas: mouseDown ", this.mouseDown);

        this.transformOrigin = this.props.canvas.transformOrigin;
        //console.log("Canvas: transformOrigin ", this.transformOrigin);

        windowHeight = window.innerHeight;
        windowWidth = window.outerWidth;
        var scrollTop = 5000 - windowHeight / 2;
        var scrollLeft = 5000 - windowWidth / 2;

        //saved scroll values
        this.scrollLeft = scrollLeft;
        this.scrollTop = scrollTop;

        //console.log("window height: ", windowHeight);
        //console.log("window width: ", windowWidth);
        //console.log("scrollTop: ", scrollTop);
        //console.log("scrollLeft: ", scrollLeft);

        $("#canvas-container").animate({
           scrollTop: scrollTop,
           scrollLeft: scrollLeft
        }, 1);

        /* KEM: may be used for debugging scrolling
        $("#canvas-container").scroll(function(e) {
            var scrollTop = e.target.scrollTop;
            var scrollLeft = e.target.scrollLeft;
            console.log("scroll: scrollLeft, scrollTop:", scrollLeft, scrollTop);
        });
        */

        currentPixelRatio = 1 / window.devicePixelRatio;

        let leftOffset = this.props.view.leftSidePanelWidth;
        let rightOffset = this.props.view.rightSidePanelWidth;

        // update the "view-boundary" element to restrict window movement.
        let viewWidth = windowWidth - leftOffset - rightOffset + 500;
        let viewHeight = windowHeight - 50;
        let viewXPos = this.transformOrigin[0] - (viewWidth / 2) + 125;
        let viewYPos = this.transformOrigin[1] - (viewHeight / 2) - (50 / 2);

        // account for browser zoom
        viewWidth = viewWidth * currentPixelRatio;
        viewHeight = viewHeight * currentPixelRatio;

        // offset the bottom side so that windows can be dragged under the bottom of the screen
        viewHeight += 800;

        this.el.insertAdjacentHTML('afterEnd',
            `<div id="view-boundary" style="z-index: -50; width: ${viewWidth}px; height: ${viewHeight}px;
                       left: ${viewXPos}px; top: ${viewYPos}px; position: absolute; ">
                  </div>`
        );

        //jCanvas.attr("id", "tile-canvas");

        this.jsplumb = getPlumbingInstance();
        let jsplumb = this.jsplumb;
        jsplumb.setContainer(this.$el);
        jsplumb.recalculateOffsets(document.body);
        // tileSetZoom(1, instance, [5000, 5000]);

        //console.log("Canvas componentDidMount: jsPlumb.bind group:add");
        jsplumb.bind("group:add", (group) => {
            //console.log("--- jsPlumb event: group:add (nothing yet implemented) ---", group);
        });
        
        // console.log("Canvas componentDidMount: jsPlumb.bind group:addMember on group");
        jsplumb.bind("group:addMember", (p) => {
            // console.log("--- jsPlumb event: group:addMember ---", p);
            // console.log("jsPlumb: added element: " + p.el.id + " to group " + p.group.id);
            //console.log("jsPlumb: updated members: ", p.group.getMembers());
            let assetId = p.el.id.replace("tile-", "");
            let sourceGroup = p.sourceGroup;
            if (this.assetInGroup(assetId, p.group.id)) {
                // console.log("Asset " + assetId + " already in asset group " + p.group.id);
            }
            else {
                let updatedAsset = {
                    id: assetId,
                    iconX: p.pos.left,
                    iconY: p.pos.top
                }
                if (sourceGroup) {
                    // console.log("jsPlumb: moving element: " + p.el.id + " from group " + sourceGroup.id + " to group " + p.group.id);
                    if (p.group.id === sourceGroup.id) {
                        // console.warn("Element " + p.el.id + " already in group " + p.group.id);
                    }
                    else {
                        this.movingAssetGroup = true;
                        this.props.dispatch(moveAssertedAssetGroup(this.props.model["id"], assetId, sourceGroup.id, p.group.id, updatedAsset));
                    }
                }
                else {
                    // console.log("Calling putGroupAddAssertedAsset: ", this.props.model["id"], p.group.id, updatedAsset);
                    this.props.dispatch(putGroupAddAssertedAsset(this.props.model["id"], p.group.id, updatedAsset));
                    
                    //ensure that asset tile has the grouped-asset class (if asset is not re-rendered)
                    let tileid = "#tile-" + assetId;
                    $(tileid).addClass("grouped-asset");
                }
            }
        });
        
        // console.log("Canvas componentDidMount: jsPlumb.bind group:removeMember on group");
        jsplumb.bind("group:removeMember", (p) => {
            // console.log("--- jsPlumb event: group:removeMember ---", p);
            if (this.movingAssetGroup) {
                console.log("Currently moving asset to new group - ignoring this request..");
                this.movingAssetGroup = false;
                return;
            }
            
            let assetId = p.el.id.replace("tile-", "");
            let el = p.el;
            let targetGroup = p.targetGroup;
            
            if (targetGroup) {
                // console.log("jsPlumb: moving element: " + p.el.id + " from group " + p.group.id + " to group " + targetGroup.id);
                // console.log("jsPlumb: assuming addMember will handle this..");
            }
            else {
                // console.log("jsPlumb: removed element: " + p.el.id + " from group " + p.group.id);
                //console.log("jsPlumb: updated members: ", p.group.getMembers());
                // console.log("Calling putGroupRemoveAssertedAsset: ", this.props.model["id"], p.group.id, assetId);
                let offsetLeft = parseInt(el.getAttribute("newpos_left"));
                let offsetTop = parseInt(el.getAttribute("newpos_top"));
                // console.log("jsPlumb: element moved to location: ", offsetLeft, offsetTop);
                let updatedAsset = {
                    id: assetId,
                    iconX: offsetLeft,
                    iconY: offsetTop
                }
                this.props.dispatch(putGroupRemoveAssertedAsset(this.props.model["id"], p.group.id, updatedAsset));
            }
        });
        
        
        //Add right-click context menu handler for canvas (currently used for adding group)
        if (! Constants.DISABLE_GROUPING) {
            this.$el.on("contextmenu", (event) => {
                this.handleCanvasContextMenu(event);
            });
        }
        
        this.forceUpdate();
    }

    /**
     * Called before the component is removed.
     * Used to remove all connections so that there are no trailing remnants.
     */
    componentWillUnmount() {
        //console.log("Canvas.componentWillUnmount");
        $(document).off("keyup");
        $(document).off("keydown");
        let plumbingInstance = getPlumbingInstance()

        plumbingInstance.reset();
    }

    shouldComponentUpdate(nextProps, nextState) {
        let shouldComponentUpdate = true;
        //console.log("this.props.loading:", this.props.loading);
        //console.log("nextProps.loading: ", nextProps.loading);
        
        if (this.props.model.id !== nextProps.model.id) {
            //console.log("shouldComponentUpdate: model id changed (model loaded) -> true");
            return true;
        }

        if (this.props.model.assets.length !== nextProps.model.assets.length) {
            //console.log("shouldComponentUpdate: asset added/removed -> true");
            return true;
        }
        
        let jsGroups = this.jsplumb.getGroups();
        //console.log("shouldComponentUpdate: jsGroups:", jsGroups);
        jsGroups.map((jsGroup) => {
            let members = jsGroup.getMembers();
            //console.log("shouldComponentUpdate: group members:", members);
        });
        
        //console.log("shouldComponentUpdate: this.props.movedAsset = " + this.props.movedAsset);
        //console.log("shouldComponentUpdate: nextProps.movedAsset = " + nextProps.movedAsset);
        if (this.props.movedAsset !== nextProps.movedAsset) {
            //console.log("shouldComponentUpdate: asset moving (or moved) -> true");
            return true;
        }
        
        //console.log("shouldComponentUpdate: nextProps: ", nextProps);
        //console.log("shouldComponentUpdate: grouping inProgress = " + nextProps.grouping.inProgress);
        if (nextProps.grouping.inProgress) {
            //console.log("shouldComponentUpdate: grouping in progress -> false");
            return false;
        }
        else if (this.props.grouping.inProgress && !nextProps.grouping.inProgress) {
            //console.log("shouldComponentUpdate: grouping task completed -> true");
            return true;
        }
        
        if (this.props.sidePanelActivated !== nextProps.sidePanelActivated) {
            //console.log("shouldComponentUpdate: sidePanelActivated changed -> false");
            return false;
        }

        if (this.props.selectedThreat.id !== nextProps.selectedThreat.id) {
            //console.log("shouldComponentUpdate: selected threat changed -> false");
            return false;
        }
        else {
            //console.log("shouldComponentUpdate: (selected threat has not changed)");
            //console.log("shouldComponentUpdate: hoveringThreat: ", hoveringThreat);
            if (hoveringThreat) {
                //console.log("shouldComponentUpdate: hovering threat -> false");
                return false;
            }
        }

        //console.log("shouldComponentUpdate: ", this.props.groups, nextProps.groups);
        
        if (this.props.groups !== nextProps.groups) {
            if (this.props.groups.length !== nextProps.groups.length) {
                //console.log("shouldComponentUpdate: group added or removed (will render groups)")
            }
            else {
                //console.log("shouldComponentUpdate: one or more groups changed -> true");
                return true;
            }
        }
        
        if (nextProps.loading.model) {
            //console.log("Canvas.shouldComponentUpdate: false: (model loading)");
            return false;
        }

        if (nextProps.model.validating) {
            //console.log("Canvas.shouldComponentUpdate: false: (model validating)");
            return false;
        }

        let { leftSidePanelWidth, rightSidePanelWidth } = this.props.view;

        if (nextProps.view.leftSidePanelWidth !== leftSidePanelWidth ||
                    nextProps.view.rightSidePanelWidth !== rightSidePanelWidth ||
                    currentPixelRatio !== (1 / window.devicePixelRatio)) {
            currentPixelRatio = (1 / window.devicePixelRatio);
            windowHeight = window.innerHeight;
            windowWidth = window.outerWidth;

            this.updateViewBoundary(nextProps.view.leftSidePanelWidth,
                    nextProps.view.rightSidePanelWidth)
        }
        
        if (this.props.view.rightSidePanelWidth !== nextProps.view.rightSidePanelWidth) {
            //console.log("shouldComponentUpdate: rightSidePanelWidth changed -> false");
            return false;
        }

        if (this.props.view.leftSidePanelWidth !== nextProps.view.leftSidePanelWidth) {
            //console.log("shouldComponentUpdate: leftSidePanelWidth changed -> false");
            return false;
        }

        //console.log("Canvas.shouldComponentUpdate: " + shouldComponentUpdate);
        return shouldComponentUpdate;
    }


    /**
     * Called after a component is updated for whatever reason.
     * Updates all of the connections.
     */
    componentDidUpdate(prevProps, prevState) {
        //console.log("Canvas componentDidUpdate state change: ", prevState, this.state);
        //console.log("Canvas componentDidUpdate props change: ", prevProps, this.props);

        this.scrollLeft = $('#canvas-container').prop('scrollLeft');
        this.scrollTop = $('#canvas-container').prop('scrollTop');

        if (!prevProps.sidePanelActivated && this.props.sidePanelActivated) {
            //console.log("side panel activated - reducing any expanded assets..");
            if (hoveredAsset && (hoveredAsset !== "")) {
                // console.log("Calling hideGlyphs on asset: " + hoveredAsset);
                let assetRef = this.getAssetRef(hoveredAsset);
                if (assetRef) assetRef[hoveredAsset].hideGlyphs();
            }
            
            //clear all hovered relations
            //console.log("side panel activated - reducing any hovered relations..");
            getPlumbingInstance().select({scope: "hovers"}).delete();
        }
        
        if (!this.props.loading.model && this.firstTime) {
            let instance = getPlumbingInstance();
            // console.log("Canvas: componentDidUpdate: adding event handlers, e.g. handleMouseOver");
            instance.on(this.$el, "mousedown", this.handleMouseDown);
            instance.on(this.$el, "mouseup", this.handleMouseUp);
            instance.on(this.$el, "mousemove", this.handleMouseMove);
            instance.on(this.$el, "mouseover", this.handleMouseOver)

            $(document).off("keydown");
            $(document).off("keyup");
            $(document).on("keydown", this.handleKeyDown);
            $(document).on("keyup", this.handleKeyUp);

            if (this.props.model.relations.length > 0) {
                relationsImmutable = fromJS(this.props.model.relations);
            }
        }
        if (!this.firstTime) {
            if (this.props.view.showInferredRelations != prevProps.view.showInferredRelations) {
                this.redrawRelations();
            }
            else if (this.props.view.showHiddenRelations != prevProps.view.showHiddenRelations) {
                this.redrawRelations();
            }
            else if (this.props.view.reCentreCanvas != prevProps.view.reCentreCanvas) {
                this.reCentreCanvas();
            }
            else if (this.props.view.reCentreModel != prevProps.view.reCentreModel) {
                this.reCentreModel();
            }
            else if (this.props.redrawRelations != prevProps.redrawRelations) {
                this.redrawRelations();
            }
            else if (this.props.selectedAsset.id !== prevProps.selectedAsset.id) {
                //console.log("selected asset changed from " + prevProps.selectedAsset.id + " to " + this.props.selectedAsset.id);
                this.redrawRelations();
                /* KEM - the following code was used in an attempt to fix an issue with selecting an asset
                 * via the relations panel (i.e. the asset did not appear selected) - see commit 62b99b6eecfb7d194425512cd3413fda14ede1a1
                 * The problem here is that it causes problems when user tries to select an asset, i.e. it loses the glyph-hover class
                 * due to the search/replace for "hover" below. TODO: fix original issue another way!
                setTimeout(() => {
                    let tiles = document.getElementsByClassName('asserted-tile')
                    _.forEach(tiles, (tile) => {
                        tile.className = tile.className.replace(/\s?danger/, "");
                        tile.className = tile.className.replace(/\s?hover/, "");
                        tile.className = tile.className.replace(/\s?active-tile/, "");
                        tile.className = tile.className.replace(/\s?selected/, "");
                    })

                    let selectedTile = document.getElementById(`tile-${this.props.selectedAsset.id}`)
                    if (selectedTile) {
                        selectedTile.className += " active-tile";
                    }
                }, 100)
                */
            }
            else {
                let newRelations = fromJS(this.props.model.relations);

                if (!newRelations.equals(relationsImmutable)) {
                    relationsImmutable = newRelations;
                    this.redrawRelations();
                }
            }
        }

        if (this.props.model.assets.length && this.props.model.assets.length !== Object.keys(assetPosse).length) {
            Object.keys(assetPosse).map((key) => {
                if (this.props.model.assets.filter(asset => {asset.id === key}).length === 0) delete assetPosse[key];
            });
            this.props.model.assets.map((asset) => {
                if (asset.asserted === true && !assetPosse.hasOwnProperty(asset.id)) {
                    assetPosse[asset.id] = [asset.iconX, asset.iconY];
                }
            })
        }

        if ( (prevProps.model.id !== this.props.model.id) || 
             (prevProps.model.loadingId && !this.props.model.loadingId) ){
            //console.log("Canvas componentDidUpdate: model loaded and rendered. Will redraw relations");
            this.redrawRelations();
        }

        if (this.zoom != getZoom()) {
            window.setZoom(1 / getZoom(), getPlumbingInstance(), this.transformOrigin)
            this.zoom = getZoom();
        }
        // if (prevProps.canvas.zoom != this.props.canvas.zoom) {
        //     //console.log("componentDidUpdate: canvas zoom update:", this.props.canvas.zoom);
        //     //this.setState({...this.state, transformOrigin: transformOrigin});
        //     tileSetZoom(this.props.canvas.zoom, getPlumbingInstance(), this.props.canvas.transformOrigin);
        // }
        // if (prevProps.canvas.transformOrigin != this.props.canvas.transformOrigin) {
        //     //console.log("componentDidUpdate: canvas transformOrigin update:", this.props.canvas.transformOrigin);
        //     //this.setState({...this.state, transformOrigin: transformOrigin});
        //     tileSetZoom(this.props.canvas.zoom, getPlumbingInstance(), this.props.canvas.transformOrigin);
        // }

        //console.log("Canvas componentDidUpdate: updated group?");
        //console.log(prevProps.groups, this.props.groups);

        if (prevProps.groups !== this.props.groups) {
            if (prevProps.groups.length !== this.props.groups.length) {
                console.log("Canvas componentDidUpdate: group added or removed (groups rendered)")
            }
            else {
                //console.log("Canvas componentDidUpdate: one or more groups changed - redrawing relations..");
                this.redrawRelations();
            }
        }
                        
        let groups = this.props.groups;
        
        if (false && groups.length > 0) {//disabled for now (see below)
            //This section checks through all defined groups and ensures that
            //each grouped asset is also in the corresponding jsPlumb group
            //TODO: check how much of this is actually required here, as we now do some of
            //these checks in Group.js
            // console.log("Canvas componentDidUpdate: checking groups: ", groups);
            groups.map((group) => {
                let jsGroup = this.jsplumb.getGroup(group.id);
                console.log("Canvas componentDidUpdate: id, jsGroup: ", group.id, jsGroup);
                if (jsGroup) {
                    let members = jsGroup.getMembers();
                    console.log("Canvas componentDidUpdate: jsGroup.members: ", members);
                    let assetIds = group.assetIds;
                    console.log("Canvas componentDidUpdate: group.assetIds: ", assetIds);
                    assetIds.map((assetId) => {
                        console.log("Canvas componentDidUpdate: checking " + assetId);
                        let assetElId = "tile-" + assetId;
                        let assetGrp = this.jsplumb.getGroupFor(assetElId);
                        console.log("Canvas componentDidUpdate: assetGrp for " + assetElId + ": ", assetGrp);
                        if (assetGrp && assetGrp.id === jsGroup.id) {
                            console.log("Canvas componentDidUpdate: " + assetId + " already in jsPlumb group " + jsGroup.id);
                        }
                        else {
                            console.log("Canvas componentDidUpdate: adding " + assetId + " to jsPlumb group..");
                            let tileId = "#tile-" + assetId;
                            let el = $(tileId)[0];
                            console.log("Canvas componentDidUpdate: el:", el);
                            if (el) {
                                console.log("Canvas componentDidUpdate: addToGroup: ", jsGroup.id, el);
                                this.jsplumb.addToGroup(jsGroup.id, el);
                                //console.log("Canvas componentDidUpdate: updated group: ", jsGroup.getMembers());
                            }
                            else {
                                console.warn("Canvas componentDidUpdate: cannot locate asset tile: " + tileId);
                            }
                        }
                    });
                }
            });
        }
        
        if (!this.props.loading.model && this.firstTime) this.firstTime = false;
        
        //For now, ensure that relations are redrawn after any canvas update
        //TODO: need to optimise this better (avoid unnecessary redraws, etc)
        this.redrawRelations();
        
    }

    componentDidCatch(error, errorInfo) {
        console.log("Canvas: componentDidCatch: ", error, errorInfo);
        alert("ERROR rendering Canvas: Please refresh the page!");
    }
  
    static getDerivedStateFromError(error) {
        console.log("Canvas: getDerivedStateFromError: " + error);
        return { hasError: true };
    }
    
    /**
     * This renders our React components to the Virtual DOM. In this case we have a collection of asserted asset
     * tiles to render.
     * @returns {XML} It is returning the XML structure used to render the HTML.
     */
    render() {
        //$("#tile-canvas").find(".tile").removeClass("active-tile");
        //console.log("Canvas render(): current groups: ", this.props.groups);

        var self = this;
        
        let hasError = this.state.hasError;
        
        if (hasError) {
            console.warn("Canvas has error");
        }
        
        let className = "tile-canvas" + (hasError ? " canvas-error" : "");
        //console.log("Canvas className: " + className);
        
        return (
            <div id="canvas-container" ref="canvas-container" onScroll={this.handleScroll}>
                <div className={className} id="tile-canvas" ref="tile-canvas">
                    {!hasError && this.props.model["assets"].map((asset) => {
                        if (this.assetInAnyGroup(asset)) {
                            //console.log("Not rendering asset on canvas (part of group): " + asset.label);
                        }
                        else {
                            //console.log("renderAsset: ", asset);
                            asset.grouped = false;
                            return this.renderAsset(asset);
                        }
                    })}
                    {!hasError && this.props.groups.map((group) => {
                        //console.log("Canvas: render group: ", group);
                        return <Group group={group}
                                      key={group.id} ref={group.id}
                                      modelId={self.props.model["id"]}
                                      isGroupNameTaken={self.props.isGroupNameTaken}
                                      renderGroupAsset={this.renderGroupAsset}
                                      handleGroupDrag={self.handleGroupDrag}
                                      expandGroup={this.expandGroup}
                                      collapseGroup={this.collapseGroup}
                                      deleteGroup={this.deleteGroup}
                                      dispatch={self.props.dispatch}
                        />;
                    })}
                </div>
            </div>
        );
    }
    
    handleCanvasContextMenu(event) {
        //console.log("handleCanvasContextMenu: ", event);
        //console.log("handleCanvasContextMenu: ", event.originalEvent);

        //Only handle event on canvas
        if (event.target.id !== "tile-canvas") {
            //console.log("handleCanvasContextMenu: target is not canvas (ignoring)");
            return;
        }
        
        event.preventDefault();
        
        let offsetX = event.originalEvent.offsetX;
        let offsetY = event.originalEvent.offsetY;

        if (!document.getElementById("canvas-context-menu")) {
            //console.log("Creating canvas-context-menu div");
            let menu = document.createElement("div");
            document.body.appendChild(menu);
            $(menu).attr("id", "canvas-context-menu");
        }
        else {
            //console.log("(canvas-context-menu div already exists)");
        }
        
        ReactDOM.render(<CanvasCtxMenu
                x={ offsetX }
                y={ offsetY }
                addGroup={ this.addGroup }
                contextTrigger={ this.canvasContextTrigger }
                />,
            document.getElementById("canvas-context-menu"));

        if (this.canvasContextTriggerVar) {
            this.canvasContextTriggerVar.handleContextClick(event);
        }
    }
    
    addGroup(x, y) {
        console.log("addGroup at pos: ", x, y);
        let name = this.getNewGroupName();
        console.log("Creating new group: " + name);
        let newGroup = {
            name: name,
            top: y + "px",
            left: x + "px",
            assetIds: []
        };
        console.log("Calling postAssertedAssetGroup: ", newGroup);
        this.props.dispatch(postAssertedAssetGroup(this.props.model.id, newGroup));
    }
    
    deleteGroup(group, deleteAssets) {
        console.log("deleteGroup: ", group, deleteAssets);
        this.props.dispatch(deleteAssertedAssetGroup(this.props.model.id, group, deleteAssets));
        
        //If we are NOT deleting the assets, we need to reposition them on the canvas
        if (!deleteAssets && (group.assetIds.length > 0)) {
            let assetIds = group.assetIds;
            let updatedAssets = [];
            assetIds.map((assetId) => {
                let assetElId = "#tile-" + assetId;
                let el = $(assetElId)[0];
                //console.log("Getting offset for element:", el);
                //Use jsPlumb method to determine the offset w.r.t the canvas
                let out = this.jsplumb.getOffset(el);
                //console.log("Element offset: ", out);

                let asset = this.props.model.assets.filter((asset) => {
                    return asset.id === assetId;
                })[0];
                
                //console.log("asset: ", asset);
                
                if (asset) {
                    //Set updated asset location
                    let updatedAsset = {
                        uri: asset.uri,
                        iconX: out.left,
                        iconY: out.top
                    };

                    //console.log("updatedAsset: ", updatedAsset);
                    updatedAssets.push(updatedAsset);
                }
            });

            if (updatedAssets.length > 0) {
                this.props.dispatch(relocateAssets(this.props.model.id, updatedAssets));
            }
            else {
                console.warn("updatedAssets is empty - not updating asset locations");
            }
        }
    }
    
    getNewGroupName() {
        let groupNames = this.props.groups.map((group) => {
            return group.name;
        });
        
        console.log("Current group names: ", groupNames);
        
        let newGroupName = "";
        let foundName = true;
        let i=1;
        
        while (foundName && i<100) {
            let checkName = "Group " + i;
            foundName = groupNames.includes(checkName);
            //console.log(checkName + ": " + foundName);
            if (!foundName) {
                newGroupName = checkName;
            }
            i++;
        }
        
        return newGroupName;
    }
    
    //Check if given asset is in specified group
    assetInGroup(assetId, groupId) {
        let assetInGrp = false;
        let group = this.props.groups.find((group) => group["id"] === groupId);
        if (group && group.assetIds.includes(assetId)) {
            assetInGrp = true;
        }
        
        return assetInGrp;
    }
    
    //Check if given asset is in any group
    assetInAnyGroup(asset) {
        let assetInGrp = false;
        this.props.groups.map((group) => {
            //console.log("assetInAnyGroup?: " + asset.id, group);
            if (group.assetIds.includes(asset.id)) {
                //console.log("assetInAnyGroup: true: " + asset.id, group);
                assetInGrp = true;
            }
        });
        
        //console.log("assetInAnyGroup: " + asset.id + " = " + assetInGrp);
        return assetInGrp;
    }
    
    //Get group that asset belongs to
    getAssetGroup(assetId) {
        let assetGrp = null;
        this.props.groups.map((group) => {
            if (group.assetIds.includes(assetId)) {
                assetGrp = group;
            }
        });
        
        return assetGrp;
    }
    
    getAssetRef(assetId) {
        //console.log("this.refs: ", this.refs);
        let assetRef = this.refs[assetId];
        if (!assetRef) {
            let assetGrp = this.getAssetGroup(assetId);
            //console.log("asset grp for asset " + assetId + ": " + assetGrp);
            if (assetGrp) {
                let group = this.refs[assetGrp.id];
                assetRef = group.refs[assetId];
            }
        }
        
        //console.log("assetRef for " + assetId + ": ", assetRef);
        return assetRef;
    }
    
    renderGroupAsset(assetElId) {
        //console.log("renderGroupAsset: element id", assetElId);
        let assetId = assetElId.replace("tile-", "");
        //console.log("renderGroupAsset: asset id", assetId);
        let asset = this.props.model["assets"].find((asset) => asset["id"] === assetId);
        asset.grouped = true; //flag to indicate that asset is within a group
        //console.log("renderGroupAsset: calling renderAsset: ", asset);
        return this.renderAsset(asset);
    }
    
    renderAsset(asset) {
        //console.log("renderAsset: ", asset);
        
        if (! asset) {
            console.warn("renderAsset(): Cannot render undefined asset!");
            return;
        }
        
        var self = this;

        if (this.firstDrawing) {
            console.log("--- first attempt to draw asset on jsPlumb ---");
            this.firstDrawing = false;
        }

        let assetType = self.props.getAssetType(asset["type"]);

        if (this.props.isAssetDisplayed(assetType)) {
            let selectedAsset = this.props.model.assets.find(asset => asset["id"] === this.props.selectedAsset["id"]);
            let inferredSelection = false;
            if (selectedAsset !== undefined && asset.inferredAssets.indexOf(selectedAsset["uri"]) > -1) {
                inferredSelection = true;
            }
            return <AssertedAsset 
                key={asset["id"]}
                ref={asset["id"]}
                asset={asset}
                assetType={assetType}
                getPalette={this.getPalette}
                canvasZoom={this.props.canvas.zoom}
                linkFromTypes={self.getValidStartpoints}
                linkToTypes={self.getValidEndpoints}
                loading={this.props.loading["asset"]}
                handleAssetDrag={ self.handleAssetDrag }
                handleAssetMouseDown={ self.handleAssetMouseDown }
                handleAssetMouseUp={ self.handleAssetMouseUp}
                handleAssetMouseOver={ self.handleAssetMouseOver }
                isSelectedAsset={self.isSelectedAsset}
                isAssetNameTaken={self.props.isAssetNameTaken}
                getAssetLabelByID={this.getAssetLabelByID}
                modelId={self.props.model["id"]}
                selectedInferred={inferredSelection}
                clearAssetSelection={self.clearAssetSelection}
                dispatch={self.props.dispatch}
                authz= {self.props.authz}
                />;
        }
    }

    /**
     * Visuals
     */

    /**
     * Used to update all connections on the UI.
     */
    redrawRelations(instance=getPlumbingInstance()) {
        var self = this;

        //console.log("suppressCanvasRefresh = " + self.props.suppressCanvasRefresh);

        if (self.state.suppressCanvasRefresh) {
            // console.log("Suppressing canvas refresh");
            return;
        }
        
        //console.log("redrawRelations");
        
        //Following is only necessary if one or more groups is defined
        //Groups should be expanded initially, in order to connect up the assets properly
        //otherwise, connections to collapsed groups are sent to the top-left corner of the canvas!
        let groups = this.props.groups;
        if (groups.length > 0) {
            //Initially expand all groups, so that relations will display properly
            groups.map((group) => {
                //console.log("Group: calling expandGroup for " + group.id + 
                //   (!group.expanded ? " (will collapse after redrawing relations)" : ""));
                this.expandGroup(group.id);
            });
            
            //Now redraw connections; those to grouped assets should display correctly
            //console.log("Canvas componentDidUpdate: redrawConnections");
            self.redrawConnections(instance);
            
            //console.log("Canvas componentDidUpdate: checking for collapsed groups: ", groups);
            //Finally collapse all groups that have expanded=false
            groups.map((group) => {
                //console.log("Group " + group.id + " expanded = " + group.expanded);
                if (!group.expanded) {
                    this.collapseGroup(group.id);
                }
            });
        }
        else {
            //If no groups defined, simply call redrawConnections
            this.redrawConnections(instance);
        }
    }

    redrawConnections(instance) {
        var self = this;
        //console.log("Redrawing relations on canvas");

        instance.select({scope:
                ["relations", "inferred-relations", "hovers", "patterns"]
        }, true).unbind();

        instance.deleteEveryEndpoint();
        instance.deleteEveryConnection();

        relationsMap = Map();

        let sortedRelations = _.orderBy((self.props.model["relations"]), ['asserted'], ['desc']);

        instance.batch(() => {
            sortedRelations.map((relation) => {
                if (relation["visible"] === false) return;
                var labelClass = "label-tag"; // "fa fa-spinner fa-pulse fa-lg fa-fw"
                var labelId = "label" + relation["id"];
                var spinnerId = "spinner" + relation["id"];
                let relationType = "basic";
                let scope = "relations";
                if (relation.asserted === false) {
                    relationType = "inferred";
                    scope = "inferred-relations";
                }

                let showRelation = true;
                if (!self.props.view.showInferredRelations && !relation.asserted) {
                    if (self.props.view.showHiddenRelations && relation.hidden) {
                        showRelation = true;
                    } else showRelation = false;
                }
                else if (!self.props.view.showHiddenRelations && relation.hidden) {
                    showRelation = false;
                }

                if (self.props.isRelationDisplayed(relation) && showRelation) {
                    var inferredAsset = relation["inferredAsset"]; // deprecated
                    var inferredAssets = relation["inferredAssets"]; // new field, allowing multiple assets

                    //Support for previous method, if still used
                    if (inferredAssets === null) {
                        inferredAssets = [];
                        if (inferredAsset !== null) {
                            inferredAssets.push(inferredAsset);
                        }
                    }

                    if (inferredAssets.length > 0) {
                        //mark relation as having inferred asset(s)
                        labelClass += " has-inferred-asset";
                        let selectedAsset = this.props.model.assets.find(asset => asset["id"] === this.props.selectedAsset["id"]);
                        if (selectedAsset !== undefined && inferredAssets.indexOf(selectedAsset["uri"]) > -1) {
                            labelClass += " has-selected-inferred-asset";
                        }
                    }

                    let source = $(document).find("div[id='tile-" + relation["fromID"] + "']");
                    let target = $(document).find("div[id='tile-" + relation["toID"] + "']");

                    if (!(source.length === 0 || target.length === 0)) {
                        let conn = addConn(relation, labelClass, relationType, scope);
                        if (conn !== undefined) {
                            let relMapMap = relationsMap.get(relation["fromID"], Map());
                            let relMapMapList = relMapMap.get(relation["toID"], List())
                            relMapMapList = relMapMapList.push(conn.id);
                            relMapMap = relMapMap.set(relation["toID"], relMapMapList);
                            relationsMap = relationsMap.set(relation["fromID"], relMapMap)

                            let relMapMapReverse = relationsMap.get(relation["toID"], Map());
                            let relMapMapListReverse = relMapMapReverse.get(relation["fromID"], List());

                            if (relMapMapListReverse.size + relMapMapList.size > 1) {
                                let conns = instance.getConnections({ scope: ["relations", "inferred-relations"] }, true)
                                let overlappingRelations = relMapMapList.concat(relMapMapListReverse).toJS();

                                let locationSpace = 1 / (overlappingRelations.length * 2);
                                let newLoc = 0.4 - ((locationSpace * overlappingRelations.length) / 3);
                                let idx = 1;
                                overlappingRelations.map((connId) => {
                                    conns.forEach((eachConn) => {
                                        if (eachConn.id !== connId) return;

                                        let connLabel = eachConn.getOverlay("label");
                                        let location = connLabel.getLocation();
                                        connLabel.setLocation(newLoc * idx);
                                        idx += 1;
                                    })
                                });
                            }

                            let relationDeleting = relation["deleting"]; //is relation currently deleting? (i.e. request sent)
                            let spinner = conn.getOverlay(spinnerId);

                            if (relationDeleting) {
                                spinner && spinner.show(); //show spinner if relation is deleting
                            } else {
                                spinner && spinner.hide(); //otherwise hide it
                            }

                            let assetFrom = self.props.model["assets"].find((mAsset) => mAsset["id"] === relation["fromID"]);
                            let assetTo = self.props.model["assets"].find((mAsset) => mAsset["id"] === relation["toID"]);
                            let paletteLink = self.getPaletteLink(assetFrom["type"], relation["type"]);
                            let relationComment = paletteLink ? paletteLink["comment"] : "";

                            if (!relationDeleting) {
                                conn.bind("mouseover", (conn, event) => {
                                    //console.log("conn mouseover: ", conn);
                                    this.handleConnMouseOver(conn, relation, labelClass, relationComment, assetFrom, assetTo);
                                });
                                conn.bind("mouseout", (conn, event) => {
                                    let relatedTarget = event.relatedTarget;
                                    //console.log("conn mouseout: ", relatedTarget, relatedTarget.nodeName);
                                    if (relatedTarget.className === "tile-canvas" || 
                                            relatedTarget.className === "react-draggable" ||
                                            relatedTarget.nodeName === "svg"
                                        ) {
                                        this.handleConnMouseOut(conn);
                                    }
                                });
                            }

                            if (!relationDeleting) conn.bind("contextmenu", (conn, event) => {
                                event.preventDefault();
                                setTimeout(() => {
                                    this.handleMouseOver(event)
                                }, 52)

                                self.handleContextMenu(conn, event, relation, relationComment,
                                    self.props.model.id, assetFrom, assetTo);
                            })
                            if (!relationDeleting && inferredAssets.length > 0) conn.bind("click", (conn, event) => {
                                this.handleRelationClick(conn, event, inferredAssets);
                            });
                        }
                    }
                }
            })
        });
    }
    
    
    handleConnMouseOut(conn) {
        //console.log("handleConnMouseOut: ", conn);
        this.unhoverConn();
    }
    
    handleConnMouseOver(conn, relation, labelClass, relationComment, assetFrom, assetTo) {
        //console.log("handleConnMouseOver: ", conn);
        let instance = getPlumbingInstance();
        
        if (draggingConnection) return;
        conn = conn.component || conn;

        if (linkHover) {
            this.unhoverConn();
        }

        setLinkHover(true)

        if (hoveredAsset && outerOver) {
            console.log("t1: hideGlyphs 1");
            let assetRef = this.getAssetRef(hoveredAsset);
            if (assetRef) assetRef.hideGlyphs();
            //console.log("setHoveredAsset 1");
            setHoveredAsset("");
        }

        if (!draggingConnection) {
            _delEndpoints();
            setDragDotOver(false);
            setOuterOver(false);
        }

        instance.batch(() => {
            let connLabel = conn.getOverlay("label");
            let labelLoc = connLabel.getLocation();
            let connEl = connLabel.getElement();
            let endpoints = conn.endpoints;
            //console.log("conn endpoints: ", endpoints);
            let sourceEp = endpoints[0];
            let targetEp = endpoints[1];
            //console.log("conn sourceEp: ", sourceEp);
            //console.log("conn targetEp: ", targetEp);
            //console.log("conn sourceEp.element.className: ", sourceEp.element.className);
            //console.log("conn targetEp.element.className: ", targetEp.element.className);

            if (sourceEp.element.className.includes("jtk-group-collapsed") ||
                   targetEp.element.className.includes("jtk-group-collapsed")) {
                //console.log("handleConnMouseOver: source or target is a collapsed group");
               
                let currType = conn.getType();
                let originalType = [...currType];
                conn.setType("hover");
                
                //ideally, we would set the connection scope here, however the following doesn't currently work
                //conn.applyType({scope: "hovers"});

                var hoveredConn = {
                    conn:  conn,
                    labelEl: connEl,
                    originalType: originalType
                };

                this.hoveredLink = hoveredConn;
            }
            else {
                this.addHoveredConn(conn, relation, labelClass, relationComment, assetFrom, assetTo);
            }

        });
    }
    
    addHoveredConn(conn, relation, labelClass, relationComment, assetFrom, assetTo) {
        var self = this;
        let connLabel = conn.getOverlay("label");
        let labelLoc = connLabel.getLocation();
            
        let newConn = addConn(relation, labelClass,
            "hover", "hovers", labelLoc);
            
        var inferredAssets = relation["inferredAssets"];
        //console.log("newConn:", newConn);

        newConn.bind("contextmenu", (conn, event) => {
            event.preventDefault();
            setTimeout(() => {
                this.handleMouseOver(event)
            }, 51)
            self.handleContextMenu(conn, event, relation, relationComment,
                self.props.model.id, assetFrom, assetTo);
        });
        newConn.bind("mouseover", (conn) => {
            //console.log("conn mouseover: setTimeout: t3", conn);
            //setTimeout(() => {
                //KEM - this seems to cause asset not to be expanded when it should
                if (hoveredAsset && outerOver) {
                    //console.log("t3: hoveredAsset: ", hoveredAsset, "outerOver: ", outerOver);
                    //console.log("t3: coon mouseover hideGlyphs 2");
                    let assetRef = this.getAssetRef(hoveredAsset);
                    if (assetRef) assetRef.hideGlyphs();
                    //console.log("t3: setHoveredAsset 2");
                    setHoveredAsset("");
                }

                if (!draggingConnection) {
                    _delEndpoints();
                    setDragDotOver(false);
                    setOuterOver(false);
                }
                //console.log("t3: done");
            //}, 2)
        });
        newConn.bind("click", (connEl, event) => {
            this.handleRelationClick(conn, event, inferredAssets);
        });
    }
    
    unhoverConn() {
        //console.log("unhoverConn: this.hoveredLink = ", this.hoveredLink);
        getPlumbingInstance().select({scope: "hovers"}).delete();
        if (this.hoveredLink) {
            let hoveredConn = this.hoveredLink;
            let conn = hoveredConn.conn;
            let originalType = hoveredConn.originalType;
            conn.setType(originalType.join(" "));
            this.hoveredLink = null;
        }
        setLinkHover(false);
    }
    
    collapseGroup(groupId) {
        let tileId = "#tile-" + groupId;
        $(tileId).css('height', ''); //reset height prior to collapsing
        
        //console.log("Group: calling collapseGroup for " + groupId);
        this.jsplumb.collapseGroup(groupId);
        this.hideGroupConnectionSpinners(groupId);        
    }

    expandGroup(groupId) {
        //console.log("Group: calling expandGroup for " + groupId);
        this.jsplumb.expandGroup(groupId);
        this.hideGroupConnectionSpinners(groupId);
    }
    
    hideGroupConnectionSpinners(groupId) {
        let jsGroup = this.jsplumb.getGroup(groupId);
        //console.log("group connections:", jsGroup.connections);
        
        //First get an array of all group connections, by concatenating all source and target connections
        let connections = jsGroup.connections.source.concat(jsGroup.connections.target);
        
        //Loop through connections, locate any spinner overlays and hide them
        //(the jsplumb.collapseGroup above shows any overlays by default)
        connections.map((c) => {
            let overlayKeys = Object.keys(c.getOverlays());
            overlayKeys.map((key) => {
                if (key.startsWith("spinner")) {
                    //TODO: may need to check for case that connection (relation)
                    //is currently being deleted, as group is collapsed, in which
                    //case, keep the spinner overlay shown
                    c.hideOverlay(key);
                }
            });
        });
    }

    /**
     * Adjustments
     */

    /**
     * When a tile is clicked we want to update the UI.
     *
     * @param {type} event the event that triggered the call of this method
     */
    handleClick(event, diffX, diffY) {
        //console.log("Canvas.handleClick");
        //console.log("Canvas.handleClick: closest tile length: " + $(event.target).closest(".tile").length);
        //console.log("Canvas.handleClick: selected asset = " + this.props.selectedAsset["id"]);

        //console.log( $(event.target) );
        var nodeName = $(event.target)[0].nodeName;
        //console.log(nodeName);
        if (nodeName.toLowerCase() === "path") {
            //console.log("path detected")
        }
        else if ((Math.abs(diffX) < (5 * this.zoom) && Math.abs(diffY) < (5 * this.zoom))) {
            //console.log("Canvas.handleClick: deselecting tile");
            this.clearAssetSelection();

            $(ReactDOM.findDOMNode(this)).blur();
            //console.log("Canvas.handleClick: changeSelectedAsset");
            //console.log("canvas clicked: selected asset: ", this.props.selectedAsset);
            //Only dispatch changeSelectedAsset if it has really changed
            if (this.props.selectedAsset.id !== "") {
                this.props.dispatch(changeSelectedAsset(""));
            }
            //Stop connection if a connection is currently happening
            if(getStartElementId()!= null) {
                const selectionDomNode = document.getElementById("conn-selection-menu");
                if (selectionDomNode !== null) {
                    ReactDOM.unmountComponentAtNode(selectionDomNode);
                    $(selectionDomNode).remove();
                }
                stopConnection();
            }
        }
        if (!this.state.suppressCanvasRefresh && this.transformOrigin[0] > window.outerWidth / 2 &&
                this.transformOrigin[1] > window.innerHeight / 2) {
            this.setState({
                transformOrigin: this.transformOrigin,
                suppressCanvasRefresh: false
            })
        }

    }

    clearAssetSelection() {
        //console.log("Canvas: clearAssetSelection");
        $(".tile").removeClass("active-tile");
        assetSelection.map(item => {getPlumbingInstance().removeFromPosse(item, "asset-selection")})
        assetSelection.splice(0, assetSelection.length);
    }

    handleMouseDown(event) {
        if (event.button == 2) return;
        let target = event.target;
        // console.log("Canvas.handleMouseDown: ", event.target, event.target.id);
        let className = target.className;
        let includesExists = typeof className.includes !== 'undefined';

        if (className === "text-primary title-change") return;
        else if (includesExists && className.includes("add-connection")) return;
        else if (includesExists && className.includes("delete-asserted-asset")) return;
        else if (includesExists && className.includes("cancel-connection")) return;
        else if (includesExists && className.includes("complete-connection")) return;
        else if (target.id !== "tile-canvas") return;

        this.mouseDown = true;
        windowHeight = window.innerHeight;
        windowWidth = window.outerWidth;

        this.startX = event.clientX;
        this.startY = event.clientY;

        this.el.style.cursor = 'grabbing';

        if (target.id === "canvas-container") {
            this.scrollLeft = $("#canvas-container").prop('scrollLeft');
            this.scrollTop = $("#canvas-container").prop('scrollTop');
        }
    }

    reCentreModel() {
        if (this.props.model.assets && !this.props.model.assets.length) return;
        if (this.recentring) return;
        this.recentring = true;
        getPlumbingInstance().setSuspendDrawing(true);
        this.setState({
            ...this.state,
            suppressCanvasRefresh: true
        });

        windowHeight = window.innerHeight;
        windowWidth = window.outerWidth;

        let asset_xs = Object.keys(assetPosse).map(assetId => { return assetPosse[assetId][0] });
        let new_x = ((Math.max(...asset_xs) - Math.min(...asset_xs)) / 2) + Math.min(...asset_xs);
        // let sidePanels = $(".ssm-sliding-panel-container")
        let leftOffset = this.props.view.leftSidePanelWidth;
        let rightOffset = this.props.view.rightSidePanelWidth;

        let diffX = 5000 - new_x + ((windowWidth - window.screen.width + leftOffset - rightOffset) / 2);

        let asset_ys = Object.keys(assetPosse).map(assetId => { return assetPosse[assetId][1] });
        let new_y = ((Math.max(...asset_ys) - Math.min(...asset_ys)) / 2) + Math.min(...asset_ys);
        let diffY = 5000 - new_y - ((window.screen.height - windowHeight) / 2);

        let newAssets = [];
        let newAssetDTOs = [];
        Object.keys(assetPosse).map((assetId) => {
            let thisAsset = this.props.model.assets.filter((asset) => {
                return asset.id === assetId;
            })[0]

            let newAssetDTO = {
                uri: thisAsset.uri,
                iconX: thisAsset.iconX + diffX,
                iconY: thisAsset.iconY + diffY
            }

            newAssetDTOs.push(newAssetDTO);
            assetPosse[thisAsset.id] = [newAssetDTO.iconX, newAssetDTO.iconY];

        })
        this.props.dispatch(relocateAssets(this.props.model.id, newAssetDTOs));

        setTimeout(() => {
            this.reCentreCanvas();
            // deselect assets
            assetSelection.map(item => {getPlumbingInstance().removeFromPosse(item, "asset-selection")})
        }, 200)
        setTimeout(() => {
            getPlumbingInstance().setSuspendDrawing(false, true);
            getPlumbingInstance().revalidate();
            this.props.dispatch(redrawRelations());
            // reselect assets
            assetSelection.map(item => {getPlumbingInstance().addToPosse(item, "asset-selection")})
            this.recentring = false;
            }, 800)
    }

    reCentreCanvas() {
        this.transformOrigin = [5000, 5000];
        windowHeight = window.innerHeight;
        windowWidth = window.outerWidth;

        $("#canvas-container").animate({
            scrollTop: 5000 - windowHeight / 2,
            scrollLeft: 5000 - windowWidth / 2
        }, 200);

        this.updateViewBoundary();

        this.setState({
            transformOrigin: [5000, 5000],
            suppressCanvasRefresh: false,
        });
    }

    handleMouseUp(event) {
        //console.log("Canvas handleMouseUp event: ", event);
        this.mouseDown = false;

        this.updateViewBoundary();

        if (event.button == 2) return;
        let target = event.target;
        //console.log("Canvas.handleMouseUp target: ", target);

        if (target.className === "text-primary title-change") return;
        else if (target.id !== "tile-canvas") return;

        this.el.style.cursor = 'grab';

        const diffX = this.startX - event.clientX;
        const diffY = this.startY - event.clientY;
        // console.log("Canvas calling handleClick");
        this.handleClick(event, diffX, diffY);
    }

    handleMouseMove(event) {
        //console.log("Canvas.handleMouseMove: ", event);
        this.tox_delta = 0;
        this.toy_delta = 0;

        if (!this.state.suppressCanvasRefresh && this.mouseDown &&
                (event.movementX !== 0 || event.movementY !== 0) ) {
            //console.log("Canvas.handleMouseMove: ", event.movementX, event.movementY);
            this.tox_delta = -event.movementX * (1 / getZoom());
            this.toy_delta = -event.movementY * (1 / getZoom());

            let to = this.transformOrigin;
            let tox = to[0] + this.tox_delta;
            let toy = to[1] + this.toy_delta;
            let transformOrigin = [tox, toy];

            if(!(transformOrigin[0] > windowWidth / 2 && transformOrigin[1] > windowHeight / 2)){
                return;
            }

            //console.log("this.props.canvas.zoom: ", this.props.canvas.zoom);
            //console.log("transformOrigin: ", transformOrigin);

            this.transformOrigin = transformOrigin;
            //this.props.dispatch(setTransformOrigin(transformOrigin));

            // setZoom(1)

            var scrollLeft = tox - windowWidth / 2;
            var scrollTop = toy - windowHeight / 2;

            document.getElementById("canvas-container").scrollTo(scrollLeft, scrollTop);
        }
    }

    handleMouseOver(event) {
        //console.log("Canvas: handleMouseOver: ", event);
        let fromEl = event.fromElement;
        let toEl = event.target;
        
        //console.log("Canvas: handleMouseOver: from ", fromEl, " to ", event.target);
        //console.log("Canvas: handleMouseOver: from " + fromEl.nodeName + " \"" + fromEl.className + "\" to " + 
        //        toEl.nodeName + " \"" + toEl.className + "\"");
        //console.log("Canvas: handleMouseOver: from style", fromEl.style);
        
        //TODO: following may not be required as now handled in Group handlePointerOver
        // if (fromEl && fromEl.style && fromEl.style.cursor.includes("resize")) {
        //     console.log("Canvas: handleMouseOver: moving off group resize element");
        //     let resizeSpan = fromEl.parentNode;
        //     console.log("Canvas: handleMouseOver: parent span element: ", resizeSpan);
        //     if (resizeSpan) {
        //         let groupEl = resizeSpan.previousSibling;
        //         console.log("Canvas: handleMouseOver: group element: ", groupEl);
        //         if (groupEl) {
        //             console.log("Canvas: handleMouseOver: group element id: ", groupEl.id);
        //             if (groupEl.id.includes("tile-")) {
        //                 let groupId = groupEl.id.replace("tile-", "");
        //                 //console.log("Canvas: handleMouseOver: group id: ", groupId);
        //                 //console.log("refs: ", this.refs);
        //                 //let group = this.refs[groupId];
        //                 //console.log("group: ", group);
        //                 console.log("Setting resizable to false for group " + groupId);
        //                 this.props.dispatch(setGroupResizable(groupId, false));
        //             }
        //         }
        //     }
        // }
        
        if (this.props.sidePanelActivated) {
            //console.log("dispatch sidePanelDeactivated");
            this.props.dispatch(sidePanelDeactivated());
        }
        else {
            //console.log("(side panel already deactivated)");
        }
        
        //KEM: what is hoverCapture used for?
        if (hoverCapture) {
            hoverCapture.releasePointerCapture(hoverCaptureId)
            hoverCapture = null;
            hoverCaptureId = null;
            //console.log("hover capture is released from asset");
        }
        
        if (hoveredAsset && event.target.id) {
            //console.log("hoveredAsset: ", hoveredAsset, " event.target.id: ", event.target.id);
            if (!event.target.id.includes(hoveredAsset)) {
                //console.log("hovered asset id changed");
            }
        }

        if (event.target.id !== "tile-canvas" && !event.target.id.includes("group")) {
            //console.log("not tile-canvas or group event: returning..");
            return;
        }
        else if (this.mouseDown) {
            //console.log("this.mouseDown: returning..");
            return;
        }
        //setTimeout(() => {
            if (linkHover) {
                //getPlumbingInstance().select({scope: "hovers"}).delete();
                //this.hoveredLink = null;
                this.unhoverConn();
            }
        //}, 1);

        if (hoveredAsset && outerOver) {
            //console.log("t2: handleMouseOver: calling hideGlyphs");
            let assetRef = this.getAssetRef(hoveredAsset);
            if (assetRef) assetRef.hideGlyphs();
            //console.log("setHoveredAsset 3");
            setHoveredAsset("");
        }
        //else {
        //    console.log("handleMouseOver: NOT calling hideGlyphs");                
        //}

        if (!draggingConnection) {
            _delEndpoints();
            setDragDotOver(false);
            setOuterOver(false);
        }
    }

    handleScroll(event) {
        if (this.mouseDown) {
            return;
        }
        let newScrollLeft = event.target.scrollLeft;
        let newScrollTop = event.target.scrollTop;

        //jh17: Added scaling with canvas zoom
        let deltax = this.scrollLeft + ((newScrollLeft - this.scrollLeft)  * (1 / getZoom()));
        let deltay = this.scrollTop + ((newScrollTop - this.scrollTop)  * (1 / getZoom()));

        //console.log("scrollLeft (old, new):", this.scrollLeft, newScrollLeft);
        //console.log("scrollTop (old, new):", this.scrollTop, newScrollTop);

        windowHeight = window.innerHeight;
        windowWidth = window.outerWidth;

        let tox = deltax + windowWidth / 2;
        let toy = deltay + windowHeight / 2;
        let transformOrigin = [tox, toy];
        this.transformOrigin = transformOrigin;

        this.updateViewBoundary();

        this.setState({
            ...this.state,
            transformOrigin: transformOrigin
        });

        this.scrollLeft = newScrollLeft;
        this.scrollTop = newScrollTop;
    }

    handleKeyDown(event) {

        if (event.ctrlKey && event.key === "#") {
            // tell the Modeller to toggle dev mode
            this.props.dispatch(toggleDeveloperMode());
        }

        else if (event.ctrlKey && event.target.id === "jsPlumb_2_1") {
            if (event.key === "a" ) {
                event.preventDefault();
                assetSelection.splice(0, assetSelection.length);
                this.props.model.assets.map(asset => {
                    if (asset.asserted) {
                        assetSelection.push("tile-" + asset.id);
                        getPlumbingInstance().addToPosse("tile-" + asset.id, "asset-selection");
                    }
                })

                $(".tile").addClass("active-tile");
            }
            else if (event.key === "c") {
                this.props.dispatch(clearClipboard());
                assetSelection.map(assetTileId => {
                    let asset = _.find(this.props.model.assets,
                        ['id', assetTileId.replace("tile-", "")])
                    if (asset) {
                        this.props.dispatch(addAsset(asset, true));
                    }
                });
                assetSelection.map(assetTileId => {
                    let asset = _.find(this.props.model.assets,
                        ['id', assetTileId.replace("tile-", "")])
                    if (asset) {
                        assetSelection.map(assetTileId_2 => {
                            if (assetTileId === assetTileId_2) return;
                            let asset_2 = _.find(this.props.model.assets,
                                ['id', assetTileId_2.replace("tile-", "")])
                            if (asset_2) {
                                let relations = _.filter(this.props.model.relations,
                                    {
                                        'fromID': asset.id,
                                        'toID': asset_2.id,
                                        'asserted': true
                                    }
                                );
                                relations.map((relation) => {
                                    // add these for debugging if necessary
                                    // relation['fromAssetLabel'] = asset.label;
                                    // relation['toAssetLabel'] = asset_2.label;
                                    this.props.dispatch(addRelation(relation, true));
                                })
                            }
                        });
                    }
                })
            }
            else if (event.key === "v") {
                var loaders = {};
                var newAssetMap = {};
                this.props.clipboard.map((action, index) => {
                    let loaderFunc;
                    if (action.action === "addAsset") {
                        let newAssetName = action.name + "_" + Math.random().toString(36).substring(2, 7);
                        newAssetMap[action.id] = newAssetName;
                        let asset = {
                            "label": newAssetName,
                            "type": action.type,
                            "asserted": true,
                            "visible": true,
                            "iconX": action.iconX + 60,
                            "iconY": action.iconY - 60,
                            "population": action.population
                        };
                        loaderFunc = () => {
                            return postAssertedAsset(
                                this.props.model.id,
                                asset,
                                action.iconX + 60,
                                action.iconY - 60
                            )
                        }
                        loaders[index] = setInterval((loaderFunc) => {
                            if (!this.props.loading.newFact.length) {
                                this.props.dispatch(
                                    loaderFunc()
                                );
                                clearInterval(loaders[index]);
                                delete loaders[index];
                            }
                        }, 100, loaderFunc)
                    } else if (action.action === "addRelation") {
                        loaderFunc = (fromAsset, toAsset, relType) => {
                            return postAssertedRelation(
                                this.props.model.id,
                                fromAsset[0].id,
                                toAsset[0].id,
                                relType
                            )
                        }

                        loaders[index] = setInterval((loaderFunc) => {
                            let c = 0;
                            if (!this.props.loading.newFact.length) {
                                let fromAsset = _.filter(this.props.model.assets,
                                    ['label', newAssetMap[action.fromID]])
                                let toAsset = _.filter(this.props.model.assets,
                                    ['label', newAssetMap[action.toID]])
                                let relType = {
                                    "label": action.label,
                                    "type": action.type
                                }

                                if (!fromAsset.length || !toAsset.length) {
                                    c += 1;
                                    if (c == 600) {
                                        console.log("could not find assets to create relation");
                                        clearInterval(loaders[index]);
                                        delete loaders[index];
                                        return;
                                    }
                                }

                                this.props.dispatch(
                                    loaderFunc(fromAsset, toAsset, relType)
                                );
                                clearInterval(loaders[index]);
                                delete loaders[index];
                            }
                        }, 100, loaderFunc)
                    }
                })
                this.props.dispatch(clearClipboard());
            }
        } else if (!event.shiftKey && !event.altKey && event.target.id === "jsPlumb_2_1") {
            if (event.key === "Delete") {
                //console.log("Delete key down on Canvas");
                assetSelection.map(item => {
                    let assetId = item.replace("tile-", "");
                    getPlumbingInstance().removeFromPosse(item, "asset-selection");
                    let assetRef = this.getAssetRef(assetId);
                    if (assetRef) assetRef.handleDelete();
                });
                assetSelection.splice(0, assetSelection.length);
            }
        }
    }

    handleKeyUp(event) {
        if (event.key === "Escape") {
            //console.log("Canvas handleKeyUp: Escape");
            //deselect selection
            this.clearAssetSelection();
            this.props.dispatch(changeSelectedAsset(""))
            if (document.getElementsByClassName("valid-targets").length > 0) stopConnection();
        }
    }

    handleAssetDrag(event, newX, newY, tileId, trigAssetId) {
        // console.log("Canvas handleAssetDrag: ", event, newX, newY, tileId, trigAssetId);
        //console.log("componentDidMount: loading: " + this.state.loading);

        let assetId = tileId.replace("tile-", "");
        let assetElId = "tile-" + assetId;
        if (this.props.grouping.inProgress) {
            console.log("Canvas handleAssetDrag: ", assetId, assetElId);
            console.log("Grouping currently in progress - assuming asset position already updated..");
            event.stopPropagation();
            event.preventDefault();
            return;
        }
        else {
            let jsGrp = this.jsplumb.getGroupFor(assetElId);
            // console.log("Canvas handleAssetDrag: ", assetId, assetElId, jsGrp);
            if (jsGrp) {
                let assetGrp = this.getAssetGroup(assetId);
                if (assetGrp) {
                    // console.log("Canvas handleAssetDrag: assetGrp: ", assetGrp);
                    if (newX < 0 || newX > assetGrp.width || newY < 0 || newY > assetGrp.height) { //TODO: get the group width/height, remove hardwired values
                        // console.log("Canvas handleAssetDrag: asset moving out of group - will delegate handing of new asset position to jsPlumb");
                        return;
                    }
                    else {
                        // console.log("Canvas handleAssetDrag: new asset location still within group - will update asset location..");
                    }
                }
                else {
                    console.warn("Cannnot locate asset group for id: " + assetId);
                }
            }
        }

        let diffX = newX - assetPosse[trigAssetId][0];
        let diffY = newY - assetPosse[trigAssetId][1];
        
        // console.log("Canvas handleAssetDrag: trigAssetId, assetId: ", trigAssetId, assetId);

        if (trigAssetId !== assetId) {
            // console.log("Canvas handleAssetDrag: trigAssetId !== assetId (returning)");
            return;
        }

        if (this.state.loading) {
            // console.log("Canvas handleAssetDrag: asset location change ignored (asset deleting)");
        } else if (assetSelection.length > 1 && assetSelection.includes(assetElId) &&
                !(Math.abs(diffX) < (5 * this.zoom) && Math.abs(diffY) < (5 * this.zoom))) {
            // console.log("Canvas handleAssetDrag: handling multiple selected assets");
            let newAssets = [];
            const endpoints = getPlumbingInstance().select({scope: "connection-source"});
            endpoints.each((ep) => { ep.hide() });
            assetSelection.map((selAssetId) => {
                let thisAsset = this.props.model.assets.filter((asset) => {
                    return asset.id === selAssetId.replace("tile-", "");
                })[0]
                let newAsset = {
                    uri: thisAsset.uri,
                    iconX: thisAsset.iconX + diffX,
                    iconY: thisAsset.iconY + diffY
                }
                newAssets.push(newAsset);
                assetPosse[thisAsset.id] = [newAsset.iconX, newAsset.iconY];

                // refresh the asset overlays window
                let $assetHoverEl = $(`#asset-cap-${thisAsset.id}`);

                let styles = {
                    width: `${130 * this.props.canvas.zoom}px`,
                    height: `${160 * this.props.canvas.zoom}px`,
                    // 130px for asset boundary, 98px for asset
                    left: `${Math.ceil(newAsset.iconX - (((130 * this.props.canvas.zoom) - 98) / 2))}px`,
                    // 160px for asset boundary, 127px for asset
                    top: `${Math.ceil(newAsset.iconY - (((160 * this.props.canvas.zoom) - 127)  / 2))}px`,
                };
                Object.entries(styles).map((entry) => {
                    $assetHoverEl[0].style[entry[0]] = entry[1]
                });
                getPlumbingInstance().revalidate(`#asset-cap-${thisAsset.id}`);
            })
            endpoints.each((ep) => { ep.show() });

            console.log("relocating assets: calling relocateAssets");
            this.recentring = true;
            this.props.dispatch(relocateAssets(this.props.model.id, newAssets));
            setTimeout(() => {this.recentring = false}, 2000)
        } else if (!(Math.abs(diffX) < (5 * this.zoom) && Math.abs(diffY) < (5 * this.zoom))) {
            // console.log("Canvas diffX, diffY = ", diffX, diffY);
            let newAsset = {
                id: assetId,
                iconX: assetPosse[assetId][0] + diffX,
                iconY: assetPosse[assetId][1] + diffY,
            }
            assetPosse[assetId] = [newAsset.iconX, newAsset.iconY];
            // console.log("Canvas updated assetPosse: ", assetPosse[assetId]);
            // console.log("Canvas asset location changed: calling putAssertedAssetRelocate: ", newAsset);
            //setTimeout(() => {
                this.props.dispatch(putAssertedAssetRelocate(this.props.model.id, newAsset));
            //}, 2000);
        }
        else {
            // console.log("Canvas handleAssetDrag (ignoring)");
        }
    }
    
    handleGroupDrag(event, newX, newY, groupId, groupUri) {
        //console.log("Canvas handleGroupDrag: ", event, newX, newY, groupId);
        //console.log("target: " + event.target.localName);
        if (event.target.localName === "span") {
            //console.log("(ignoring drag on span element)");
            return;
        }
        
        let updatedGroup = {
            id: groupId,
            uri: groupUri,
            left: Math.round(newX) + "px",
            top: Math.round(newY) + "px"
        };
        
        console.log("Canvas group location changed: calling putAssertedGroupRelocate: ", updatedGroup);
        this.props.dispatch(putAssertedGroupRelocate(this.props.model.id, updatedGroup));
    }

    handleAssetMouseDown(event, assetId, assetEl) {
        //console.log("handleAssetMouseDown: ", assetId);
        if (event.button == 2) return;
        if (event.target.className === "text-primary title-change") return;
        else if (event.target.className.includes("add-connection")) return;
        else if (event.target.className.includes("delete-asserted-asset")) return;
        else if (event.target.className.includes("cancel-connection")) return;
        else if (event.target.className.includes("complete-connection")) return;

        let $assetEl = $(assetEl);

        this.assetStartX = event.clientX;
        this.assetStartY = event.clientY;

        if (event.ctrlKey) {
            // Control is pressed, so add/remove the elements to the selection list
            let assetPosseId = "tile-" + assetId;

            if (assetSelection.includes(assetPosseId)) {
                //deselecting
                assetSelection.splice(assetSelection.indexOf(assetPosseId), 1);
                getPlumbingInstance().removeFromPosse(assetPosseId, "asset-selection");
                $assetEl.removeClass("active-tile");

                if (assetSelection.length === 0) this.props.dispatch(changeSelectedAsset(""));
            } else {
                //selecting
                assetSelection.push(assetPosseId);
                getPlumbingInstance().addToPosse(assetPosseId, "asset-selection");
                $assetEl.addClass("active-tile");

                if (assetSelection.length === 1) this.props.dispatch(changeSelectedAsset(assetId));
            }
        } else {
            $assetEl.css("cursor", "grabbing");
        }
    }

    handleAssetMouseUp(event, assetId, assetEl, draggedEvent=false) {
        //console.log("handleAssetMouseUp: " + assetId);

        // console.log("canvas asset dragging " + draggingConnection);
        if (draggingConnection) {
            if (!draggedEvent) return;

            if (assetEl.className.includes("valid-targets")) {
                let assetRef = this.getAssetRef(assetId);
                if (assetRef) assetRef.completeConnection(event);
            } else stopConnection();

            return;
        }

        if (event.button === 2) return;
        if (event.target.className === "text-primary title-change") return;
        else if (event.target.className.includes("add-connection")) return;
        else if (event.target.className.includes("delete-asserted-asset")) return;
        else if (event.target.className.includes("cancel-connection")) return;
        else if (event.target.className.includes("complete-connection")) return;

        let $assetEl = $(assetEl);

        let diffX = this.assetStartX - event.clientX;
        let diffY = this.assetStartY - event.clientY;

        if (!event.ctrlKey && (Math.abs(diffX) < (5 * this.zoom) && Math.abs(diffY) < (5 * this.zoom))) {

            assetSelection.map(item => {getPlumbingInstance().removeFromPosse(item, "asset-selection")})
            assetSelection.splice(0, assetSelection.length);

            assetSelection.push("tile-" + assetId);
            getPlumbingInstance().addToPosse("tile-" + assetId, "asset-selection");

            $(".tile").removeClass("active-tile");
            $assetEl.addClass("active-tile");

            if (!this.isSelectedAsset(assetId)) {
                //console.log("handleAssetMouseUp: dispatch changeSelectedAsset");
                this.props.dispatch(changeSelectedAsset(assetId));
            }

            if (assetEl.className.includes("valid-targets")) {
                let assetRef = this.getAssetRef(assetId);
                if (assetRef) assetRef.completeConnection(event);
            } else stopConnection();
        }
        else {
            //console.log("(not changing selection)");
        }

        $assetEl.css("cursor", "pointer");
    }

    handleAssetMouseOver(event, assetId, changingHoveredAsset=false) {
        //console.log("Canvas: handleAssetMouseOver: ", assetId, changingHoveredAsset, event);
        //console.log("Canvas: handleAssetMouseOver: (disabled for now)");
        
        //KEM: ensure that previously hovered asset is shrunk (via hideGlyphs), prior to expanding the new asset
        if (assetId !== hoveredAsset) {
            //console.log("Changing hovered asset from \"" + hoveredAsset + "\" to " + assetId);
            if (hoveredAsset && (hoveredAsset !== "")) {
                // console.log("Calling hideGlyphs on asset: " + hoveredAsset);
                let assetRef = this.getAssetRef(hoveredAsset);
                if (assetRef) assetRef.hideGlyphs();
            }
        }
        
        setTimeout(() => {
            getPlumbingInstance().select({scope: "hovers"}).delete();
            setLinkHover(false);
        }, 3);

        //console.log(hoveredAsset);
        //console.log(assetId);
        //console.log("changingHoveredAsset: ", changingHoveredAsset);
        if (changingHoveredAsset) {
            //console.log("changingHoveredAsset: ", changingHoveredAsset);
            let assetRef = this.getAssetRef(hoveredAsset);
            if (assetRef) {
                //console.log("calling handleMouseOut");
                assetRef.handleMouseOut(changingHoveredAsset);
            }

            setTimeout(() => {
                setOuterOver(false);
                setDragDotOver(false);
                // console.log("setHoveredAsset 4");
                setHoveredAsset(assetId);
                let assetRef = this.getAssetRef(assetId);
                if (assetRef) assetRef.handleMouseOver();
            }, 15)
        }

        if (!hoverCapture) {
            //console.log("hoverCapture: ", hoverCapture);
            //console.log("handle asset mouse out");
            //console.log("event", event);
            if (event !== null) {
                event.stopPropagation();
                event.target.setPointerCapture(event.pointerId)
                hoverCapture = event.target;
                hoverCaptureId = event.pointerId;
            }
            
            //console.log("this.refs: ", this.refs);
            let assetRef = this.getAssetRef(assetId);
            if (assetRef) {
                assetRef.handleMouseOver();
            }
            else {
                // console.warn("NOT Calling handleMouseOver for asset: " + assetId + " (missing ref)");                
            }
            //console.log("setHoveredAsset 5");
            setHoveredAsset(assetId);
        }
    }

    handleAssetMouseOut(event, assetId) {
        //console.log("handleAssetMouseOut: ", assetId);
        setTimeout(() => {
            if (!hoverCapture) {
                let assetRef = this.getAssetRef(assetId);
                if (assetRef) assetRef.handleMouseOut();
            }
        }, 5)
    }

    handleRelationClick(conn, event, inferredAssets) {
        event.stopPropagation();

        conn = conn.component || conn;

        if (inferredAssets.length > 1) { //multiple inferred assets
            if (!document.getElementById("inferred-asset-selection-menu")) {
                var menu = document.createElement("div");
                document.body.appendChild(menu);
                $(menu).attr("id", "inferred-asset-selection-menu");
            }

            ReactDOM.render(<InferredAssetSelectionMenu
                    inferredAssetOptions={this.getInferredAssetOptions(inferredAssets)}
                    selectOption={this.updateMultiChoiceInferredAsset}
                    closeMenu={this.closeInferredAssetsMenu}/>,
                document.getElementById("inferred-asset-selection-menu"));
            var jPopupMenu = $(".popup-menu");
            jPopupMenu.css({
                left: event.pageX - jPopupMenu[0].clientWidth / 2,
                top: event.pageY - jPopupMenu[0].clientHeight / 2
            });
            //Following seems to cause problems on Firefox (cannot select asset properly)
            //jPopupMenu.on("mouseleave", () => this.closeInferredAssetsMenu());
        } else if (inferredAssets.length === 1) { //single inferred asset
            console.log("Selecting single inferred asset:");
            var inferredAsset = inferredAssets[0];
            //console.log(inferredAsset);

            var infA = this.getAssetLabel(inferredAsset);
            //console.log("infA.id: " + infA.id);
            //console.log("selectedAssetID: " + this.props.selectedAsset["id"]);

            if (infA !== null && this.props.selectedAsset["id"] !== infA.id) {
                $("#tile-canvas").find(".active-tile").removeClass("active-tile");
                //console.log("changeSelectedAsset: " + infA.id);
                this.props.dispatch(changeSelectedAsset(infA.id));
            } else if (infA === null) {
                console.log("WARNING: inferred asset (infA) is null - cannot select it");
            }
        }
        // else {
        //     $("#tile-canvas").find(".active-tile").removeClass("active-tile");
        //     if (this.props.selectedAsset["id"] !== "") {
        //         this.props.dispatch(changeSelectedAsset(""));
        //     }
        // }
    }

    /**
     * Methods relating the context menu
     */
    handleContextMenu(conn, event, relation, relationComment, modelId, assetFrom, assetTo, contextTrigger) {
        //console.log("handleContextMenu: conn: ", conn);

        if (!document.getElementById("relation-context-menu")) {
            let menu = document.createElement("div");
            document.body.appendChild(menu);
            $(menu).attr("id", "relation-context-menu");
        }
        
        //console.log("this.getAssetRef:", this.getAssetRef);
        //console.log("relation.fromID: ", relation.fromID);
        //console.log("relation.toID: ", relation.toID);
        //console.log("getAssetRef[relation.fromID]: ", this.getAssetRef(relation.fromID));
        //console.log("getAssetRef[relation.toID]: ", this.getAssetRef(relation.toID));
        
        let fromIDassetRef = this.getAssetRef(relation.fromID);
        let toIDassetRef = this.getAssetRef(relation.toID);

        let changeRelationType = {
            startConnection: fromIDassetRef ? fromIDassetRef.startConnection : null,
            changeRelationType: toIDassetRef ? toIDassetRef.changeRelationType : null
        }
        
        //console.log("handleContextMenu: changeRelationType: ", changeRelationType);

        ReactDOM.render(<RelationCtxMenu
            modelId={ modelId }
            assetFromLabel={ assetFrom.label }
            assetToLabel={ assetTo.label }
            relation={ relation }
            relationComment={ relationComment }
            contextTrigger={ this.contextTrigger }
            changeRelationType={ changeRelationType }
            dispatch={ this.props.dispatch }
            authz={this.props.authz}/>,
            document.getElementById("relation-context-menu"));

        if(this.contextTriggerVar) {
            this.contextTriggerVar.handleContextClick(event);
        }
    }

    updateViewBoundary(leftOffset=this.props.view.leftSidePanelWidth,
                       rightOffset=this.props.view.rightSidePanelWidth) {
        //console.log("Canvas updateViewBoundary");

        // update the "view-boundary" element to restrict window movement.
        let viewWidth = windowWidth + 500;
        let viewHeight = windowHeight - 50;
        let viewXPos = this.transformOrigin[0] - (viewWidth / 2) + 125;
        let viewYPos = this.transformOrigin[1] - (viewHeight / 2) - (50 / 2);

        // account for browser zoom
        viewWidth = viewWidth * currentPixelRatio;
        viewHeight = viewHeight * currentPixelRatio;

        // offset the bottom side so that windows can be dragged under the bottom of the screen
        viewHeight += 800;

        let viewBoundary = document.getElementById("view-boundary");
        
        if (viewBoundary) {
            viewBoundary.style.width = `${viewWidth}px`;
            viewBoundary.style.height = `${viewHeight}px`;
            viewBoundary.style.left = `${viewXPos}px`;
            viewBoundary.style.top = `${viewYPos}px`;
        }
        else {
            console.warn("updateViewBoundary: cannot locate viewBoundary element");
        }
    }

    contextTrigger(c) {
        //console.log("contextTrigger: ", c);
        this.contextTriggerVar = c;
    }

    canvasContextTrigger(c) {
        //console.log("canvasContextTrigger: ", c);
        this.canvasContextTriggerVar = c;
    }
    
    /**
     * Get all assets from which a connection could be made
     *
     * @param {type} assetType the type of the asset for which to check for incoming connections
     * @returns {unresolved} the valid startpoints in the asset model on the canvas
     */
    getValidStartpoints(assetType) {
        var self = this;
        //console.log("Finding valid startpoints for asset type " + assetType);

        var linkTypes = this.props.model["palette"]["links"][assetType];
        //console.log(linkTypes["linksTo"]);

        var validStartpoints = {};
        if (linkTypes === undefined || linkTypes["linksTo"] === undefined) {
            return validStartpoints;
        }

        //iterate over all allowed links
        for (var conn of linkTypes["linksTo"]) {

            //this is a new relationship type
            if (validStartpoints[conn["type"]] === undefined) {
                validStartpoints[conn["type"]] = {label: conn["label"], comment: conn["comment"],
                                                assets: []};

                //check all existing assets to see if they're of the allowed type
                for (var asset of self.props.model["assets"]) {
                    //if they are add them to the list of allowed endpoints
                    if (conn["options"].indexOf(asset["type"]) >= 0) {
                        validStartpoints[conn["type"]]["assets"].push(asset["id"]);
                    }
                }
            } else {
                console.warn("duplicate entry for for connection type " + conn["type"] + ", ignoring");
            }
        }
        //console.log("valid startpoints:");
        //console.log(validStartpoints);
        return validStartpoints;
    }

    /**
     * Get all assets to which a connection could be made
     *
     * @param {type} assetType the type of the asset for which to check for outgoing connections
     * @returns {unresolved} the valid endpoints in the asset model on the canvas
     */
    getValidEndpoints(assetType) {
        var self = this;
        //console.log("Finding valid endpoints for asset type " + assetType);

        var linkTypes = this.props.model["palette"]["links"][assetType];
        //console.log(linkTypes["linksFrom"]);

        var validEndpoints = {};
        if (linkTypes === undefined || linkTypes["linksFrom"] === undefined) {
            return validEndpoints;
        }

        //iterate over all allowed links
        for (var conn of linkTypes["linksFrom"]) {

            //this is a new relationship type
            if (validEndpoints[conn["type"]] === undefined) {
                validEndpoints[conn["type"]] = {label: conn["label"], comment: conn["comment"],
                                                assets: []};

                //check all existing assets to see if they're of the allowed type
                for (var asset of self.props.model["assets"]) {
                    //if they are add them to the list of allowed endpoints
                    if (conn["options"].indexOf(asset["type"]) >= 0) {
                        validEndpoints[conn["type"]]["assets"].push(asset["id"]);
                    }
                }
            } else {
                console.warn("duplicate entry for for connection type " + conn["type"] + ", ignoring");
            }
        }
        //console.log("valid endpoints:");
        //console.log(validEndpoints);
        return validEndpoints;
    }
    
    getPaletteLink(fromAssetType, linkType) {
        //console.log("getPaletteLink from " + fromAssetType + " linkType: " + linkType);
        let fromPaletteAssetType = this.props.getAssetType(fromAssetType);
        //console.log("fromPaletteAssetType:", fromPaletteAssetType);
        let validEndpoints = this.getValidEndpoints(fromPaletteAssetType["id"]);
        let link = validEndpoints[linkType];
        //console.log("link:", link);
        return link;
    }

    getPalette() {
        return this.props.model.palette
    }

    isSelectedAsset(id) {
        let selected = (id === this.props.selectedAsset["id"]);
        //console.log("selected " + id + ": " + selected);
        return selected;
    }
    
    getAssetLabelByID(id) {
        var asset = this.props.model["assets"].find((asset) => id === asset["id"]);
        return asset ? asset["label"] : "";
    }

    getAssetLabel(uri) {
        //console.log("getAssetLabel for asset " + uri);
        var assetLabel = uri; //if name cannot be determined
        var asset = this.props.model["assets"].find((asset) => uri === asset["uri"]);

        if (asset !== null) {
            //console.log("found asset");
            //console.log(asset);
            assetLabel = asset["label"];
        } else {
            console.warn("WARNING: could not locate asset with uri: " + uri);
        }

        var namedAsset = {
            id: asset.id,
            uri: uri,
            label: assetLabel
        };

        //console.log("asset: ");
        //console.log(namedAsset);

        return namedAsset;
    }

    getInferredAssetOptions(inferredAssetURIs) {
        console.log("getInferredAssetOptions for assets:");
        //console.log(inferredAssetIDs);
        //console.log("previously selectedInferredAsset: " + this.props.selectedInferredAsset);
        var options = [];
        inferredAssetURIs.map((assetURI) => {
            var option = this.getAssetLabel(assetURI);
            if (this.props.selectedInferredAsset === option.id) {
                option["selected"] = "selected";
            }
            else {
                option["selected"] = "";
            }
            options.push(option);
        });
        //console.log("inferred asset options:");
        //console.log(options);
        return options;
    }

    updateMultiChoiceInferredAsset(inferredAssetOption) {
        if (this.props.selectedAsset["id"] !== inferredAssetOption) {
            $("#tile-canvas").find(".active-tile").removeClass("active-tile");
            this.props.dispatch(changeSelectedInferredAsset(inferredAssetOption));
            this.props.dispatch(changeSelectedAsset(inferredAssetOption));
        }

        this.closeInferredAssetsMenu();
    }

    closeInferredAssetsMenu() {
        var domNode = document.getElementById("inferred-asset-selection-menu");
        ReactDOM.unmountComponentAtNode(domNode);
        $(domNode).remove();
    }

}

/**
 * This is specifying the prop types. This reduces the manual error checking required.
 */
Canvas.propTypes = {
    model: PropTypes.object.isRequired,
    movedAsset: PropTypes.bool,
    groups: PropTypes.array,
    grouping: PropTypes.object,
    canvas: PropTypes.object,
    loading: PropTypes.object,
    isAssetDisplayed: PropTypes.func,
    isRelationDisplayed: PropTypes.func,
    selectedLayers: PropTypes.array,
    selectedAsset: PropTypes.object,
    selectedThreat: PropTypes.object,
    selectedControlSet: PropTypes.object,
    selectedInferredAsset: PropTypes.string,
    view: PropTypes.object,
    suppressCanvasRefresh: PropTypes.bool,
    sidePanelActivated: PropTypes.bool,
    redrawRelations: PropTypes.number,
    getAssetType: PropTypes.func,
    isAssetNameTaken: PropTypes.func,
    isGroupNameTaken: PropTypes.func,
    dispatch: PropTypes.func.isRequired,
    authz: PropTypes.object,
};

var mapStateToProps = function (state) {
    return {
        suppressCanvasRefresh: state.modeller.suppressCanvasRefresh,
        transformOrigin: state.modeller.canvas.transformOrigin,
        clipboard: state.interaction.clipboard
    }
}

/* Export the canvas as required. */
export default connect(mapStateToProps)(Canvas);
