import React, {Component} from "react";
import PropTypes from "prop-types";
import {Modal, Button, Col, Grid, Label, Row} from "react-bootstrap";
import {getDomainUsers, updateDomainUsers} from "../actions/api";

class AccessEditor extends Component {

    constructor() {
        super();

        this.state = {
            isOpen: false,
            activeList: []
        };

        this.addUserToList = this.addUserToList.bind(this);
        this.removeUserFromList = this.removeUserFromList.bind(this);
    }

    componentWillMount() {
        //console.log("componentWillMount");
    }

    componentWillReceiveProps(nextProps) {
        //console.log("componentWillReceiveProps:", nextProps);
        let accessListModal = nextProps.accessListModal;
        
        if (! accessListModal) {
            //console.log("(accessListModal not defined)");
            return;
        }
        
        let {isOpen, domainUri, activeList} = accessListModal;
        
        if (! isOpen) {
            this.setState({
                ...this.state,
                isOpen: false
            });
            return;
        }
        
        //console.log("componentWillReceiveProps: domainUri = " + domainUri);
        if (domainUri && domainUri !== this.props.accessListModal.domainUri) {
            //console.log("componentWillReceiveProps: getDomainUsers for " + domainUri);
            this.props.dispatch(getDomainUsers(domainUri));
            return;
        }
       
        if (activeList !== this.props.accessListModal.activeList) {
            //console.log("Setting initial active list:", activeList);
            this.setState({
                ...this.state,
                isOpen: isOpen && domainUri !== undefined,
                activeList: activeList
            })
        }
        else {
            this.setState({
                ...this.state,
                isOpen: isOpen && domainUri !== undefined
            })
        }
    }

    addUserToList(username) {
        //console.log("addUserToList:" + username);
        this.setState({
            ...this.state,
            activeList: [...this.state.activeList, username]
        })
    }

    removeUserFromList(username) {
        //console.log("removeUserFromList:" + username);
        this.setState({
            ...this.state,
            activeList: this.state.activeList.filter(u => u !== username)
        })
    }

    render() {
        //console.log("render: this.props, this.state", this.props, this.state);

        let {onHide, accessListModal, dispatch} = this.props;
        let {domainUri, domain, userList} = accessListModal;
        let {isOpen, activeList} = this.state;
        let ontology_label = domain ? domain.label : "";
        
        //console.log("ontology_label:" + ontology_label);
        //console.log("userList:", userList);
        //console.log("activeList:", activeList);

        return (
            <Modal show={isOpen} onHide={onHide} aria-labelledby="contained-modal-title-lg">
                <Modal.Header bsStyle="primary" closeButton>
                    <Modal.Title id="contained-modal-title-lg">{"Access List Editor - " + ontology_label}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Grid fluid style={{margin: 0, width: "100%", height: "100%"}}>
                        <Row className="clearfix">
                            <Col xs={12}>
                                <div style={{maxHeight: "60vh", overflowY: "scroll"}}>
                                    <div style={{backgroundColor: "#ddd", height: "200px", width: "100%"}}>
                                        {activeList.map((u, i) =>
                                            <p key={i}
                                               style={{fontSize: "1.2em", display: "inline-block", margin: "5px 5px"}}>
                                                <Label bsStyle="primary">
                                                    {u}
                                                    <span className="fa fa-close"
                                                          style={{cursor: "pointer", margin: "0 5px"}}
                                                          onClick={() => this.removeUserFromList(u)}/>
                                                </Label>
                                            </p>)}
                                    </div>
                                    <hr/>
                                    <p style={{fontSize: "1.2em"}}>
                                        <span className="text-bold text-primary">Add User to access list: </span>
                                        <select ref="select-user" style={{margin: "0 10px"}}>
                                            <option key={-1} value={-1} disabled={true}>Select a user</option>
                                            {userList.filter(u => activeList.indexOf(u) < 0).map((u, i) => {
                                                return <option value={u} key={i}>{u}</option>
                                            })}
                                        </select>
                                        <Button bsStyle="success" bsSize="xsmall"
                                                disabled={userList.filter(u => activeList.indexOf(u) < 0).length < 1}
                                                onClick={e => this.addUserToList(this.refs["select-user"].value)}>
                                            Add User
                                        </Button>
                                    </p>
                                </div>
                            </Col>
                        </Row>
                    </Grid>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="primary" onClick={this.props.onHide}>Cancel</Button>
                    <Button type="submit" 
                            bsStyle={this.props.accessListModal.activeList !== activeList ? "danger" : "success"} 
                            onClick={() => dispatch(updateDomainUsers(domainUri, activeList))}>Save</Button>
                </Modal.Footer>
            </Modal>
        );
    }

}

AccessEditor.propTypes = {
    accessListModal: PropTypes.object,
    onHide: PropTypes.func,
    dispatch: PropTypes.func
};

export default AccessEditor;
