import PropTypes from "prop-types";
import React, { Component } from "react";
import { Col, Nav, NavItem, Row, Tab } from "react-bootstrap";
import { connect } from "react-redux";
import Banner from "../../common/components/banner/Banner";
import { getUsers } from "../actions/api";
import "../styles/index.scss";
import UserTable from "./userTable/UserTable";

class Admin extends Component {
    constructor(props) {
        super(props);

        this.state = {
            modals: {
                invite: false,
            },
        };

        this.toggleModal = this.toggleModal.bind(this);
    }

    componentWillMount() {
        this.props.dispatch(getUsers());
    }

    toggleModal(modalName, flag) {
        console.log("toggleModal:", modalName, flag);
        let modals = this.state.modals;
        modals[modalName] = flag;
        this.setState({
            ...this.state,
            modals: modals,
        });
    }

    render() {
        let { userList } = this.props.admin;

        return (
            <div className="admin content">
                <div className="admin-container">
                    <Banner title="Administration" options={[]} />
                    <Tab.Container
                        id="admin-tabs"
                        defaultActiveKey="1"
                        style={{ overflow: "auto", height: "100%" }}
                    >
                        <Row className="clearfix">
                            <Col sm={2} xs={12} style={{ maxWidth: "230px" }}>
                                <Nav bsStyle="pills" stacked>
                                    <NavItem eventKey="1">Users</NavItem>
                                    <NavItem
                                        eventKey="1"
                                        href="/auth/realm-management/"
                                    >
                                        Manage Accounts
                                    </NavItem>
                                </Nav>
                            </Col>
                            <Col sm={10} xs={12}>
                                <Tab.Content animation>
                                    <Tab.Pane eventKey="1">
                                        <UserTable
                                            users={userList}
                                            toggleModal={this.toggleModal}
                                            dispatch={this.props.dispatch}
                                        />
                                    </Tab.Pane>
                                </Tab.Content>
                            </Col>
                        </Row>
                    </Tab.Container>
                </div>
            </div>
        );
    }
}

let mapStateToProps = function (state) {
    return {
        admin: state.admin,
        auth: state.auth,
    };
};

Admin.propTypes = {
    dispatch: PropTypes.func,
};

export default connect(mapStateToProps)(Admin);
