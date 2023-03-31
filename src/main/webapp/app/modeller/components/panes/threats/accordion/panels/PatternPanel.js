import React from "react";
import PropTypes from 'prop-types';
import {OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../../common/constants.js";
import {getPlumbingInstance, hoveredConns, setHoveredConns} from "../../../../util/TileFactory";
import {
    changeSelectedAsset,
    deleteAssertedRelation,
    hideRelation,
    suppressCanvasRefresh
} from "../../../../../actions/ModellerActions";

class PatternPanel extends React.Component {

    constructor(props) {
        super(props);

        this.isSelectedAsset = this.isSelectedAsset.bind(this);
        this.hoverLink = this.hoverLink.bind(this);
        this.unHoverLink = this.unHoverLink.bind(this);
        this.deleteLink = this.deleteLink.bind(this);
        this.changeSelectedNode = this.changeSelectedNode.bind(this);
        this.changeSelectedAssetByURI = this.changeSelectedAssetByURI.bind(this);
        this.findNodeAsset = this.findNodeAsset.bind(this);
    }

    render() {
        var self = this;
        //console.log("PatternPanel render(). this.props:", this.props);
        //var nodes = this.props.threat.pattern["nodes"];
        let nodes = [];

        if (this.props.getNodes !== undefined) {
            nodes = this.props.getNodes();
        }
        else {
            console.log("WARNING: this.props.getNodes is undefined");
        }

        if (! this.props.threat.pattern) {
            alert("ERROR: threat has no pattern!");
            return null;
        }

        let links = this.props.threat.pattern["links"];
        let patternBroken = this.isPatternBroken();
        //console.log("pattern.label = " + this.props.threat.pattern.label);
        //console.log("patternBroken = " + patternBroken);

        if (nodes === undefined) nodes = [];
        if (links === undefined) links = [];

        let threat = this.props.threat;
        const threatUri = threat !== undefined ? threat["uri"] : "##";
        let threatLabel = threat !== undefined ? threat["label"] : "";

        if (!threatLabel) {
            if (threatUri && threatUri.indexOf("#") > -1) {
                threatLabel = threatUri.split("#")[1];
            }
            else {
                threatLabel = threatUri;
            }
        }

        return (
            <div className="container-fluid">
                <div className="row">
                    <span><strong>{"Threat: "}</strong></span>
                </div>
                
                <div className="row detail-info">
                    <span className="col-xs-12">{threatLabel}</span>
                </div>
                
                <br />
                
                <div className="row">
                    <span><strong>{"Pattern: "}</strong></span>
                </div>
                
                <div className="row detail-info"
                     onMouseEnter={() => this.hoverPattern(true)}
                     onMouseLeave={() => this.hoverPattern(false)}
                >
                    <span className="col-xs-12">{this.props.threat.pattern.label}</span>
                </div>
                
                <br />

                <div className="row">
                    <span><strong>{"Nodes: "}</strong></span>
                </div>
                
                {/* <div className="row">
                 <span className="col-md-6"><strong>{"Node: "}</strong></span>
                 <span className="col-md-6"><strong>{"Role: "}</strong></span>
                 </div> */}
                {nodes.map((node, index) => {
                    //console.log(node);
                    if (! node["visible"])
                        return "";

                    let nodeClass = this.isSelectedAsset(node["assetLabel"])
                        ? "col-xs-6 highlighted text-bold"
                        : "col-xs-6 clickable text-bold";

                    return (
                        <div key={index} className={`row rel-info row-hover`}
                            onMouseEnter={() => this.hoverNode(node, true)}
                            onMouseLeave={() => this.hoverNode(node, false)}
                        >
                            <span style={{cursor: "pointer"}} className={nodeClass}
                                  onClick={() => this.changeSelectedNode(node)}>{node["assetLabel"]}</span>
                            <span className="col-xs-6 text-bold">{node["roleLabel"]}</span>
                        </div>
                    );
                })
                }
                <br />
                {links.length > 0 &&
                <div className="row">
                    <span>
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top" overlay={
                                                    <Tooltip id="pattern-broken-tooltip" className={"tooltip-overlay"}>
                                                        <strong>{ patternBroken ? "Pattern broken! Please revalidate!" :
                                                            "You can break the pattern (and thus disable the threat) by removing any of the " +
                                                            "asserted relations below and revalidating"}
                                                        </strong>
                                                    </Tooltip>
                                                }>
                            <strong>{"Relations: "}</strong>
                        </OverlayTrigger>
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top" overlay={
                            <Tooltip id="pattern-broken-tooltip-2" className={"tooltip-overlay"}>
                                <strong>{ patternBroken ? "Pattern broken! Please revalidate!" :
                                    "You can break the pattern (and thus disable the threat) by removing any of the " +
                                        "asserted relations below and revalidating"}
                                </strong>
                            </Tooltip>
                        }>
                            <span className="fa fa-info-circle"/>
                        </OverlayTrigger>
                    </span>
                </div>}

                {links.length > 0 && links.map((link, index) => {
                    var relExists = this.relationExists(link);

                    //var linkClass = "col-xs-4";
                    var linkClass = "";
                    linkClass += relExists ? "" : " deleted";

                    //var linkClass2 = "col-xs-3";
                    var linkClass2 = "";
                    linkClass2 += relExists ? "" : " deleted";

                    //Hide any links for paths or segments
                    let typeLabel = link["typeLabel"];
                    //console.log(typeLabel);
                    if ( (typeLabel === "startsAt") || (typeLabel === "endsAt") || (typeLabel === "via") || (typeLabel === "forwardPath") )
                        return "";

                    let relation, fromAsset, toAsset;
                    
                    if (relExists) {
                        relation = this.getRelationForLink(link);
                        //console.log("pattern relation: ", relation);
                        fromAsset = this.props.assets.find((mAsset) => mAsset["id"] === relation["fromID"]);
                        toAsset = this.props.assets.find((mAsset) => mAsset["id"] === relation["toID"]);
                    }

                    //N.B. cannot determine if asserted, if relation does not exist.
                    //But then we should not be allowed to delete an inferred relation anyway!
                    let inferred = relExists && !relation["asserted"];
                    let hiddenDiv;
                    if (relExists && fromAsset && fromAsset.asserted
                            && toAsset && toAsset.asserted && relation["hidden"]) {
                        hiddenDiv = <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left" overlay={
                            <Tooltip id={`pattern-hidden-rel-${index + 1}-tooltip`}
                                     className={"tooltip-overlay"}>
                                <strong>{"Click to Unhide"}</strong>
                            </Tooltip>
                        }>
                            <span className="inferred-col fa fa-eye-slash col-xs-1" onClick={(e) => {
                                e.nativeEvent.target.className = "inferred-col fa fa-eye col-xs-1";
                                this.props.dispatch(hideRelation(relation.id));
                            }}
                                  style={{cursor: "pointer"}}/>
                        </OverlayTrigger>
                    } else if (relExists && fromAsset && fromAsset.asserted
                        && toAsset && toAsset.asserted && !relation["hidden"]) {
                        hiddenDiv = <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left" overlay={
                            <Tooltip id={`pattern-hidden-rel-${index + 1}-tooltip`}
                                     className={"tooltip-overlay"}>
                                <strong>{"Click to Hide"}</strong>
                            </Tooltip>
                        }>
                            <span className="inferred-col fa fa-eye col-xs-1" onClick={(e) => {
                                e.nativeEvent.target.className = "inferred-col fa fa-eye-slash col-xs-1";
                                this.props.dispatch(hideRelation(relation.id));
                            }}
                                  style={{cursor: "pointer"}}/>
                        </OverlayTrigger>
                    }
                    //console.log("inferred = " + inferred);

                    let deletable = relExists && this.props.isRelationDeletable(relation);
                    //console.log("deletable = " + deletable);

                    return (
                        <div key={index} className="row rel-info row-hover"
                             onMouseEnter={() => this.hoverLink(link)}
                             onMouseLeave={() => this.unHoverLink(link)}>
                            {hiddenDiv}
                            {inferred ?
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left" overlay={
                                    <Tooltip id={`pattern-inferred-rel-${index + 1}-tooltip`}
                                             className={"tooltip-overlay"}>
                                        <strong>{"Inferred relation"}</strong>
                                    </Tooltip>
                                }>
                                    <span className="inferred-col fa fa-tag col-xs-1"
                                          style={{left: "5px"}}/>
                                </OverlayTrigger> : <span className="inferred-col col-xs-1"
                                                          style={{left: "5px"}}/>}
                        <span className="rel col-xs-10">
                            <span style={{cursor: "pointer"}}
                                  onClick={() => this.changeSelectedAssetByURI(link["fromAsset"])}
                                className={
                                    this.isSelectedAsset(link["fromAssetLabel"]) ?
                                        linkClass + " highlighted text-bold" :
                                        linkClass + " clickable text-bold"
                                }>
                                {link["fromAssetLabel"]}
                            </span>
                            <span className={linkClass2} style={{padding: 0}}><strong>{link["typeLabel"]}</strong></span>
                            <span style={{cursor: "pointer"}}
                                  onClick={() => this.changeSelectedAssetByURI(link["toAsset"])}
                                className={
                                    this.isSelectedAsset(link["toAssetLabel"]) ?
                                        linkClass + " highlighted text-bold" :
                                        linkClass + " clickable text-bold"
                                }>
                                {link["toAssetLabel"]}</span>
                        </span>
                            {deletable && this.props.authz.userEdit ?
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="right"
                                                overlay={
                                                    <Tooltip id={`pattern-del-rel-${index + 1}-tooltip`}
                                                             className={"tooltip-overlay"}>
                                                        <strong>Delete relation</strong>
                                                    </Tooltip>
                                                }>
                                <span className="menu-close fa fa-trash col-xs-1"
                                      onClick={(e) => {
                                          e.nativeEvent.target.className = "fa fa-cog fa-fw fa-spin col-xs-1";
                                          this.deleteLink(link)
                                      }}/>
                                </OverlayTrigger> : <span className="col-xs-1"/>}
                        </div>
                    );
                })
                }
            </div>
        );
    }

    unhighlightNodes() {
        $("#tile-canvas").find(".tile").removeClass("pattern-hover");
        $("#tile-canvas").find("div.tile.role-tile").hide();
    }

    changeSelectedAssetByURI(uri) {
        //console.log(uri);
        let a = this.props.assets.find(function (a) {
            return a.uri===uri;
        });
        //console.log(a);
        if (a !== undefined) {
            this.props.dispatch(changeSelectedAsset(a.id));
        }
    }

    changeSelectedNode(n) {
        let a = this.findNodeAsset(n);
        if (a !== undefined) {
            this.props.dispatch(changeSelectedAsset(a.id));
        }
    }

    findNodeAsset(n) {
        let a = this.props.assets.find(function (a) {
            return a.uri===n.asset;
        });
        //console.log(a);
        return a;
    }

    highlightNode(n, show) {
        let a = this.findNodeAsset(n);

        //add or remove class
        if (show===true && a) {
            var assetTile = $("#tile-canvas").find("#tile-" + a.id).first();
            assetTile.addClass("pattern-hover");
            var roleTile = assetTile.find("div.tile.role-tile").first();
            roleTile.find("label").text(n.roleLabel);
            roleTile.show();
        }
    }

    hoverPattern(show) {
        //TODO: The following code is almost identical to that in hoverThreat in Modeller.js
        //so common functionality should be factored out somehow. 
        //this.props.selectedThreatVisibility = show;
        var threat = this.props.threat;

        if (threat.pattern.nodes !== undefined) {

            //first unhighlight asset tiles and hide role tiles
            this.unhighlightNodes();

            //highlight each node
            threat.pattern.nodes.map((n) => {
                this.highlightNode(n, show);
            });

            if (show === true) {
                //highlight all links
                let hConns = [];
                threat.pattern.links.map((link) => {
                    var assetFrom = this.props.assets.find((mAsset) => mAsset["uri"] === link["fromAsset"]);
                    var assetTo = this.props.assets.find((mAsset) => mAsset["uri"] === link["toAsset"]);
                    if (!assetFrom || !assetTo) return;
                    getPlumbingInstance().select(
                        {
                            source: "tile-" + assetFrom["id"],
                            target: "tile-" + assetTo["id"],
                            scope: ["relations", "inferred-relations"]
                        }, true
                    ).each((conn) => {
                        let relation = {
                            label: link["typeLabel"],
                            fromID: assetFrom["id"],
                            toID: assetTo["id"]
                        };
                        let connLabel = conn.getOverlay("label");
                        let labelLoc = connLabel.getLocation();
                        let connEl = connLabel.getElement();
                        if (connEl.innerHTML === relation["label"]) {
                            connEl.className += ' pattern-hover';
                            let currType = conn.getType();
                            let originalType = [...currType];
                            conn.setType("pattern");

                            var hoveredConn = {
                                conn:  conn,
                                labelEl: connEl,
                                originalType: originalType
                            };

                            hConns.push(hoveredConn);
                        }
                    });
                });
                setHoveredConns(hConns);
            }
            else {
                //console.log("Resetting hoveredConns: ", hoveredConns);
                hoveredConns.map((hoveredConn) => {
                    let conn = hoveredConn.conn;
                    let labelEl = hoveredConn.labelEl;
                    let originalType = hoveredConn.originalType;
                    labelEl.classList.remove("pattern-hover");
                    conn.setType(originalType.join(" "));
                });

                //Finally, clear the list of hovered conns
                setHoveredConns([]);
            }
        }
    }

    hoverNode(node, show) {
        this.unhighlightNodes();
        this.highlightNode(node, show);
    }

    isSelectedAsset(label) {
        return this.props.asset && this.props.asset["label"] === label;
    }

    hoverLink(link) {
        var fromAssetUrl = link.fromAsset;
        var toAssetUrl = link.toAsset;

        var assetFrom = this.props.assets.find((mAsset) => mAsset["uri"] === fromAssetUrl);
        var assetTo = this.props.assets.find((mAsset) => mAsset["uri"] === toAssetUrl);

        if (assetFrom === undefined || assetTo === undefined) {
            return;
        }

        getPlumbingInstance().select(
            {
                source: "tile-" + assetFrom["id"],
                target: "tile-" + assetTo["id"],
                scope: ["relations", "inferred-relations"]
            }, true
        ).each((conn) => {
            let relation = {
                label: link["typeLabel"],
                fromID: assetFrom["id"],
                toID: assetTo["id"]
            };

            let connLabel = conn.getOverlay("label");
            let labelLoc = connLabel.getLocation();
            let connEl = connLabel.getElement();
            if (connEl.innerHTML === relation["label"]) {
                let currType = conn.getType();
                let originalType = [...currType];
                conn.setType("hover");
                
                var hoveredConn = {
                    conn:  conn,
                    labelEl: connEl,
                    originalType: originalType
                };
                
                this.hoveredLink = hoveredConn;
            }
        });

        $("#tile-" + assetFrom["id"]).addClass("hover");
        $("#tile-" + assetTo["id"]).addClass("hover");
    }

    unHoverLink() {
        if (this.hoveredLink) {
            let hoveredConn = this.hoveredLink;
            let conn = hoveredConn.conn;
            let originalType = hoveredConn.originalType;
            conn.setType(originalType.join(" "));
            this.hoveredLink = null;
        }
        
        $(".tile").removeClass("hover");
    }

    isPatternBroken() {
        var patternBroken = false;

        if (this.props === undefined) {
            console.log("isPatternBroken: this.props is undefined");
            return false;
        }

        if (this.props.threat === undefined) {
            console.log("isPatternBroken: this.props.threat is undefined");
            return false;
        }

        var links = this.props.threat.pattern["links"];
        if (links === undefined) links = [];
        links.map((link) => {
            var relExists = this.relationExists(link);
            if (! relExists) {
                patternBroken = true;
            }
        });

        return patternBroken;
    }

    relationExists(link) {
        //console.log("Checking if relation exists for link:");
        //console.log(link);
        var relation = this.getRelationForLink(link);
        return (relation !== undefined);
    }

    getRelationForLink(link) {
        if (this.props === undefined) {
            console.log("getRelationForLink: this.props is undefined");
            return undefined;
        } else if (!link) {
            return undefined;
        }
        return this.props.relations.find((rel) => (rel["from"] === link["fromAsset"]) && (rel["to"] === link["toAsset"]) && (rel["type"] === link["type"]));
    }

    deleteLink(link) {
        //console.log("Delete link: ");
        //console.log(link);

        var relation = this.getRelationForLink(link);

        if (relation) {
            console.log("Deleting relation: " + relation["id"]);
            this.props.dispatch(suppressCanvasRefresh(false));
            var typeSuffix = relation["type"].split("#")[1]
            this.props.dispatch(deleteAssertedRelation(this.props.modelId, relation["relationId"], relation["fromId"], typeSuffix, relation["toId"]));
        }
        else {
            alert("ERROR: Could not locate relation");
        }
    }
}

PatternPanel.propTypes = {
    modelId: PropTypes.string,
    threat: PropTypes.object,
    asset: PropTypes.object,
    assets: PropTypes.array,
    relations: PropTypes.array,
    getNodes: PropTypes.func,
    dispatch: PropTypes.func,
    isRelationDeletable: PropTypes.func
};

export default PatternPanel;
