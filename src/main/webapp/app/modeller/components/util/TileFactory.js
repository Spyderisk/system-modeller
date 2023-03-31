/* Default variables */
import variables from "../../../common/styles/vars.scss";

let firstAttempt = true;
let placingTile = null;
let currentEndpoints = {};
var startElementId = null;
let thisPlumbingInstance = jsPlumb.getInstance();
export var zoom = 1;

export default function setupJsPlumb() {
    thisPlumbingInstance.importDefaults({
        DragOptions: { cursor: "pointer", zIndex: 20000 },
        Anchor: ["Perimeter", { shape: "Rectangle", anchorCount: 200 }],
        Connector: ["Straight", {}],
        Endpoint: "Blank",
    });

    thisPlumbingInstance.registerConnectionTypes({
        basic: {
            paintStyle: {
                stroke: variables.BOX_SHADOW_COLOUR,
                outlineStroke: variables.BOX_SHADOW_COLOUR,
                outlineWidth: 1,
                strokeWidth: 3,
            },
            // hoverPaintStyle: {
            //     fill: variables.BRIGHT_RED, // $BRIGHT_RED,
            //     stroke: variables.BRIGHT_RED, // $BRIGHT_RED,
            //     outlineStroke: variables.BRIGHT_RED, // $BRIGHT_RED
            // }
        },
        hover: {
            paintStyle: {
                stroke: variables.BRIGHT_RED,
                outlineStroke: variables.BRIGHT_RED,
                outlineWidth: 1,
                strokeWidth: 4,
            },
            // hoverPaintStyle: {
            //     fill: variables.BRIGHT_RED, // $BRIGHT_RED,
            //     stroke: variables.BRIGHT_RED, // $BRIGHT_RED,
            //     outlineStroke: variables.BRIGHT_RED, // $BRIGHT_RED
            // }
        },
        pattern: {
            paintStyle: {
                fill: variables.YELLOW_GREEN, // $YELLOW_GREEN,
                stroke: variables.YELLOW_GREEN, // $YELLOW_GREEN,
                outlineStroke: variables.YELLOW_GREEN, // $YELLOW_GREEN,
                outlineWidth: 1,
                strokeWidth: 4,
            },
        },
        inferred: {
            paintStyle: {
                stroke: variables.LIGHT_YELLOW, // $LIGHT_YELLOW
                outlineStroke: variables.BOX_SHADOW_COLOUR, // $BOX_SHADOW_COLOUR
                outlineWidth: 1,
                strokeWidth: 4,
            },
            // hoverPaintStyle: {
            //     fill: variables.BRIGHT_RED, // $BRIGHT_RED,
            //     stroke: variables.BRIGHT_RED, // $BRIGHT_RED,
            //     outlineStroke: variables.BRIGHT_RED, // $BRIGHT_RED
            // }
        },
    });
}

/**
 * Code snippet from jsPlumb
 */

// Storing 1/zoom level e.g 200% is zoom 0.5
export function getZoom() {
    return zoom;
}

// Storing 1/zoom level e.g 200% is zoom 0.5
export function setZoom(newZoom) {
    zoom = newZoom;
}

window.setZoom = function (zoom, instance, transformOrigin) {
    let el = document.getElementById("tile-canvas");
    if (!transformOrigin) {
        console.log(transformOrigin);
    }
    let oString = transformOrigin[0] + "px " + transformOrigin[1] + "px";
    // const p = ["webkit", "moz", "ms", "o"],
    //     s = "scale(" + zoom + ")",
    //     oString = transformOrigin[0] + "px " + transformOrigin[1] + "px";
    //
    // for (let i = 0; i < p.length; i++) {
    //     el.style[p[i] + "Transform"] = s;
    //     el.style[p[i] + "TransformOrigin"] = oString;
    // }

    el.style["transform"] = "scale(" + zoom + ")";
    // el.style["transformOrigin"] = oString;

    instance.setZoom(zoom);
};

/**
 * Called when an asserted asset wants to initiate a new connection.
 * @param id The ID of the  asserted asset.
 * @param validEndpoints the list of possible endpoint types for the starting element
 */
