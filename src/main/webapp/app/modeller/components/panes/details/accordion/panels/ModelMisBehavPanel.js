import React, {Fragment} from "react";
import PropTypes from "prop-types";
import {getRootCauses, updateMisbehaviourImpact, revertMisbehaviourImpact} from "../../../../../actions/ModellerActions";
import {FormControl, OverlayTrigger, Tooltip, FormGroup, Form, Checkbox, InputGroup} from 'react-bootstrap';
import {getLevelColour, getLevelValue, getRenderedLevelText} from "../../../../util/Levels";
import * as Constants from "../../../../../../common/constants.js";
import {connect} from "react-redux";
import PagedPanel from "../../../../../../common/components/pagedpanel/PagedPanel";
import {fromJS} from "immutable";
import {bringToFrontWindow} from "../../../../../actions/ViewActions";

var _ = require('lodash');
const { Map } = require('immutable');

var misBehavImmutable = Map();
var lastFilterState;

class ModelMisBehavPanel extends React.Component {

    constructor(props) {
        super(props);

        this.renderMisbehaviours = this.renderMisbehaviours.bind(this);
        this.valueChanged = this.valueChanged.bind(this);
        this.openMisbehaviourExplorer = this.openMisbehaviourExplorer.bind(this);
        this.sortByColumns = this.sortByColumns.bind(this);

        this.defaultFilterState = {
            search: "",
            hideInferredAssets: true,
            showFilters: false,
        };
        
        const sortDefaults = {
            column: 'riskValue', //main sortBy column (indicated by caret)
            direction: 'desc', //main sortBy direction (indicated by caret)
            columns: ['riskValue', 'name'], //initial sort columns (for _.orderBy function)
            directions: ['desc', 'asc'] //initial sort directions (for _.orderBy function)
        }
        
        let defaultColumns = [...sortDefaults.columns];
        let defaultDirections = [...sortDefaults.directions];
        
        this.state = {
            sortDefaults: {...sortDefaults},
            sort: {
                column: undefined, //Currently selected sortBy column
                direction: undefined,   //Currently selected sort direction
                columns: defaultColumns,
                directions: defaultDirections
            }
        };
        
        this.state.filter = this.defaultFilterState;

        lastFilterState = this.state;

        this.state = {
            ...this.state,
            tableData: [],
            viewedLevels: [],
            updating: {},
            selected: '',
            selectedThreat: undefined
        };
    }

    static getUpdatedState(props, state) {
        //console.log("getUpdatedState: props: ", props, state);
        
        let levels = Object.values(props.levels).sort(function(a, b) {
            return b.value - a.value;
        });

        //if we are selecting misbehaviours, use this list, otherwise use full list
        var misbehaviours = (props.panelType === "model-misbehaviours") ? Object.values(props.misbehaviours) : props.selectedMisbehaviours;
        //if (props.panelType == "secondary-effects") console.log("getUpdatedState: misbehaviours: ", misbehaviours);
        let tableData = [];

        let impact = {};
        let likelihood = {};
        let risk = {};
        let updating = {};

        // get list of assets for filtering
        let inferredAssets = _.filter(props.assets, ['asserted', false]);
        let inferredAssetURIs = _.map(inferredAssets, 'uri');

        let assetURIs = _.map(props.assets, 'uri');
        let idx = -1;
        let filterString = state.filter.search ? state.filter.search.trim().toLowerCase() : '';

        misbehaviours.map((misbehaviour, index) => {
            let uri = misbehaviour.uri;
            impact[uri] = misbehaviour.impactLevel;
            likelihood[uri] = misbehaviour.likelihood;
            risk[uri] = misbehaviour.riskLevel;
            updating[uri] = false;

            let impactValue = getLevelValue(misbehaviour.impactLevel);
            let likelihoodValue = getLevelValue(misbehaviour.likelihood);
            let riskValue = getLevelValue(misbehaviour.riskLevel);

            if (state.filter.hideInferredAssets) {
                idx = _.findIndex(inferredAssetURIs, (assetURI) => {
                    return assetURI === misbehaviour.asset
                });
                if (idx !== -1) {
                    return null;
                }
            }

            if (state.filter.search) {
                let searchString = `${misbehaviour.misbehaviourLabel} ${misbehaviour.assetLabel}`.toLowerCase();
                if (searchString.indexOf(filterString) === -1) {
                    return null;
                }
            }

            tableData.push({
                'uri': uri,
                'name': misbehaviour.misbehaviourLabel,
                'asset': misbehaviour.assetLabel,
                'impactValue': impactValue,
                'likelihoodValue': likelihoodValue,
                'riskValue': riskValue
            });
        });

        if (state.sort.columns && state.sort.directions) {
            //if we have user selected sortby column and direction then use this
            //console.log("Sorting by ", state.sort.columns, state.sort.directions);
            tableData = _.orderBy(tableData, state.sort.columns, state.sort.directions);
        }
        else {
            //otherwise, use the default sorting parameters
            //console.log("Sorting by ", state.sortDefaults.columns, state.sortDefaults.directions);
            tableData = _.orderBy(tableData, state.sortDefaults.columns, state.sortDefaults.directions);
        }

        //if (props.panelType == "secondary-effects") console.log("getUpdatedState: tableData:", tableData);
        let selectedThreat = props.selectedThreat;

        return {
            tableData,
            viewedLevels: levels || [],
            updating,
            selectedThreat
        };
    }

