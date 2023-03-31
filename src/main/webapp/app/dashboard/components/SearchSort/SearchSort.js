import React from 'react';
import {
    Badge,
    Button,
    Checkbox, DropdownButton,
    FormGroup,
    MenuItem
} from "react-bootstrap";
import { getDomains } from "../../actions/api";
import ImportModelModal from "../popups/ImportModelModal";
import NewModelModal from "../popups/NewModelModal";
import './SearchSort.scss';



class SearchSort extends React.Component{

    constructor(props) {
        super(props);
       // this.props.dispatch(getDomains());
        this.getOntologies = this.getOntologies.bind(this);
        this.openNewModelModal = this.openNewModelModal.bind(this);
        this.onHideImportModal = this.onHideImportModal.bind(this);
        this.handleCheck = this.handleCheck.bind(this);
       // console.log("this.props");
       // console.log(this.props.ontologies);
        this.state = {
            modal: {
                importModel: false,
                newModel: false
            }
        }
    }

    render() {
        let {filter, ontologies} = this.props;

        return (
            <div className={"search-sort-main"}>
                <div className="search-sort-main2">
                    <DropdownButton
                        bsSize="small"
                        title={"Sort by: " + this.sortTitle(filter.sortMethod)}
                        id="dropdown-size-small"
                        className={"search-sort-btn"}
                        onSelect={(e) => this.handleSelect(e)}
                    >
                        <MenuItem eventKey="modelName">{this.sortTitle("modelName")}</MenuItem>
                        <MenuItem eventKey="owner">{this.sortTitle("owner")}</MenuItem>
                        <MenuItem eventKey="dateModified">{this.sortTitle("dateModified")}</MenuItem>
                        <MenuItem eventKey="dateCreated">{this.sortTitle("dateCreated")}</MenuItem>
                        <MenuItem eventKey="domainModel">{this.sortTitle("domainModel")}</MenuItem>

                    </DropdownButton>
                    <Button bsSize="small" onClick={() => this.swap()} className={"search-sort-btn"}>
                        <i className={"fa " + (filter.order ? "fa-long-arrow-up" : "fa-long-arrow-down")}/>
                    </Button>
                    {ontologies.length > 1 ?
                        <DropdownButton
                            className={"search-sort-btn"}
                            bsSize="small"
                            title="Filter knowledgebase"
                            id="dropdown-size-extra-small"
                        >
                            {filter.domainModelFilters.map((ontology, i) => {
                                return (<div key={i} className={"menu-item"} onClick={(e) => e.stopPropagation()}>
                                    <div className={"option-para"}>
                                        <p className={"search-sort-para"}>{ontology.name}</p>
                                    </div>
                                    <div className={"option-div"}>
                                        <FormGroup>
                                            <Checkbox checked={ontology.checked} onChange={() =>
                                                this.handleCheck(i)
                                            }>
                                            </Checkbox>
                                        </FormGroup>
                                    </div>
                                </div>)
                            })}
                        </DropdownButton>
                        :
                        <div></div>
                    }
                    <div className={"search-sort-search-main"}>
                        <div>
                            <input className={"search-sort-btn search-sort-input"} type="text" id="usr" placeholder={"Search"} onChange={(e) => this.props.handleFilterText(e.nativeEvent.target.value)}/>
                        </div>
                    </div>
                    <Button bsStyle="primary"
                            bsSize="small"
                            onClick={() => this.openNewModelModal()}
                            className={"search-sort-btn2"}
                    >
                        <div><span className="fa fa-plus"/>{" Create new model"}</div>
                    </Button>
                    <Button onClick={() => this.setState({...this.state, modal: {importModel: true, newModel: false}})}
                            bsStyle="primary"
                            className="test1"
                            bsSize="small"
                    >
                        <span className="fa fa-upload"/>{" Import model"}
                    </Button>
                    <ImportModelModal show={this.state.modal.importModel}
                                      onHideImportModal={this.onHideImportModal}
                                      dispatch={this.props.dispatch}
                                      upload={this.props.upload}/>
                    <NewModelModal show={this.state.modal.newModel && !this.props.loading.ontologies}
                                   onHide={() => this.setState({...this.state, modal: {...this.state.modal, newModel: false}})}
                                   ontologies={this.props.ontologies}
                                   loading={this.props.loading}
                                   dispatch={this.props.dispatch}/>
                </div>
                <div className={"search-sort-tags"}>
                    {filter.domainModelFilters.map((tag,i) => {
                        if(tag.checked){
                            return <Badge key={i} variant="info" className={"search-sort-tag"} >
                                {tag.name}
                                <Button bsSize="small" className={"cross fa fa-times fa-lg"} onClick={() => this.handleCheck(i)}> <span><i className={""}></i></span></Button>
                            </Badge>;
                        } else {
                            return null;
                        }
                    })}
                </div>
            </div>
        );

    }

    sortTitle(sort) {
        switch(sort) {
            case "dateModified":
                return "Date modified";
            case "dateCreated":
                return "Date created";
            case "modelName":
                return "Model name";
            case "owner":
                return "Owner";
            case "domainModel":
                return "Knowledgebase";
        }
    }

    handleCheck(index) {
        let temp = this.props.filter.domainModelFilters;
        temp[index].checked = !temp[index].checked;
        this.props.changeFilters(temp);
    }


    handleSelect(eventKey) {
        this.props.handleSortClick(eventKey);
    }

    getOntologies(ontologies) {
        if (jQuery.isEmptyObject(ontologies)) {
            //console.warn("WARNING: no knowledgebases are currently available");
            return [];
        }

        return ontologies;
    }

    swap = () => {
        this.props.handleOrder();
    }

    openNewModelModal() {
        console.log("openNewModelModal (will open once knowledgebases are loaded)");
        //N.B. modal will actually open when knowledgebases have finished loading (this.props.loading.ontologies == false)
        this.setState({...this.state, modal: {importModel: false, newModel: true}});
        this.props.dispatch(getDomains());
    }

    onHideImportModal() {
        //console.log("onHideImportModal");
        this.setState({
            ...this.state,
            modal: {...this.state.modal, importModel: false}
        });
    }
}


export default SearchSort;