//export function startConnection (id, validEndpoints) {
export function startConnection(
    id,
    linkToTypes,
    linkFromTypes,
    updating = false
) {
    //console.log(getStartElementId());

    startElementId = id;
    //currentEndpoints = validEndpoints;
    currentEndpoints = getValidEndpoints(linkToTypes, linkFromTypes);

    //console.log("currentEndpoints: ", currentEndpoints);
    //console.log("getCurrentEndpointIds: ", getCurrentEndpointIds());
    let validTargetsArray = getCurrentEndpointIds();
    if (!updating) {
        let allAssertedTiles = document.getElementsByClassName("asserted-tile");
        for (let i in allAssertedTiles) {
            if (
                allAssertedTiles[i].id &&
                startElementId === allAssertedTiles[i].id.replace("tile-", "")
            )
                continue;
            if (
                validTargetsArray.every((element) => {
                    return (
                        allAssertedTiles[i].id &&
                        element !== allAssertedTiles[i].id.replace("tile-", "")
                    );
                })
            ) {
                $("#" + allAssertedTiles[i].id).addClass("fade-tile");
            }
        }

        $("#tile-" + validTargetsArray.join(",#tile-")).each(function () {
            if ($(this).attr("id") !== "tile-" + id) {
                $(this)
                    .find("span[id='" + $(this).attr("id") + "-complete']")
                    .show();
            } else {
                $(this)
                    .find("span[id='" + $(this).attr("id") + "-add']")
                    .show();
            }
            $(this).find(".glyph-bar").show();

            $(this).addClass("valid-targets");
        });
    }
}

function getValidEndpoints(linkToTypes, linkFromTypes) {
    //console.log("getting all valid endpoints");
    //console.log("linkFromTypes: ", linkFromTypes);
    //console.log("linkToTypes: ", linkToTypes);
    let validEndpoints = { incoming: linkFromTypes, outgoing: linkToTypes };
    //console.log("validEndpoints: ", validEndpoints);
    return validEndpoints;
}

/**
 * Called when an asserted asset wants to stop a connection.
 */
export function stopConnection() {
    $(".tile").each(function () {
        if ($(this).attr("id") !== startElementId) {
            $(this)
                .find("span[id='" + $(this).attr("id") + "-complete']")
                .hide();
        } else {
            if (!$(this).is(":hover")) {
                $(this)
                    .find("span[id='" + $(this).attr("id") + "-add']")
                    .hide();
            }
        }
        $(this).find(".glyph-bar").hide();
    });
    if (getStartElementId() !== null) {
        const startingAsset = $(document).find(
            "div[id='tile-" + getStartElementId() + "']"
        );
        const startingAssetAddGlyph = startingAsset.find(
            "span[id='tile-" + getStartElementId() + "-add']"
        );
        startingAsset.removeClass("connecting-tile");
        startingAssetAddGlyph.removeClass("fa-stop-circle cancel-connection");
        startingAssetAddGlyph.addClass("fa-plus add-connection");
        startingAssetAddGlyph.hide();
        startingAsset
            .find("span[id='" + getStartElementId() + "-complete']")
            .hide();
    }

    let allAssertedTiles = document.getElementsByClassName("asserted-tile");
    for (let i in allAssertedTiles) {
        $("#" + allAssertedTiles[i].id).removeClass(
            "fade-tile valid-source valid-targets"
        );
    }
    clearConnection();
}

/**
 * Called to reset a connection state and start again
 */
export function clearConnection() {
    if (startElementId !== null) {
        startElementId = null;
        currentEndpoints = {};
    }
}

/* Getters and setters in the TileFactory */

export function getPlumbingInstance() {
    if (firstAttempt) {
        console.log("--- first attempt to reach jsPlumb has been fired ---");
        firstAttempt = false;
    }
    return thisPlumbingInstance;
}

export function isPlacingTile() {
    return placingTile !== null;
}

export function startPlacingTile(el1) {
    placingTile = el1;
}

export function endPlacingTile() {
    placingTile = null;
}

export function isConnStarted() {
    return startElementId !== null;
}

export function getStartElementId() {
    return startElementId;
}

export function getCurrentEndpoints() {
    return currentEndpoints;
}