    componentWillUnmount() {
        misBehavImmutable = Map();
    }

    componentDidMount() {
        setTimeout(() => {
            if (misBehavImmutable.count() === 0) {
                this.forceUpdate();
            }
        }, 50);
    }

    sortByColumns (byColumn) {
        //console.log("sortByColumns:", byColumn);
        
        let sortByIndex = _.findIndex(this.state.sort.columns, (el) => { return el === byColumn; });
        let newDirection = this.state.sort.direction;
        let newColumns = this.state.sort.columns;
        let newDirections = this.state.sort.directions;
        let defaultDirection = this.state.sortDefaults.direction;
        
        if (byColumn === "") {
            newColumns = [...this.state.sortDefaults.columns];
            newDirections = [...this.state.sortDefaults.directions];
            byColumn = newColumns[0];
            newDirection = newDirections[0];
        }
        else if (byColumn !== this.state.sort.column) {
            if (this.state.sort.column || sortByIndex === -1) {
                //here, we are moving to a new column, so use the default direction for that column
                newDirection = defaultDirection;
                
                if (sortByIndex > 0) {
                    newColumns.splice(sortByIndex, 1);
                    newDirections.splice(sortByIndex, 1);
                }
                
                newColumns.splice(0, 0, byColumn);
                newDirections.splice(0, 0, newDirection);
            }
            else {
                //here, we don't already have a user-selected column, so we are currently using the default direction
                //therefore we need to reverse this
                newDirection = (this.state.sortDefaults.direction === 'desc') ? 'asc' : 'desc';
                newColumns[0] = byColumn;
                newDirections[0] = newDirection;
            };
        }
        else {
            newDirection = (this.state.sort.direction === 'desc') ? 'asc' : 'desc';
            
            if (sortByIndex !== -1) {
                newColumns[sortByIndex] = byColumn;
                newDirections[sortByIndex] = newDirection;
            }
            else {
                newColumns[0] = byColumn;
                newDirections[0] = newDirection;
            }
        }

        this.setState({
            sort: {...this.state.sort,
                column: byColumn,
                direction: newDirection,
                columns: newColumns,
                directions: newDirections
            }
        });
    }

    static getDerivedStateFromProps(props, state) {
        //console.log("getDerivedStateFromProps: ", props, state);

        if (props.selectedMisbehaviour.misbehaviour && props.selectedMisbehaviour.misbehaviour.id &&
                props.selectedMisbehaviour.misbehaviour.id !== state.selected) {
            //console.log("Sorting: changing selected misbehaviour: " + state.selected 
            //        + " to " + props.selectedMisbehaviour.misbehaviour.id);
            return {
                ...state,
                selected: props.selectedMisbehaviour.misbehaviour.id
            }
        }

        let newMisBehav = fromJS(props.misbehaviours);

        //console.log("state.selectedThreat:", state.selectedThreat);
        //console.log("props.selectedThreat:", props.selectedThreat);
        //console.log("misBehavImmutable:", misBehavImmutable);
        //console.log("newMisBehav:", newMisBehav);

        if (props.selectedThreat !== state.selectedThreat) {
            misBehavImmutable = newMisBehav;
            lastFilterState = {
                filter: state.filter,
                sort: state.sort
            };
            //console.log("selected threat changed");
            return {
                ...state,
                ...ModelMisBehavPanel.getUpdatedState(props, state)
            };
        }
        else if (!newMisBehav.equals(misBehavImmutable)) {
            misBehavImmutable = newMisBehav;
            lastFilterState = {
                filter: state.filter,
                sort: state.sort
            };
            //console.log("misbehaviours changed");
            return {
                ...state,
                ...ModelMisBehavPanel.getUpdatedState(props, state)
            };
        }
        else if (!_.every([lastFilterState], { filter: state.filter, sort: state.sort })) {
            lastFilterState = {
                filter: state.filter,
                sort: state.sort
            };
            //console.log("filter or sort changed");
            return {
                ...state,
                ...ModelMisBehavPanel.getUpdatedState(props, state)
            }
        }

        // null represents no change to state
        //console.log("nothing changed");
        return null;
    }

