import React from "react";
import PropTypes from "prop-types";
import {deleteAssertedRelation, hideRelation, putRelationRedefine} from "../../../actions/ModellerActions";
import {ContextMenu, ContextMenuTrigger, MenuItem, SubMenu} from "react-contextmenu";
import {wordWrap} from "../../util/wordWrap"

class RelationCtxMenu extends React.Component {

    constructor(props) {
        super(props);

        this.handleDeleteRelation = this.handleDeleteRelation.bind(this);
        this.handleCardinalities = this.handleCardinalities.bind(this);
        this.handleChangeRelation = this.handleChangeRelation.bind(this);
        this.handleHideRelation = this.handleHideRelation.bind(this);

        this.state = {
            loading: false,
            cardinalities: {
                source: -1,
                target: -1
            },
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            ...this.state,
            cardinalities: {
                source: nextProps.relation["sourceCardinality"],
                target: nextProps.relation["targetCardinality"]
            }
        })
    }


    render() {
        let {cardinalities} = this.state;
        let badCardinality = cardinalities.source === -2 || cardinalities.target === -2;
        let titleInferred = '';
        if (!this.props.relation.asserted) {
            titleInferred += " (inferred)"
        }

        return (
            <div className="relation-ctx-menu">
                <ContextMenuTrigger ref={this.props.contextTrigger} id="relation-ctx-menu"
                                    disableIfShiftIsPressed={true}>
                    <div></div>
                </ContextMenuTrigger>
                <ContextMenu id="relation-ctx-menu">
                    <MenuItem attributes={{style: {userSelect: "all"}}}
                              preventClose={true}>
                        {this.props.assetFromLabel}
                        <strong>{" " + (this.props.relation.label || "connects") + " "}</strong>
                        {this.props.assetToLabel + titleInferred}
                    </MenuItem>
                    <MenuItem divider/>

                    <SubMenu title="Details">
                <span id={this.props.relation.id + "ctx-menu"}
                      style={{overflowWrap: "break-word", maxWidth: "240px", userSelect: "all"}}>
                    {wordWrap(this.props.relationComment || " ", 36, "\n").split("\n")
                        .map((i, key) => {
                            return <div key={key}>{i}<br/></div>;
                        })}
                </span>
                    </SubMenu>
                    {this.props.authz.userEdit ?
                        <div>
                            <MenuItem onClick={this.handleChangeRelation}
                                disabled={!this.props.relation.asserted}>Edit Type
                            </MenuItem>

                            <MenuItem onClick={this.handleHideRelation}>{this.props.relation.hidden ? "Unhide" : "Hide"}
                            </MenuItem>

                            <MenuItem onClick={this.handleDeleteRelation}
                                disabled={!this.props.relation.asserted}>Delete
                            </MenuItem>
                        </div>
                        :
                        <div></div>
                    }

                </ContextMenu>
            </div>);

        /*                    <div className="cardinalities">
                                <strong>Cardinality:</strong>
                                <div className="cardinality-form">
                                    <span style={{fontSize: '0.8em'}}>Source:</span>
                                    <Input type="text"
                                           value={cardinalities.source === -1 ? "*" :
                                               cardinalities.source === -2 ? "" : cardinalities.source}
                                           shouldUpdate={!this.props.loading}
                                           onChange={(e) => {
                                               e.preventDefault();
                                               let val = e.nativeEvent.target.value;
                                               if (/^(\d*|\*)$/.test(val)) {
                                                   if (val === "*") {
                                                       val = -1;
                                                   }
                                                   if (val === "") {
                                                       val = -2;
                                                   } else {
                                                       val = parseInt(val);
                                                   }
                                                   this.setState({...this.state, cardinalities: {...cardinalities, source: val}})
                                               }
                                           }}/>
                                    <span style={{fontSize: '0.8em'}}>Target:</span>
                                    <Input type="text"
                                           value={cardinalities.target === -1 ? "*" :
                                               cardinalities.target === -2 ? "" : cardinalities.target}
                                           shouldUpdate={!this.props.loading}
                                           onChange={(e) => {
                                               e.preventDefault();
                                               let val = e.nativeEvent.target.value;
                                               if (/^(\d*|\*)$/.test(val)) {
                                                   if (val === "*") {
                                                       val = -1;
                                                   }
                                                   if (val === "") {
                                                       val = -2;
                                                   } else {
                                                       val = parseInt(val);
                                                   }
                                                   this.setState({...this.state, cardinalities: {...cardinalities, target: val}})
                                               }
                                           }}/>
                                </div>
                                <div style={{width: '100%', textAlign: 'right', paddingTop: '0.4em'}}>
                                <Button disabled={badCardinality} bsSize="xsmall"
                                        bsStyle={badCardinality ? "danger" : "primary"}
                                        onClick={this.handleCardinalities}>{this.props.loading ?
                                    <span className="fa fa-cog fa-fw fa-spin"/> : <span>Save</span>}</Button>
                                </div>
                            </div>
                            <hr/>*/
    }

    handleCardinalities() {
        this.setState({
            ...this.state,
            loading: true
        });
        this.props.dispatch(putRelationRedefine(this.props.modelId, this.props.relation["id"], {
            ...this.props.relation,
            sourceCardinality: this.state.cardinalities.source,
            targetCardinality: this.state.cardinalities.target
        }));
    }

    handleDeleteRelation() {
        if (this.props.relation.asserted) {
            var typeSuffix = this.props.relation.type.split("#")[1]
            this.props.dispatch(deleteAssertedRelation(this.props.modelId, this.props.relation.id, this.props.relation.fromID, typeSuffix, this.props.relation.toID));
        }
    }

    handleChangeRelation(event) {
        console.log(this.props.relation.id);
        if (this.props.relation.asserted) {
            this.props.changeRelationType.startConnection(event, false, true);
            this.props.changeRelationType.changeRelationType(event,
                this.props.relation.fromID, this.props.relation.id);
        }
    }

    handleHideRelation() {
        this.props.dispatch(hideRelation(this.props.relation.id));
    }

}

/**
 * This describes the data types of all of the props for validation.
 * @type {{onRelationCreation: *, asset: *, assetType: *, modelId: *, dispatch: *}}
 */
RelationCtxMenu.propTypes = {
    modelId: PropTypes.string,
    assetFromLabel: PropTypes.string,
    assetToLabel: PropTypes.string,
    relation: PropTypes.object,
    relationComment: PropTypes.string,
    contextTrigger: PropTypes.func,
    changeRelationType: PropTypes.object,
    dispatch: PropTypes.func,
    authz: PropTypes.object
};

/* This exports the AssertedAsset class as required. */
export default RelationCtxMenu;
