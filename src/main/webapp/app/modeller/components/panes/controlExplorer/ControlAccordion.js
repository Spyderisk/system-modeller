import React from "react";
import PropTypes from "prop-types";
import {Badge, OverlayTrigger, Panel, Row, Tooltip, Button, ButtonToolbar} from "react-bootstrap";
import * as Constants from "../../../../common/constants.js";
import {
    changeSelectedAsset,
    getControlStrategiesForControlSet,
    openControlStrategyExplorer,
    openControlExplorer,
    updateControlOnAsset,
    updateControls
} from "../../../actions/ModellerActions";
import {getPlumbingInstance, hoveredConns, setHoveredConns} from "../../util/TileFactory";
import PagedPanel from "../../../../common/components/pagedpanel/PagedPanel"
import {bringToFrontWindow} from "../../../actions/ViewActions";

class ControlAccordion extends React.Component {

    constructor(props) {
        super(props);
        
        this.hoverAsset = this.hoverAsset.bind(this);
        this.getControlStrategies = this.getControlStrategies.bind(this);
        this.toggleExpanded = this.toggleExpanded.bind(this);
        this.getRelatedControls = this.getRelatedControls.bind(this);
        this.getControlSets = this.getControlSets.bind(this);

        this.state = {
            expanded: {
                assets: true,
                related: false
            },
            controlSets: this.getControlSets(props.model.controlSets, props.selectedAsset.selectedControl)
        }
    }
    
    shouldComponentUpdate(nextProps, nextState) {
        return true;
    }

    componentWillReceiveProps(nextProps) {
        let expanded = this.state.expanded;
        let updatedControlSets = {};
        
        if (this.props.selectedAsset.selectedControl !== nextProps.selectedAsset.selectedControl) {
            updatedControlSets = this.getControlSets(nextProps.model.controlSets, nextProps.selectedAsset.selectedControl);
        }
        else {
            let nextControlSets = Object.values(this.getControlSets(nextProps.model.controlSets, nextProps.selectedAsset.selectedControl));
            let cs = nextControlSets; //KEM - just set the cs to the incoming objects
            let updatedCS = cs.filter((c) => c !== undefined);

            updatedControlSets = {...this.state.controlSets};
            updatedCS.forEach((cs) => {
                updatedControlSets[cs.uri] = cs;
            });
        }
            
        this.setState({...this.state,
            expanded: expanded,
            controlSets: updatedControlSets
        });
    }

    getControlSets(allControlSets, selectedControl) {
        let controlSets = {};
        allControlSets.forEach((cs) => {
            if (cs.control === selectedControl){
                controlSets[cs.uri] = cs;
            }
        });

        return controlSets;
    }