    render() {
        //console.log("render: ", misBehavImmutable.count(), this.state.tableData);
        
        //Get current settings for sort column/direction. Use defaults if undefined
        let column = this.state.sort.column ? this.state.sort.column : this.state.sortDefaults.column;
        let direction = this.state.sort.direction ? this.state.sort.direction : this.state.sortDefaults.direction;
        
        //Set initial caret classes (i.e. no caret by default)
        let carets = {
            "name": "fa",
            "asset": "fa",
            "impactValue": "fa",
            "likelihoodValue": "fa",
            "riskValue": "fa"
        };
        
        //Determine caret style for the current sort direction
        let caretClass = (direction === 'desc') ? "fa fa-caret-down" : "fa fa-caret-up";
        
        //Set the correct caret style on the main sort column
        carets[column] = caretClass;
        
        let headersDiv = <div key={0} className='row head-sortable'>
            <span className="col-xs-3"
                style={{ minWidth: "50px" }}
                onClick={() => { this.sortByColumns("name") }}>
                Consequence
                <span className={carets['name']}
                    style={{
                        display: carets['name'] !== "fa" ? "inline-block" : "none",
                    }} />
            </span>
            <span className="col-xs-3"
                style={{ minWidth: "50px" }}
                onClick={() => { this.sortByColumns("asset") }}>
                Asset
                <span className={carets['asset']}
                    style={{
                        display: carets['asset'] !== "fa" ? "inline-block" : "none",
                    }} />
            </span>
            <span className="col-xs-2 impact"
                onClick={() => { this.sortByColumns("impactValue") }}>
                <span className="head-text">Direct Impact</span>
                <span className={carets['impactValue']}
                    style={{
                        display: carets['impactValue'] !== "fa" ? "inline-block" : "none",
                    }} />
            </span>
            <span className="col-xs-1 likelihood"
                onClick={() => { this.sortByColumns("likelihoodValue") }}>
                <span className="head-text">Likelihood</span>
                <span className={carets['likelihoodValue']}
                    style={{
                        display: carets['likelihoodValue'] !== "fa" ? "inline-block" : "none",
                    }} />
            </span>
            <span className="col-xs-1 risk"
                onClick={() => { this.sortByColumns("riskValue") }}>
                <span className="head-text">Direct Risk</span>
                <span className={carets['riskValue']}
                    style={{
                        display: carets['riskValue'] !== "fa" ? "inline-block" : "none",
                    }} />
            </span>
        </div>;

        let showFiltersDiv = null;
        // TODO: not clear that this is the right test as it is this.state.tableData that is rendered
        if (misBehavImmutable.count() !== 0) {
            if (!this.state.filter.showFilters) {
                showFiltersDiv = <div>
                    <a className="filter show-filter"
                    onClick={() => {
                        this.setState({
                            ...this.state,
                            filter: {
                                ...this.state.filter,
                                showFilters: true
                            }
                        })
                    }}>Show filters
                    </a>
                    <a className="filter reset-filter"
                    onClick={() => {this.sortByColumns("")}}>Reset sort
                    </a>
                </div>
            } else if (this.state.filter.showFilters) {
                showFiltersDiv = <div>
                    <a className="filter show-filter"
                        onClick={() => {
                            this.setState({
                                ...this.state,
                                filter: {
                                    ...this.state.filter,
                                    showFilters: false
                                }
                            })
                        }}>Hide filters
                    </a>
                    <a className="filter reset-filter"
                        onClick={() => {
                            //reset all filters, but keep filters open
                            this.setState({
                                ...this.state,
                                filter: {
                                    ...this.defaultFilterState,
                                    showFilters: true
                                }
                            });
                        }}>Reset filters
                    </a>
                    <a className="filter reset-filter"
                        onClick={() => {this.sortByColumns("")}}>Reset sort
                    </a>
                </div>
            }
        }
        return <div className="misbehaviours detail-list">
            <div className="container-fluid">
            <Form>
                <FormGroup>
                    {showFiltersDiv}
                </FormGroup>
                {this.state.filter.showFilters && <Fragment>
                    <FormGroup>
                        <InputGroup>
                            <InputGroup.Addon><i className="fa fa-lg fa-filter"/></InputGroup.Addon>
                            <FormControl 
                                type="text"
                                placeholder="Filter consequences by name"
                                id="misbehaviours-filter"
                                value={ this.state.filter.search }
                                onChange={(e) => {
                                    let searchText = e.nativeEvent.target.value;
                                    this.setState({
                                        ...this.state,
                                        filter: {
                                            ...this.state.filter,
                                            search: searchText
                                        }
                                    })
                                }}
                                // need to prevent the Form being submitted when Return is pressed
                                onKeyPress={(e) => { e.key === 'Enter' && e.preventDefault(); }}
                            />
                        </InputGroup>
                    </FormGroup>
                    <FormGroup>
                        <Checkbox
                            checked={ this.state.filter.hideInferredAssets }
                            onChange={() => {
                                this.setState({
                                    ...this.state,
                                    filter: {
                                        ...this.state.filter,
                                        hideInferredAssets: !this.state.filter.hideInferredAssets
                                    }
                                })
                            }}>
                            Hide inferred assets
                        </Checkbox>
                    </FormGroup>
                </Fragment>}
            </Form>

            {misBehavImmutable.count() > 0 && this.renderMisbehaviours(this.state.tableData, headersDiv)}

            </div>
        </div>;
    }

