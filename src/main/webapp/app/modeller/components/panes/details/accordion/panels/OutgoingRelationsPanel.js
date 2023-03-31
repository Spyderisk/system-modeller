import React from "react";
import PropTypes from 'prop-types';
import {Button, OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../../common/constants.js";
import {
    changeSelectedAsset,
    deleteAssertedRelation,
    hideRelation,
    postAssertedRelation,
    putRelationRedefine
} from "../../../../../actions/ModellerActions";
import {getPlumbingInstance} from "../../../../util/TileFactory";
import ChangeRelationModal from "../../popups/ChangeRelationModal";

class OutgoingRelationsPanel extends React.Component {

    constructor(props) {
        super(props);

        this.hoverRel = this.hoverRel.bind(this);
        this.unHoverRel = this.unHoverRel.bind(this);

        this.handleEdit = this.handleEdit.bind(this);
        this.openChangeRelationModal = this.openChangeRelationModal.bind(this);
        this.submitChangeRelationModal = this.submitChangeRelationModal.bind(this);
        this.closeChangeRelationModal = this.closeChangeRelationModal.bind(this);
    }

    componentWillMount() {
        this.setState({
            changeRelationModal: {
                show: false,
                assets: [],
                relation: {},
                host: {},
                isFrom: true,
                issue: {}
            }
        });
    }

    render() {
        const self = this;
        //TODO: rename incomingRelations to outgoingRelations (or use generic name)
        const incomingRelations = this.props.model["relations"].filter((relation) => relation["fromID"] === self.props.selectedAsset["id"]);
        //console.log(incomingRelations);

        let hiddenDiv;

        return (
            <div className="outgoing-rel detail-list">
                <div className="container-fluid">
                    <span>{incomingRelations.length === 0 ? "No outgoing relations defined" : ""}</span>
                    {incomingRelations.sort((relA, relB) => {
                        if (relA["label"].localeCompare(relB["label"]) === 0) {
                            let assetA = this.props.model["assets"].find((asset) => asset["id"] === relA["fromID"]);
                            if (assetA === undefined) return 1;
                            let assetB = this.props.model["assets"].find((asset) => asset["id"] === relB["fromID"]);
                            if (assetB === undefined) return -1;
                            return assetA["label"].localeCompare(assetB["label"]);
                        }
                        return relA["label"].localeCompare(relB["label"]);
                    }).map((relation, index) => {
                        let fromAsset = this.props.model["assets"].find((mAsset) => mAsset["id"] === relation["fromID"]);
                        let toAsset = this.props.model["assets"].find((mAsset) => mAsset["id"] === relation["toID"]);
                        let toAssetInfo = toAsset === undefined ?
                            "?"
                            :
                            toAsset["label"];
                        //console.log("Relation " + relation["label"] + ": asserted = " + relation["asserted"]);
                        //Currently the asserted flag is faulty due to problem in querier. TODO: use the below once that problem has been fixed
                        //let deleteTooltipText = relation["asserted"] ? "Delete relation" : "Inferred asset (cannot delete)";
                        let deleteTooltipText = "Delete relation";
                        let deletable = this.props.isRelationDeletable(relation);

                        if (fromAsset && fromAsset.asserted && toAsset && toAsset.asserted
                                && relation["hidden"]) {
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
                        } else if (fromAsset && fromAsset.asserted && toAsset && toAsset.asserted
                            && !relation["hidden"]) {
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

                        return (
                            <div key={"rel" + index} className="row detail-info rel-info row-hover"
                                 onMouseOver={() => this.hoverRel(relation)}
                                 onMouseOut={this.unHoverRel}>
                                {hiddenDiv}
                                {!relation["asserted"] &&
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top" overlay={
                                    <Tooltip id={`out-rel-${index}-tooltip`}
                                             className={"tooltip-overlay"}>
                                        <strong>{"Inferred relation"}</strong>
                                    </Tooltip>
                                }>
                                    <span className="inferred-col fa fa-tag col-xs-1"
                                          style={{left: "5px"}}/>
                                </OverlayTrigger>}
                                <span className={"rel col-xs-10" + (relation["asserted"] ? " col-xs-offset-1" : "")}>
                                    <span className="detail-selected-asset">{this.props.asset["label"]}</span>
                                    {" "}
                                    <strong>
                                    {relation["label"] !== null ? this.props.formatRelationLabel(relation["label"]) : "->"}
									</strong>
                                    {" "}

                                    <span style={{cursor: "pointer"}} className={"clickable"}
                                          onClick={() => this.props.dispatch(changeSelectedAsset(toAsset.id))}>
                                        <strong>{toAssetInfo}</strong>
                                    </span>
                                </span>

                                {deletable && this.props.authz.userEdit ?
                                <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                                overlay={
                                                    <Tooltip id={`inc-rel-${index}-del-tooltip`}
                                                             className={"tooltip-overlay"}>
                                                        <strong>{deleteTooltipText}</strong>
                                                    </Tooltip>
                                                }>
                                        <span className="menu-close fa fa-trash col-xs-1" onClick={(e) => {
                                            e.nativeEvent.target.className = "fa fa-cog fa-fw fa-spin";
                                            var typeSuffix = relation["type"].split("#")[1]
                                            this.props.dispatch(deleteAssertedRelation(this.props.model["id"], relation["relationId"], relation["fromID"], typeSuffix, relation["toID"]));
                                        }}/>
                                </OverlayTrigger> : <span className="col-xs-1"/> }
                            </div>
                        );
                    })}
                    {this.props.selectedAsset["id"] !== "" && this.props.authz.userEdit ?
                        <div className="define-rel">
                            <Button bsStyle="primary"
                                    bsSize="small"
                                    onClick={() => this.props.handleAdd(self.props.model.assets
                                        .find((asset) => asset["id"] === self.props.selectedAsset["id"]), false)}>
                                {"+ Add"}
                            </Button>
                        </div>
                        :
                        null
                    }
                </div>

                <ChangeRelationModal show={this.state.changeRelationModal.show}
                                     onHide={this.closeChangeRelationModal}
                                     assets={this.state.changeRelationModal.assets}
                                     host={this.state.changeRelationModal.host}
                                     relation={this.state.changeRelationModal.relation}
                                     issue={this.state.changeRelationModal.issue}
                                     isFrom={this.state.changeRelationModal.isFrom}
                                     formatRelationLabel={this.props.formatRelationLabel}
                                     submit={this.submitChangeRelationModal}/>

            </div>
        );
    }

    /**
     * Handle adding a mandatory relationship
     */
    handleEdit(linkTo, relation, host, isFrom, issue) {
        let assets = this.props.getAssetsForType(linkTo);
        
        if (assets.length <= 0) {
            console.warn("No matching assets found");
        }

        this.openChangeRelationModal(assets, relation, host, isFrom, issue);
    }

    openChangeRelationModal(assets, relation, host, isFrom, issue) {
        this.setState({
            ...this.state,
            changeRelationModal: {
                ...this.state.changeRelationModal,
                show: true,
                assets: assets,
                relation: relation,
                host: host,
                isFrom: isFrom,
                issue: issue
            }
        });
    }

    submitChangeRelationModal(relation) {
        //console.log("submitChangeRelationModal:", relation);
        if (typeof relation["id"] === 'undefined') {
            this.props.dispatch(postAssertedRelation(this.props.model["id"], relation["fromID"], relation["toID"], relation));
        }
        else {
            this.props.dispatch(putRelationRedefine(this.props.model["id"], relation["id"], relation));
        }
    }

    closeChangeRelationModal() {
        this.setState({
            ...this.state,
            changeRelationModal: {
                ...this.state.changeRelationModal,
                show: false
            }
        });
    }

    hoverRel(rel) {
        const assetFrom = this.props.model["assets"].find((mAsset) => mAsset["id"] === rel["fromID"]);
        const assetTo = this.props.model["assets"].find((mAsset) => mAsset["id"] === rel["toID"]);
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
            let connLabel = conn.getOverlay("label");
            let labelLoc = connLabel.getLocation();
            let connEl = connLabel.getElement();
            if (connEl.innerHTML === rel["label"]) {
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

    unHoverRel() {
        if (this.hoveredLink) {
            let hoveredConn = this.hoveredLink;
            let conn = hoveredConn.conn;
            let originalType = hoveredConn.originalType;
            conn.setType(originalType.join(" "));
            this.hoveredLink = null;
        }
        
        $(".tile").removeClass("hover");
    }

}

OutgoingRelationsPanel.propTypes = {
    model: PropTypes.object,
    selectedAsset: PropTypes.object,
    asset: PropTypes.object,
    dispatch: PropTypes.func,
    getAssetType: PropTypes.func,
    getAssetsForType: PropTypes.func,
    getLink: PropTypes.func,
    handleAdd: PropTypes.func,
    formatRelationLabel: PropTypes.func,
    isRelationDeletable: PropTypes.func
};

export default OutgoingRelationsPanel;