export function getCurrentEndpointIds() {
    const flatEndpoints = [];

    //incoming relations
    let incomingEndpoints = currentEndpoints["incoming"];
    for (let type in incomingEndpoints) {
        if (incomingEndpoints.hasOwnProperty(type)) {
            let assets = incomingEndpoints[type]["assets"];
            for (let i = 0; i < assets.length; i++) {
                let id = assets[i];
                if ($.inArray(id, flatEndpoints) === -1) {
                    flatEndpoints.push(id); //add to list, if not presemt
                }
            }
        }
    }

    //outgoing relations
    let outgoingEndpoints = currentEndpoints["outgoing"];
    for (let type in outgoingEndpoints) {
        if (outgoingEndpoints.hasOwnProperty(type)) {
            let assets = outgoingEndpoints[type]["assets"];
            for (let i = 0; i < assets.length; i++) {
                let id = assets[i];
                if ($.inArray(id, flatEndpoints) === -1) {
                    flatEndpoints.push(id); //add to list, if not presemt
                }
            }
        }
    }

    const index = flatEndpoints.indexOf(getStartElementId());
    if (index >= 0) {
        flatEndpoints.splice(index, 1);
    }

    return flatEndpoints;
}

// this is the paint style for the connecting lines..
export var connectorPaintStyle = {
    strokeWidth: 3,
    stroke: variables.BOX_SHADOW_COLOUR,
    // joinstyle: "round",
    outlineStroke: variables.BOX_SHADOW_COLOUR,
    outlineWidth: 1,
};
export var endpointHoverStyle = {
    fill: "#216477",
    stroke: "#216477",
};

export var outerOver = false;
export var dragDotOver = false;
export var linkHover = false;
export var hoveredAsset = "";
export var draggingConnection = false;

export var hoveringThreat = false;
export var hoveredConns = [];

export function setHoveringThreat(status) {
    hoveringThreat = status;
    //console.log("hoveringThreat:", hoveringThreat);
}

export function setHoveredConns(conns) {
    hoveredConns = conns;
    //console.log("hoveredConns:", hoveredConns);
}

export function setOuterOver(status) {
    outerOver = status;
}

export function setDragDotOver(status) {
    dragDotOver = status;
}

export function setLinkHover(status) {
    linkHover = status;
}

export function setHoveredAsset(assetId) {
    //console.log("setHoveredAsset: " + assetId);
    hoveredAsset = assetId;
}

export function setDraggingConnection(status) {
    draggingConnection = status;
}

export var _addEndpoints = (el, assetId, sourceEndpoint) => {
    thisPlumbingInstance.batch(() => {
        // The syntax here is x, y, dx, dy, offset-x, offset-y
        // x, y: position of anchor with 0, 0 being top left and 1,1 being bottom-right
        // dx, dy: where any connector curve emanates from (not relevant as we use stright line)
        // offset: moves the anchor relative to default position (which is too far away from tile)
        let sourceAnchors = [
            [0.5, 0, 0, 0, 0, 10],
            [0.5, 1, 0, 0, 0, -10],
            [0, 0.5, 0, 0, 10, 0],
            [1, 0.5, 0, 0, -10, 0]
        ];

        for (const a of sourceAnchors) {
            thisPlumbingInstance.addEndpoint(el, sourceEndpoint, {
                anchor: a,
                scope: "connection-source",
            });
        }

        // listen for new connections; initialise them the same way we initialise the connections at startup.
        let endpoints = thisPlumbingInstance.selectEndpoints({
            scope: "connection-source",
        });
        // console.log(`add - ${endpoints.length}`);

        endpoints.bind("mouseover", () => {
            dragDotOver = true;
            outerOver = true;
            setLinkHover(false);
            //console.log("setHoveredAsset 6");
            setHoveredAsset(assetId);
            thisPlumbingInstance.select({ scope: "hovers" }).delete();
        });
        endpoints.bind("mouseleave", () => {
            dragDotOver = false;
            //console.log("setHoveredAsset 7");
            setHoveredAsset("");
        });
    });
};

export var _delEndpoints = () => {
    let endpoints = getPlumbingInstance().selectEndpoints({
        scope: "connection-source",
    });
    // console.log(`delete - ${endpoints.length}`);
    dragDotOver = false;
    endpoints.unbind();
    endpoints.each((endpoint) => {
        getPlumbingInstance().deleteEndpoint(endpoint);
    });
};