    renderMisbehaviours(tableData, headersDiv) {
        let levels = this.state.viewedLevels;

        let tableDivs = [];
        _.forEach(tableData, ({ uri, name, asset }) => {
            let misbehaviour = this.props.misbehaviours[uri];
            let misbehaviourId = misbehaviour["id"];

            //is misbehaviour selected?
            let selected = this.state.selected === misbehaviourId;
            
            //is misbehaviour visible?
            let visible = misbehaviour["visible"];
            
            if (visible !== undefined && !visible) {
                //console.log("Hiding misbehaviour: ", misbehaviour);
                return;
            }

            let impact = misbehaviour["impactLevel"];
            let likelihood = misbehaviour["likelihood"];
            let risk = misbehaviour["riskLevel"];
            let updating = this.state.updating[uri];

            let likelihoodRender = getRenderedLevelText(levels, likelihood);
            let riskRender = getRenderedLevelText(levels, risk);

            //show revert impact level button only if the level has been asserted by the user
            let showRevertButton = misbehaviour.impactLevelAsserted;

            tableDivs.push(<div key={misbehaviourId} className={
                `row detail-info ${
                    selected === true ? "selected-row" : "row-hover"
                }`
            }>
                    <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                                    trigger={["hover"]}
                                    overlay={
                                        <Tooltip id={`misbehaviour-${misbehaviourId}-tooltip`}
                                                 className={"tooltip-overlay"}>
                                            { misbehaviour.description ? misbehaviour.description :
                                                "View consequence" + (selected ? " (selected)" : "") }
                                        </Tooltip>
                                    }>
                        <span onClick={() => {
                            this.openMisbehaviourExplorer(misbehaviour);
                            this.props.dispatch(bringToFrontWindow("misbehaviourExplorer"));
                        }}
                              className={"col-xs-3 misbehaviour" + (selected ? "" : " clickable")}
                              style={{minWidth: "50px"}}>
                            { name }
                        </span>
                    </OverlayTrigger>
                    <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="left"
                                    trigger={["hover"]}
                                    overlay={
                                        <Tooltip id={`misbehaviour-asset-${misbehaviourId}-tooltip`}
                                                 className={"tooltip-overlay"}>
                                            {`${asset}`}
                                        </Tooltip>
                                    }>
                        <span className={"col-xs-3"}
                              style={{minWidth: "50px", textOverflow: "ellipsis", whiteSpace: "nowrap"}}>
                            { asset }
                        </span>
                    </OverlayTrigger>
                    <span className="col-xs-2 impact">
                        <FormControl
                            disabled={!this.props.authz.userEdit}
                            componentClass="select"
                            className="impact-dropdown level"
                            id={uri}
                            value={impact != null ? impact.uri : ""}
                            style={{ backgroundColor: getLevelColour(levels, impact) }}
                            onChange={this.valueChanged}
                            ref="select-initial-tw">
                            {levels.map((level, index) =>
                                <option key={index + 1}
                                    value={level.uri}
                                    style={{ backgroundColor: getLevelColour(levels, level) }}>
                                    {level.label}
                                </option>
                            )};
                        </FormControl>
                        {updating ? <i className="fa fa-spinner fa-pulse fa-lg fa-fw" /> : null}
                        &nbsp;
                        <span style={{cursor: "pointer", display: showRevertButton ? "inline-block" : "none"}} className="fa fa-undo undo-button" 
                            onClick={((e) => {
                                this.onClickRevertImpactLevel(uri);
                            })}
                        />
                    </span>
                    <span className="likelihood col-xs-1">
                        { likelihoodRender }
                    </span>
                    <span className="risk col-xs-1">
                        { riskRender }
                    </span>
                </div>
            );
        });

        return (
            <div>
                {headersDiv}
                <PagedPanel panelData={tableDivs}
                            pageSize={10}
                            context={`model-misbehaviours-${this.props.modelId}`}
                            noDataMessage={"No consequences found"}/>
            </div>
        )
    }

