import PropTypes from "prop-types";
import React, { Component } from "react";
import { Col, Row } from "react-bootstrap";
import ModelItem from "../modelItem/ModelItem";
import './ModelList.scss';

class ModelList extends Component {
    constructor(props) {
        super(props);
    }

    render() {
        let {models, filter, dispatch, ontologies, user} = this.props;

        models.map(model => {
            model.name = model.name ? model.name : "null";
            return model;
        });

        let domainToFilter = []

        for (let i = 0; i < filter.domainModelFilters.length; i++) {
            if(filter.domainModelFilters[i].checked){
                domainToFilter[i] = filter.domainModelFilters[i].name.toLowerCase();
            }
        }
        
        return (
            <div className="minimum">
                <Row className="model-list-row">
                    <Col xs={2} md={2}
                        className="column-header-clickable"
                        onClick={() => this.handleSelect("modelName")}
                    >
                        <span>Name </span>
                        {this.props.filter.sortMethod === "modelName"
                            ? (this.props.filter.order ? <span className="fa fa-long-arrow-up"></span> : <span className="fa fa-long-arrow-down"></span>)
                            : null
                        }
                    </Col>
                    <Col
                        xs={3} md={3}
                    >
                        <span>Description </span>
                    </Col>
                    <Col
                        xs={1} md={1}
                        className="column-header-clickable owner"
                        onClick={() => this.handleSelect("owner")}
                    >
                        <span>Owner </span>
                        {this.props.filter.sortMethod === "owner"
                            ? (this.props.filter.order ? <span className="fa fa-long-arrow-up"></span> : <span className="fa fa-long-arrow-down"></span>)
                            : null
                        }
                    </Col>
                    <Col
                        xs={1} md={1}
                        className="column-header-clickable"
                        onClick={() => this.handleSelect("dateModified")}
                    >
                        <span>Last modified </span>
                        {this.props.filter.sortMethod === "dateModified"
                            ? (this.props.filter.order ? <span className="fa fa-long-arrow-up"></span> : <span className="fa fa-long-arrow-down"></span>)
                            : null
                        }
                    </Col>
                    <Col
                        xs={1} md={1}
                        className="column-header-clickable"
                        onClick={() => this.handleSelect("domainModel")}
                    >
                        <span>Knowledgebase </span>
                        {this.props.filter.sortMethod === "domainModel"
                            ? (this.props.filter.order ? <span className="fa fa-long-arrow-up"></span> : <span className="fa fa-long-arrow-down"></span>)
                            : null
                        }
                    </Col>
                    <Col
                        xs={2} md={2}
                        className="column-header-clickable version"
                        onClick={() => this.handleSelect("domainVersion")}
                    >
                        <span>KB Version </span>
                        {this.props.filter.sortMethod === "domainVersion"
                            ? (this.props.filter.order ? <span className="fa fa-long-arrow-up"></span> : <span className="fa fa-long-arrow-down"></span>)
                            : null
                        }
                    </Col>
                </Row>
                <br />
                <div className="model-list-container">
                    <div className={"model-list"}>
                        {(models.length !== 0) ? models
                            .sort((a, b) => {
                                let nameA = a["name"];
                                let nameB = b["name"];
                                let createdA = a["created"];
                                let createdB = b["created"];
                                let modifiedA = a["modified"];
                                let modifiedB = b["modified"];
                                let ownerA = a["userId"];
                                let ownerB = b["userId"];
                                let domainA = a["domainGraph"].replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-", "");
                                let domainB = b["domainGraph"].replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-", "");
                                switch (filter.sortMethod) {
                                    case "dateModified":
                                        return (filter.order ? this.compare(modifiedA,modifiedB) : this.compare(modifiedB,modifiedA));
                                    case "dateCreated":
                                        return (filter.order ? this.compare(createdA,createdB) : this.compare(createdB,createdA));
                                    case "modelName":
                                        return (filter.order ? nameA.localeCompare(nameB) : nameB.localeCompare(nameA) );
                                    case "owner":
                                        return (filter.order ? ownerA.localeCompare(ownerB) : ownerB.localeCompare(ownerA));
                                    case "domainModel":
                                        console.log("done");
                                        return (filter.order ? domainA.localeCompare(domainB) : domainB.localeCompare(domainA));
                                }
                            }).filter((a) => {
                                if(domainToFilter.length === 0){
                                    return true;
                                } else if (domainToFilter.includes(a.domainGraph.replace("http://it-innovation.soton.ac.uk/ontologies/trustworthiness/domain-", ""))){
                                    return true;
                                } else {
                                    return false;
                                }
                            })
                            .filter((a) => {if (a.name.toLowerCase().includes(filter.filterText.toLowerCase()) || a.description.toLowerCase().includes(filter.filterText.toLowerCase())
                                || a.userId.toLowerCase().includes(filter.filterText.toLowerCase())) {
                                return (a.name.toLowerCase().indexOf(filter.filterText.toLowerCase()) > -1) || (a.description.toLowerCase().indexOf(filter.filterText.toLowerCase()) > -1)
                                    || (a.userId.toLowerCase().indexOf(filter.filterText.toLowerCase()) > -1);
                            } else return false;}).map((model, index) => {
                                return <ModelItem key={index} model={model} models={models} ontologies={ontologies} dispatch={dispatch} user={user}/>;
                            }) : <p>You don't have any models. Use the Create or Import buttons.</p>}
                    </div>
                </div>
            </div>
        );
    }

    compare(a,b){
        if(a < b) {
            return -1;
        }
        else if(a > b){
            return 1;
        } else {
            return 0;
        }
    }

    handleSelect (eventKey){
        this.props.handleSortClick(eventKey);
    }
}

ModelList.propTypes = {
    models: PropTypes.array,
    filter: PropTypes.object
};

export default ModelList;
