import React, {Component} from "react";
import PropTypes from "prop-types";
import {Alert, Button, ButtonGroup, Col, Row} from "react-bootstrap";
import {connect} from "react-redux";
import {getUser, getModels, getModelsForUser, getDomains} from "../../actions/api";
import ModelList from "../modelList/ModelList";
import "./Dashboard.scss";
import RecentList from "../RecentList/RecentList";
import SearchSort from "../SearchSort/SearchSort";

class Dashboard extends Component {

    constructor(props) {
        super(props);
    }

    componentWillMount() {
        this.setState({
            filter: {
                viewLevel: "list",
                sortMethod: "dateModified",
                filterText: "",
                order: true,
                domainModelFilters: []
            }
        });

        this.changeViewLevel = this.changeViewLevel.bind(this);
        this.changeSortMethod = this.changeSortMethod.bind(this);
        this.changeFilterText = this.changeFilterText.bind(this);
        this.changeOrdering = this.changeOrdering.bind(this);
        this.changeDomainFilter = this.changeDomainFilter.bind(this);
        this.changeOrderingByColumn = this.changeOrderingByColumn.bind(this);

        this.props.dispatch(getDomains());

        // The userid is stored in a meta tag for simplicity.
        let userid = $("meta[name='_userid']").attr("content");
        
        if (userid !== undefined) {
            console.log("Getting user details for userid: ", userid);
            this.props.dispatch(getUser(userid));
            console.log("Getting models for userid: ", userid);
            this.props.dispatch(getModelsForUser(userid));
        }
        else {
            //console.log("Getting my models");
            this.props.dispatch(getModels());
        }
    }

    componentDidUpdate(prevProps) {
        let {ontologies} = this.props;

        if (prevProps.loading.ontologies && !this.props.loading.ontologies) {
            if (ontologies.length == 0) {
                let userRole = this.props.auth.user.role;
                if (userRole === 1) { //admin only
                    alert("No knowledgebases available! Will redirect to Knowledgebase Manager..");
                    let kbManagerURL = process.env.config.END_POINT + "/domain-manager";
                    console.log("Redirecting to: " + kbManagerURL);
                    window.location.replace(kbManagerURL);
                }
                else { //standard users
                    alert("No knowledgebases available! Please contact administrator.");
                }
            }    
        }

        if (!this.ontologyNamesEqual(prevProps.ontologies, ontologies)) {
            let temp = []
            for (let i = 0; i < ontologies.length; i++) {
                temp[i] = {
                    name: ontologies[i].name,
                    checked: false
                }
            }
            this.setState({
                filter: {
                    ...this.state.filter,
                    domainModelFilters: temp
                }
            });
        }
    }

    render() {
        let {user, models, ontologies, upload, auth, dispatch, loading} = this.props;
        let {filter} = this.state;
        //console.log("user:", user); //specific user (e.g. if Admin user is viewing dashboard fir another user)
        let bannerTitle = ((user && user.username) ? " (" + user.username + ")" : "");

        return (
            <div id="dashboard-popups" className="dashboard content" >
                <div className="dashboard-container">
                    <div className={"minimum-dash"}>
                        <h1>Dashboard {bannerTitle}</h1>
                        {auth.user !== "" && auth.user !== null && auth.user.username === "test" &&
                        <Alert bsStyle="danger">
                            You are logged in as TEST. This account should only be used for unit testing.
                            Please use your account, or request one from an administrator.</Alert>}
                        <RecentList models={models} user={auth.user} dispatch={dispatch}/>
                    </div>
                    <div className={"minimum-dash"}>
                        <Row>
                            <Col xs={4} lg={4}>
                                <h3 className={"models-title"}>Models</h3>
                            </Col>
                            <Col xs={8} lg={8}>
                                <div className={"dashboard-sort"}>
                                    <SearchSort dispatch={dispatch} ontologies={ontologies}
                                                filter={filter}
                                                loading={loading}
                                                changeFilters={this.changeDomainFilter}
                                                handleSortClick={this.changeSortMethod}
                                                handleViewClick={this.changeViewLevel}
                                                handleFilterType={this.changeFilterText}
                                                handleOrder={this.changeOrdering}
                                                handleFilterText={this.changeFilterText}
                                                upload={upload}
                                    />
                                </div>
                            </Col>
                        </Row>
                        <ModelList models={models} user={auth.user} filter={filter} dispatch={dispatch} ontologies={ontologies}
                                    handleSortClick={this.changeOrderingByColumn}/>
                    </div>
                </div>
            </div>
        );
    }

    ontologyNamesEqual(previous, current) {
        if (previous.length !== current.length) {
            return false;
        }

        // Assumes the arrays are sorted
        for (let i = 0; i < previous.length; i++) {
            if (previous[i].name !== current[i].name) {
                return false;
            }
        }

        return true;
    }

    changeViewLevel(key) {
        this.setState({
            ...this.state,
            filter: {
                ...this.state.filter,
                viewLevel: key
            }
        });
    }

    changeSortMethod(key) {
        this.setState({
            ...this.state,
            filter: {
                ...this.state.filter,
                sortMethod: key
            }
        });
    }

    changeFilterText(text) {
        this.setState({
            ...this.state,
            filter: {
                ...this.state.filter,
                filterText: text
            }
        });
    }

    changeOrdering(e){
        this.setState({
            ...this.state,
            filter: {
                ...this.state.filter,
                order: !this.state.filter.order
            }
        })
    }

    changeDomainFilter(filters){
        this.setState({
            ...this.state,
            filter: {
                ...this.state.filter,
                domainModelFilters: filters
            }
        });
    }

    changeOrderingByColumn(sort){
        console.log("Clicked")
        this.setState({
            ...this.state,
            filter: {
                ...this.state.filter,
                sortMethod: sort,
                order: !this.state.filter.order
            }
        })
    }


}

var mapStateToProps = function (state) {
    return {
        user: state.dashboard.user,
        models: state.dashboard.models,
        ontologies: state.dashboard.ontologies,
        upload: state.dashboard.upload,
        download: state.dashboard.download,
        loading: state.dashboard.loading,
        auth: state.auth
    };
};

Dashboard.propTypes = {
    user: PropTypes.object,
    dispatch: PropTypes.func,
    ontologies: PropTypes.array,
    models: PropTypes.array,
    upload: PropTypes.object,
    download: PropTypes.object,
    loading: PropTypes.object,
    auth: PropTypes.object,
};

export default connect(mapStateToProps)(Dashboard);