    valueChanged(e) {
        let misbehaviourUri = e.target.id;
        let selectedLevelUri = e.target.value;

        let selectedLevel = this.props.levels.find((level) => level["uri"] === selectedLevelUri);

        let updating = this.state.updating;
        updating[misbehaviourUri] = true;

        this.setState({
            ...this.state,
            updating: updating
        });

        let misbehaviour = this.props.misbehaviours[misbehaviourUri];

        let updatedMisbehaviour = {
            id: misbehaviour["id"],
            uri: misbehaviour["uri"],
            impactLevel: {uri: selectedLevelUri}
        };

        this.props.dispatch(updateMisbehaviourImpact(this.props.modelId, updatedMisbehaviour));
    }

    onClickRevertImpactLevel(misbehaviourUri) {
        //console.log("onClickRevertImpactLevel:", misbehaviourUri);
        if (misbehaviourUri) {
            //set updating flag for this impact label
            let updatedUpdating = {...this.state.updating};
            updatedUpdating[misbehaviourUri] = true;

            this.setState({
                ...this.state,
                updating: updatedUpdating
            });

            let misbehaviour = this.props.misbehaviours[misbehaviourUri];

            let ms = {
                id: misbehaviour["id"],
                uri: misbehaviour["uri"]
            }

            this.props.dispatch(revertMisbehaviourImpact(this.props.modelId, ms));
        }
    }

    openMisbehaviourExplorer(misbehaviour) {
        //console.log("Displaying root causes for misbehaviour: ", misbehaviour);

        // Now we get root causes for a specified misbehaviour (rather than a threat)
        //console.log("misbehaviourUri:", misbehaviour.misbehaviours[0].id);
        let misbehaviourUri = misbehaviour.uri; //in theory all misbehaviours in group have the same id
        //console.log(misbehaviourUri);
        let updateRootCausesModel = true;
        //console.log("dispatch getRootCauses...");
        this.props.dispatch(getRootCauses(this.props.modelId, misbehaviour));
    }

}

let mapStateToProps = function (state) {
    return {
        assets: state.modeller.model.assets,
        modelId: state.modeller.model["id"],
        levels: state.modeller.model.levels ? state.modeller.model.levels["ImpactLevel"] : {},
        misbehaviours: state.modeller.model.misbehaviourSets,
        selectedMisbehaviour: state.modeller.selectedMisbehaviour,
        rightSidePanelWidth: state.modeller.view.rightSidePanelWidth,
        authz: state.modeller.authz,

    };
};

//N.B. adjustAssetNameSizes is set to false if this panel is in Threat Explorer
//as the asset label lengths are currently baesd on the rightSidePanelWidth (see above),
//which is only in the ModelSummary.
ModelMisBehavPanel.propTypes = {
    panelType: PropTypes.string,
    selectedMisbehaviours: PropTypes.array,
    selectedThreat: PropTypes.string,
    adjustAssetNameSizes: PropTypes.bool,
    authz: PropTypes.object,

};

export default connect(mapStateToProps)(ModelMisBehavPanel);