export var connAddOverlay = (conn, relation) => {
    conn.addOverlay([
        "Arrow",
        {
            width: 20,
            length: 12,
            location: 1.0,
            id: `hover-ov-${conn.id}`,
            paintStyle: {
                fill: variables.BRIGHT_RED, // $BRIGHT_RED,
                stroke: variables.BRIGHT_RED, // $BRIGHT_RED
            },
        },
    ]);
    conn.addOverlay([
        "Label",
        {
            label:
                relation["sourceCardinality"] > -1
                    ? relation["sourceCardinality"] + ""
                    : "*",
            location: 0.15,
            id: "srcCard",
            cssClass: "cardinality-overlay",
        },
    ]);
    conn.addOverlay([
        "Label",
        {
            label:
                relation["targetCardinality"] > -1
                    ? relation["targetCardinality"] + ""
                    : "*",
            location: 0.85,
            id: "tgtCard",
            cssClass: "cardinality-overlay",
        },
    ]);
};

export var addConnHover = () => {};

export var addConn = (
    relation,
    labelClass,
    relationType,
    scope,
    labelLoc = null,
    source = null,
    target = null
) => {
    let overlays;

    if (scope === "hovers") {
        overlays = [
            [
                "Label",
                {
                    id: "label",
                    label: relation["label"] || "->",
                    location: labelLoc,
                    cssClass: labelClass,
                },
            ],
            [
                "Label",
                {
                    label:
                        relation["targetCardinality"] > -1
                            ? relation["targetCardinality"] + ""
                            : "*",
                    location: 0.85,
                    id: "tgtCard",
                    cssClass: "cardinality-overlay",
                },
            ],
            [
                "Label",
                {
                    label:
                        relation["sourceCardinality"] > -1
                            ? relation["sourceCardinality"] + ""
                            : "*",
                    location: 0.15,
                    id: "srcCard",
                    cssClass: "cardinality-overlay",
                },
            ],
            [
                "Arrow",
                {
                    width: 20,
                    length: 12,
                    location: 1.0,
                    id: `hover-ov`,
                    paintStyle: {
                        fill: variables.BRIGHT_RED, // $BRIGHT_RED,
                        stroke: variables.BRIGHT_RED, // $BRIGHT_RED
                    },
                },
            ],
        ];
    } else if (scope === "patterns") {
        overlays = [
            [
                "Arrow",
                {
                    width: 20,
                    length: 12,
                    location: 1.0,
                    id: `pattern-ov`,
                },
            ],
            [
                "Label",
                {
                    id: "pattern-label-ov",
                    label: relation["label"] || "->",
                    location: labelLoc,
                    cssClass: labelClass,
                },
            ],
        ];
    } else if (relationType === "inferred") {
        overlays = [
            [
                "Label",
                {
                    id: "label",
                    label: relation["label"] || "->",
                    location: 0.4,
                    cssClass: labelClass,
                },
            ],
            [
                "Custom",
                {
                    create: function (component) {
                        return $(
                            '<div><i class="fa fa-spinner fa-pulse fa-lg fa-fw"/></div>'
                        );
                    },
                    id: "spinner" + relation["id"],
                    location: 0.4,
                },
            ],
            [
                "Arrow",
                {
                    width: 20,
                    length: 12,
                    location: 1.0,
                    id: `inferred-ov`,
                    paintStyle: {
                        stroke: variables.BOX_SHADOW_COLOUR, // $BOX_SHADOW_COLOUR
                        fill: variables.LIGHT_YELLOW, // $LIGHT_YELLOW
                        outlineStroke: variables.BOX_SHADOW_COLOUR, // $BOX_SHADOW_COLOUR
                    },
                },
            ],
        ];
    } else {
        overlays = [
            [
                "Label",
                {
                    id: "label",
                    label: relation["label"] || "->",
                    location: 0.4,
                    cssClass: labelClass,
                },
            ],
            [
                "Custom",
                {
                    create: function (component) {
                        return $(
                            '<div><i class="fa fa-spinner fa-pulse fa-lg fa-fw"/></div>'
                        );
                    },
                    id: "spinner" + relation["id"],
                    location: 0.4,
                },
            ],
            [
                "Arrow",
                {
                    width: 20,
                    length: 12,
                    location: 1.0,
                    id: "basic-ov",
                },
            ],
        ];
    }
    return thisPlumbingInstance.connect({
        source: source ? source : 'tile-' + relation["fromID"],
        target: target ? target : 'tile-' + relation["toID"],
        type: relationType,
        scope: scope,
        overlays: overlays,
    });
};
