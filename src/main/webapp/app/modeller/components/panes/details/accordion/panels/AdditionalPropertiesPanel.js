import React from "react";
import PropTypes from "prop-types";
import {
    retrieveMetaData,
    updateMetaData
} from "../../../../../actions/ModellerActions";
import {Button, OverlayTrigger, Tooltip} from "react-bootstrap";
import * as Constants from "../../../../../../common/constants";

class AdditionalPropertiesPanel extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            newRow : false,
            newestKey : null,
            newestValue : null,
            tableData : [],
            editingItem: [],
            editingStarted: false,
            editingKey : null,
            editingValue : null
        };

        this.getMetaData();
    }

    componentDidUpdate(nextProps) {
        // console.log("componentDidUpdate");
        
        if (nextProps.asset.id !== this.props.asset.id){
            this.getMetaData();
        }
    }

    static getDerivedStateFromProps(props){
        // console.log("getDerivedStateFromProps");
        
        if (!props.asset.metaData)
            return null;

        let metaDataFromProps = props.asset.metaData;
        let tableDataArray = [];

        metaDataFromProps.forEach((item =>
                tableDataArray.push({"key": item.key, "value" : item.value, "label" : item.label})
        ));

        tableDataArray.sort(function(a,b){
            if (a.key < b.key){return -1}
            if (a.key > b.key){return 1}
            return 0;
        });

        return {
            tableData: tableDataArray
        }
    }

    getMetaData(){
        // console.log("getMetaData()");
        this.props.dispatch(retrieveMetaData(this.props.modelId, this.props.asset));
    }

    putMetaData(tableData){
        // console.log("putMetaData()");
        this.props.dispatch(updateMetaData(this.props.modelId, this.props.asset, tableData));
    }

    onClickSubmitNewItem(){
        // console.log("onClickSubmitNewItem");

        if (this.state.newestKey !== null && this.state.newestValue !== null){
            let tableDataArray = this.state.tableData;

            tableDataArray.push({key: this.state.newestKey, value: this.state.newestValue});

            this.putMetaData(tableDataArray);

        }

        this.setState({
            ...this.state,
            newRow: false,
            newestKey: null,
            newestValue: null
        });

    }

    onClickCancelNewItem(){
        // console.log("onClickCancelNewItem");

        this.setState({
            ...this.state,
            newRow: false
        });

        // console.log(this.state.newRow)
    }

    onChangeNewestKey(e){
        this.setState({
            ...this.state,
            newestKey : e.target.value
        });
    }

    onChangeNewestValue(e){
        this.setState({
            ...this.state,
            newestValue : e.target.value
        });
    }

    newRow(){

        if (this.state.newRow){
            return (
                <tr>
                    <td>
                        <input type="text" id="newestKey" name="newestKey" size="10"
                               onChange={(e) => {this.onChangeNewestKey(e)}}/>
                    </td>
                    <td>
                        <input type="text" id="newestValue" name="newestValue" size="10"
                               onChange={(e) => {this.onChangeNewestValue(e)}}/>
                    </td>
                    <td>
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                        overlay={
                                            <Tooltip id={`inc-rel-submit-tooltip`} className={"tooltip-overlay"}>
                                                <strong>Submit</strong>
                                            </Tooltip>
                                        }>
                                        <span style={{color:"green", cursor: "pointer"}} className="menu-close fa fa-check" onClick={(e) =>
                                        {
                                            e.nativeEvent.target.className = "fa fa-cog fa-fw fa-spin";
                                            this.onClickSubmitNewItem();
                                        }}/>
                        </OverlayTrigger>
                    </td>
                    <td>
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                        overlay={
                                            <Tooltip id={`inc-rel-cancel-tooltip`} className={"tooltip-overlay"}>
                                                <strong>Cancel</strong>
                                            </Tooltip>
                                        }>
                                        <span style={{color:"red", cursor: "pointer"}} className="fa fa-times-circle" onClick={(e) =>
                                        {
                                            e.nativeEvent.target.className = "fa fa-cog fa-fw fa-spin";
                                            this.setState({
                                                ...this.state,
                                                newRow: false
                                            });
                                        }}/>
                        </OverlayTrigger>
                    </td>
                </tr>
            )
        }

    }

    onClickEditSubmit(item){
        // console.log("onClickEditSubmit()");

        this.setState({
            ...this.state,
            editingItem: []
        });

        let tableDataArray = this.state.tableData;
        const elementsIndex = this.state.tableData.findIndex(element => element.label === item.label);

        tableDataArray[elementsIndex] = {...tableDataArray[elementsIndex], key: this.state.editingKey, value : this.state.editingValue};

        this.putMetaData(tableDataArray);
    }

    onClickEditCancel(){
        this.setState({
            ...this.state,
            editingKey : null,
            editingValue: null,
            editingItem: []
        });
    }

    onChangeEditKey(e){
        this.setState({
            ...this.state,
            editingKey : e.target.value
        });
    }

    onChangeEditValue(e){
        this.setState({
            ...this.state,
            editingValue : e.target.value
        });
    }

    editingRow(item){

        return (
            <tr key={item.label}>
                <td>
                    <b>
                        <input type="text" id="newestKey" name="newestKey" size="10" defaultValue={this.state.editingKey}
                               onChange={(e) => {this.onChangeEditKey(e)}}/>
                    </b>
                </td>
                <td>
                    <input type="text" id="newestValue" name="newestValue" size="10" defaultValue={this.state.editingValue}
                           onChange={(e) => {this.onChangeEditValue(e)}}/>
                </td>
                <td>
                    <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                    overlay={
                                        <Tooltip id={`inc-rel-cancel-tooltip`} className={"tooltip-overlay"}>
                                            <strong>Submit</strong>
                                        </Tooltip>
                                    }>
                                    <span style={{color:"green", cursor: "pointer"}} className="menu-close fa fa-check" onClick={((e) => {
                                        e.nativeEvent.target.className = "fa fa-cog fa-fw fa-spin";
                                        this.onClickEditSubmit(item);})}/>
                    </OverlayTrigger>
                </td>
                <td>
                    <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                                    overlay={
                                        <Tooltip id={`inc-rel-cancel-tooltip`} className={"tooltip-overlay"}>
                                            <strong>Cancel</strong>
                                        </Tooltip>
                                    }>
                        <span style={{color:"red", cursor: "pointer"}} className="fa fa-times-circle" onClick={((e) => {
                            e.nativeEvent.target.className = "fa fa-cog fa-fw fa-spin";
                            this.onClickEditCancel(item);})}/>
                    </OverlayTrigger>
                </td>
            </tr>
        )

    }

    onClickEdit(item){
        // console.log("onClickEdit()");

        this.setState({
            ...this.state,
            editingItem : item,
            editingKey : item.key,
            editingValue : item.value,
        });

    }

    onClickDelete(item){
        // console.log("onClickDelete()");

        let tableDataArray = this.state.tableData;

        tableDataArray = tableDataArray.filter( function (obj){
            return obj.label !== item.label;
        });

        this.putMetaData(tableDataArray);
    }

    nonEditingRow(item){

        return (
            <tr key={item.label}>
                <td>
                    <b>
                        {item.key}
                    </b>
                </td>
                <td>
                    {item.value}
                </td>
                <td>
                    {this.props.authz.userEdit ?
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                            overlay={
                                <Tooltip id={`inc-rel-cancel-tooltip`} className={"tooltip-overlay"}>
                                    <strong>Edit</strong>
                                </Tooltip>
                            }>
                            <span style={{ cursor: "pointer" }} className="fa fa-pencil" onClick={((e) => {
                                e.nativeEvent.target.className = "fa fa-cog fa-fw fa-spin";
                                this.onClickEdit(item);
                            })} />
                        </OverlayTrigger>
                        :
                        <div></div>
                    }
                </td>
                <td>
                    {this.props.authz.userEdit ?
                        <OverlayTrigger delayShow={Constants.TOOLTIP_DELAY} placement="top"
                            overlay={
                                <Tooltip id={`inc-rel-cancel-tooltip`} className={"tooltip-overlay"}>
                                    <strong>Delete</strong>
                                </Tooltip>
                            }>
                            <span style={{ color: "red", cursor: "pointer" }} className="fa fa-trash" onClick={((e) => {
                                e.nativeEvent.target.className = "fa fa-cog fa-fw fa-spin";
                                this.onClickDelete(item);
                            })} />
                        </OverlayTrigger>
                        :
                        <div></div>
                    }
                </td>
            </tr>
        )

    }

    createTable(){
        return (
            this.state.tableData.map(((item)  =>{
                if (item.label === this.state.editingItem.label){
                    return this.editingRow(item);
                } else {
                    return this.nonEditingRow(item);
                }
            }))
        )
    }

    showTable(){

        if (this.state.tableData.length > 0){
            return (
                <table className="table table-hover table-sm table-condensed">
                    <thead className="thead-light">
                    <tr>
                        <th>Key</th>
                        <th>Value</th>
                        <th/>
                        <th/>
                    </tr>
                    </thead>
                    <tbody>
                    {this.createTable()}
                    {this.newRow()}
                    </tbody>
                </table>
            );
        } else {
            return (
                //No current additional properties but '+ New' pressed, creates a new table
                this.state.newRow?(
                    <table className="table table-hover table-sm table-condensed">
                        <thead className="thead-light">
                        <tr>
                            <th>Key</th>
                            <th>Value</th>
                            <th/>
                            <th/>
                        </tr>
                        </thead>
                        <tbody>
                        {this.newRow()}
                        </tbody>
                    </table>
                ):<span>No additional properties found</span>)
        }

    }

    render() {
        // console.log("render()");
        // console.log("Render - TableData : ", this.state.tableData);

        return (
            <div className="asset-controls detail-list">
                <div className="container-fluid">
                    {this.showTable()}
                    {this.props.authz.userEdit ?
                            <div style={{ display: "flex", justifyContent: "center" }}>
                                <Button bsStyle="primary"
                                    bsSize="small"
                                    onClick={() => this.setState({
                                        ...this.state,
                                        newRow: true
                                    })}>
                                    + New
                                </Button>
                            </div>
                            :
                            <div></div>
                    }
                </div>

            </div>
        );
    }
}

AdditionalPropertiesPanel.propTypes = {
    modelId: PropTypes.string,
    asset: PropTypes.object,
    dispatch: PropTypes.func
};

export default AdditionalPropertiesPanel;