    render() {
        let {expanded} = this.state;

        let controlSets = Object.values(this.state.controlSets);
        
        var assetUris = new Map();
        controlSets.forEach((c) => {
            assetUris.set(c.assetUri, c);
        });

        var assets = new Map();
        this.props.model.assets.forEach((a) => {
            if(Array.from(assetUris.keys()).includes(a.uri)){
                assets.set(a, assetUris.get(a.uri));
            }
        });
        assets = new Map([...assets].sort((a, b) => {return (a[0].label > b[0].label) ? 1 : ((b[0].label > a[0].label) ? -1 : 0)}));

        var relatedControls = this.getRelatedControls();

        let assetsRender = [];

        Array.from(assets).map((data, index) => {
             var asset = data[0];
             var set = data[1];
             if (!set.assertable) return;
             var propsSet = this.props.model.controlSets.find((cs) => cs.id === set.id);
             var assetType = this.props.getAssetType(asset["type"]);
             let csgsMap = getControlStrategiesForControlSet(set.uri, this.props.model.threats, this.props.model.controlStrategies);
             let csgs = Object.values(csgsMap);
             let csgCount = csgs.length;
             let context = {"selection": "controlSet", "controlSet": set};

             let spinnerActive = set.proposed !== propsSet.proposed;
             assetsRender.push(
                 <Row
                     key={index + 1}
                     style={{margin: "5px 0 5px 0"}}
                     className={"row-hover"}
                 >
                     <span onMouseEnter={() => this.hoverAsset(asset, true, (set.proposed ? "green" : "default"))}
                          onMouseLeave={() => this.hoverAsset(asset, false, (set.proposed ? "green" : "default"))}>

                        <span className={this.props.authz.userEdit ? "traffic-lights" : "traffic-lights-view-only"}> {/*Don't need disabled tag as non assertable control sets are ignored / not rendered */}
                            <OverlayTrigger key={"red" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                                <Tooltip id={"ctrl-red-tooltip-" + index}
                                         className={"tooltip-overlay"}>
                                    {"Control not implemented"}
                                </Tooltip>
                            }>
                                <span className="fa-stack" onClick={() => this.props.authz.userEdit ? this.updateControl(set,false) : undefined}>
                                    <i className="bg fa fa-circle fa-stack-1x"/>
                                    <i className={!set.proposed ? "red fa fa-stop-circle fa-stack-1x" : "red fa fa-circle fa-stack-1x"}/>
                                </span>
                            </OverlayTrigger>
                            <OverlayTrigger key={"amber" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                                <Tooltip id={"ctrl-amber-tooltip-" + index}
                                         className={"tooltip-overlay"}>
                                    {"Control to be implemented"}
                                </Tooltip>
                            }>
                                <span className="fa-stack" onClick={() => this.props.authz.userEdit ? this.updateControl(set, true, true) : undefined}>
                                    <i className="bg fa fa-circle fa-stack-1x"/>
                                    <i className={set.proposed && set.workInProgress ?  "amber fa fa-stop-circle fa-stack-1x" : "amber fa fa-circle fa-stack-1x"}/>
                                </span>
                            </OverlayTrigger>
                            <OverlayTrigger key={"green" + index} delayShow={Constants.CTRL_TOOLTIP_DELAY} placement="top" overlay={
                                <Tooltip id={"ctrl-green-tooltip-" + index}
                                         className={"tooltip-overlay"}>
                                    {"Control is implemented"}
                                </Tooltip>
                            }>
                                <span className="fa-stack" onClick={() => this.props.authz.userEdit ? this.updateControl(set,true) : undefined}>
                                    <i className="bg fa fa-circle fa-stack-1x"/>
                                    <i className={set.proposed && !set.workInProgress ? "green fa fa-stop-circle fa-stack-1x" : "green fa fa-circle fa-stack-1x"}/>
                                </span>
                            </OverlayTrigger>
                        </span>

                         <OverlayTrigger
                             delayShow={Constants.TOOLTIP_DELAY} delayHide={0} placement="left"
                             overlay={
                                 <Tooltip
                                     id={`ctrl-accor-assets-${index + 1}-tooltip1`}
                                     className={"tooltip-overlay"}>
                                         This control is used in {csgCount === 1 ? "1 control strategy" : csgCount +
                                         " control strategies"}
                                 </Tooltip>
                             }>
                                 <Badge 
                                    style={{left:"5px", marginLeft:"6px", cursor: (csgCount > 0) ? "pointer" : "default"}}
                                    onClick={() => (csgCount > 0) ? this.openCsgExplorer(csgs, context): undefined}
                                 >{csgCount}</Badge>
                         </OverlayTrigger>
                         <OverlayTrigger
                             delayShow={Constants.TOOLTIP_DELAY} delayHide={0} placement="left"
                             overlay={
                                 <Tooltip
                                     id={`ctrl-accor-assets-${index + 1}-tooltip2`}
                                     className={"tooltip-overlay"}>
                                         {set.assertable ? (assetType["description"] !== null ? "Type: " +
                                             assetType["label"] : "No description available") :
                                             "Inferred controls cannot be asserted"}
                                 </Tooltip>
                             }>
                                 <span
                                     onClick={() => {
                                         this.props.dispatch(changeSelectedAsset(asset.id));
                                     }}
                                     className={"clickable"}
                                     style={{
                                         margin: "2px 0 0 6px",
                                         cursor: "pointer"
                                     }}>
                                     {asset.label + " "}

                                 </span>
                         </OverlayTrigger>
                         <span>{spinnerActive ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw"/> : null}</span>
                     </span>
               </Row>
             );
         });

        return (
            <div className="panel-group accordion">
                <Panel bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            Related Controls
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            {relatedControls.size < 1 ? <span style={{ marginLeft: "5px" }}> No related controls found </span> : Array.from(relatedControls).map((data, index) => {
                                var uri = data[0];
                                var label = data[1];
                                return (
                                    <Row key={index + 1}
                                        className={"row-hover"}
                                    >
                                        <span className="col-xs-12 clickable"
                                            style={{ paddingLeft: "0px" }}
                                            onClick={() => {
                                                this.props.dispatch(openControlExplorer(uri));
                                                this.props.dispatch(bringToFrontWindow("controlExplorer"));
                                            }}>
                                            {label}
                                        </span>
                                    </Row>
                                );
                            })}
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
                <Panel defaultExpanded bsStyle="primary">
                    <Panel.Heading>
                        <Panel.Title toggle>
                            Assets
                        </Panel.Title>
                    </Panel.Heading>
                    <Panel.Collapse>
                        <Panel.Body>
                            {this.props.authz.userEdit ?
                                <ButtonToolbar>
                                    <Button bsSize="xsmall"
                                        onClick={() => this.updateSetOfControls(
                                            Array.from(assets.values()), true)
                                        }>
                                        Assert All
                                    </Button>
                                    <Button bsStyle="danger" bsSize="xsmall"
                                        onClick={() => this.updateSetOfControls(
                                            Array.from(assets.values()), false)
                                        }
                                    >
                                        Remove All
                                    </Button>
                                </ButtonToolbar>
                            :
                                <div></div>
                            }
                            <div className="container-fluid">
                                <PagedPanel panelData={assetsRender}
                                    pageSize={15}
                                    context={"control-accordion-" + this.props.selectedAsset.selectedControl}
                                    noDataMessage={"No assets"} />
                            </div>
                        </Panel.Body>
                    </Panel.Collapse>
                </Panel>
            </div>
        );
    }

    openCsgExplorer(csgs, context) {
        if (csgs.length > 0) {
            this.props.dispatch(openControlStrategyExplorer(csgs, context));
            this.props.dispatch(bringToFrontWindow("controlStrategyExplorer"));
        }
    }

    updateSetOfControls(controlsSet, flag) {
        let controlsToUpdate = Array.from(controlsSet.map((c) => {
            if (c.assertable && (flag !== c.proposed || c.workInProgress)) {
                return c.uri;
            }
        }).filter(uri => {return uri !== undefined}));
        
        if (controlsToUpdate.length > 0) {
            this.updateControlsState(controlsToUpdate, flag);
            this.props.dispatch(updateControls(this.props.model.id, controlsToUpdate, flag));
        }
        else {
            //console.log("No controls to update");
        }
    }

    updateControl(clickedControl, proposed = true, workInProgress = false) {
        if (clickedControl.hasOwnProperty("label")) {
            let updatedControl = {
                ...clickedControl,
                proposed: proposed,
                workInProgress: workInProgress
            };
            let updatedControlSets = {...this.state.controlSets};
            updatedControlSets[clickedControl.uri] = updatedControl;
            this.setState({...this.state,
                controlSets: updatedControlSets
            });
            this.props.dispatch(updateControlOnAsset(this.props.model.id, updatedControl["assetId"], updatedControl));
        } else {
            //console.log("Control's hasOwnProperty(label) is false");
        }
    }
    
    updateControlsState(controlsToUpdate, flag) {
        let updatedControlSets = {...this.state.controlSets};
        controlsToUpdate.map((c_uri) => {
            let control = this.state.controlSets[c_uri];
            if (control) {
                let updatedControl = {
                    ...control,
                    proposed: flag,
                    workInProgress: false
                };
                updatedControlSets[c_uri] = updatedControl;
            }
        });
        this.setState({...this.state,
            controlSets: updatedControlSets
        });
    }

    toggleExpanded(label) {
        let expanded = this.state.expanded;
        expanded[label] = !expanded[label];
        this.setState({
                           ...this.state,
                           expanded: expanded
                       })
    }

    getControlStrategies() {
        var csgs = new Map();
        var control = this.props.selectedAsset.selectedControl;
        if (control != null) {
            Object.values(this.props.model.threats).forEach((t) => {
                Object.keys(t.controlStrategies).forEach((csgUri) => {
                    let csg = this.props.model.controlStrategies[csgUri];
                    let controlSetUris = csg.mandatoryControlSets.concat(csg.optionalControlSets);
                    let controlSets = controlSetUris.map(csUri => this.props.controlSets[csUri]);
                    controlSets.forEach((cs) => {
                        if (cs.control === control) {
                            csgs.set(csg, t);
                        };
                    });
                });
            });
        }
        return csgs;
    }

    getRelatedControls() {
        var control = this.props.selectedAsset.selectedControl;
        var controls = new Map();
        if (control != null) {
            var csgs = this.getControlStrategies();
            Object.values(Array.from(csgs.keys())).forEach((csg) => {
                let controlSetUris = csg.mandatoryControlSets.concat(csg.optionalControlSets);
                let controlSets = controlSetUris.map(csUri => this.props.controlSets[csUri]);
                controlSets.forEach((cs) => {
                    if (cs.control != control) {
                        controls.set(cs.control, cs.label);
                    }
                });
            });
        }
        return controls;
    }

    hoverAsset(asset, flag, colour="default") {
        var styleClass = (colour == null || colour == "default" ? "hover" : "active-tile");

        if (!flag) {
            hoveredConns.map((hoveredConn) => {
                let conn = hoveredConn.conn;
                let labelEl = hoveredConn.labelEl;
                let originalType = hoveredConn.originalType;
                conn.setType(originalType.join(" "));
            });

            //Finally, clear the list of hovered conns
            setHoveredConns([]);
        }

        if (asset.asserted) {
            if (flag) {
                $("#tile-" + asset["id"]).addClass(styleClass);
            } else {
                $("#tile-" + asset["id"]).removeClass(styleClass);
            }
        } else {
            let a = this.props.model.assets.filter(b => b.inferredAssets.indexOf(asset.uri) > -1);
            if (a.length !== 0) {
                for (let c = 0; c < a.length; c++) {
                    if (flag) {
                        $("#tile-" + a[c]["id"]).addClass(styleClass);
                    } else {
                        $("#tile-" + a[c]["id"]).removeClass(styleClass);
                    }
                }
            }
            a = this.props.model.relations.filter(b => b.inferredAssets.indexOf(asset.uri) > -1);
            if (a.length !== 0 && flag) {
                let hConns = hoveredConns;
                for (let c = 0; c < a.length; c++) {
                    getPlumbingInstance().select(
                        {
                            source: "tile-" + a[c]["fromID"],
                            target: "tile-" + a[c]["toID"],
                            scope: ["relations", "inferred-relations"]
                        }, true
                    ).each((conn) => {
                        let connLabel = conn.getOverlay("label");
                        let labelLoc = connLabel.getLocation();
                        let connEl = connLabel.getElement();
                        if (connEl.innerHTML === a[c]["label"]) {
                            let currType = conn.getType();
                            let originalType = [...currType];
                            conn.setType("hover");

                            var hoveredConn = {
                                conn:  conn,
                                labelEl: connEl,
                                originalType: originalType
                            };
                            
                            hConns.push(hoveredConn);
                        }
                    });
                };
                setHoveredConns(hConns);
            }
        }
    }

}

ControlAccordion.propTypes = {
    selectedAsset: PropTypes.object,
    model: PropTypes.object,
    controlSets: PropTypes.object,
    loading: PropTypes.object,
    hoverThreat: PropTypes.func,
    dispatch: PropTypes.func,
    getAssetType: PropTypes.func,
};

export default ControlAccordion;
