import React, { Component } from "react";
import {
    Alert,
    Button,
    Col,
    MenuItem,
    Modal,
    Row,
} from "react-bootstrap";
import { connect } from "react-redux";
import { axiosInstance } from "../../../common/rest/rest";
import { updateModel } from "../../../modeller/actions/ModellerActions";

class ShareModelModal extends Component {
    constructor(props) {
        super(props);

        this.state = {
            transfer: {
                username: "",
                modelName: "",
                retainAccess: "write",
            },
            writeAdd: "",
            readAdd: "",
            ownerAdd: "",
            updatedAuthz: {
                readUsernames: [],
                writeUsernames: [],
                ownerUsernames: [],
                noRoleUrl: "",
                readUrl: "",
                writeUrl: "",
                ownerUrl: "",
            },
            error: undefined,
        };

        this.handleAddUser = this.handleAddUser.bind(this);
        this.handleAddWriteUser = this.handleAddWriteUser.bind(this);
        this.handleAddReadUser = this.handleAddReadUser.bind(this);
        this.handleAddOwnerUser = this.handleAddOwnerUser.bind(this);

        this.handleRemoveUser = this.handleRemoveUser.bind(this);
        this.handleRemoveWriteUser = this.handleRemoveWriteUser.bind(this);
        this.handleRemoveReadUser = this.handleRemoveReadUser.bind(this);
        this.handleRemoveOwnerUser = this.handleRemoveOwnerUser.bind(this);
        this.handleTransfer = this.handleTransfer.bind(this);

        this.handleChangeWriteValue = this.handleChangeWriteValue.bind(this);
        this.handleChangeReadValue = this.handleChangeReadValue.bind(this);
        this.handleChangeOwnerValue = this.handleChangeOwnerValue.bind(this);

        this.handleGetAuthz = this.handleGetAuthz.bind(this);
    }
    componentDidMount() {
        let model = this.props.model;
        this.handleGetAuthz(model["id"]);
    }

    handleGetAuthz = (modelId) => {
        axiosInstance.get("/models/" + modelId + "/authz").then(response => {
            this.setState({ updatedAuthz: response.data });
        });
    };

    handlePutAuthz = (modelId, updatedAuthz) => {
        axiosInstance.put("/models/" + modelId + "/authz", updatedAuthz).then(response => {
            //update the state manually to the incoming updatedAuthz
            //also clear the input field(s)
            this.setState({ updatedAuthz: updatedAuthz, writeAdd: "", readAdd: "", ownerAdd: ""});
        })
        .catch((error) => {
            console.log("Error: ", error.response.data);
            this.setState({ error: error.response.data});
        });
    };

    handleAddUser(namesList, nameInput) {
        //copy names array
        let usernames = [...this.state.updatedAuthz[namesList]];

        //get name to add and trim it
        let addName = this.state[nameInput].trim();

        if (usernames.indexOf(addName) > -1) {
            this.setState({
                error: {message: "The username is already on the list"}
            });
        } else if (addName == "") {
            this.setState({
                error: {message: "Please enter a username"}
            });
        } else {
            //name is OK - add it to the list
            usernames.push(addName);

            //copy updatedAuthz array
            let updatedAuthz = {...this.state.updatedAuthz};

            //set usernames to updated usernames
            updatedAuthz[namesList] = usernames;

            //update authz on server
            this.handlePutAuthz(this.props.model.id, updatedAuthz);
        }
    }

    handleAddWriteUser() {
        this.handleAddUser("writeUsernames", "writeAdd");
    }

    handleAddReadUser() {
        this.handleAddUser("readUsernames", "readAdd");
    }

    handleAddOwnerUser() {
        this.handleAddUser("ownerUsernames", "ownerAdd");
    }

    handleRemoveUser(namesList, username) {
        //copy names array
        let usernames = [...this.state.updatedAuthz[namesList]];
        usernames.splice(usernames.indexOf(username), 1);

        //copy updatedAuthz array
        let updatedAuthz = {...this.state.updatedAuthz};

        //set usernames to updated usernames
        updatedAuthz[namesList] = usernames;

        //update authz on server
        this.handlePutAuthz(this.props.model.id, updatedAuthz);
    }

    handleRemoveWriteUser(username) {
        this.handleRemoveUser("writeUsernames", username);
    }

    handleRemoveReadUser(username) {
        this.handleRemoveUser("readUsernames", username);
    }

    handleRemoveOwnerUser(username) {
        this.handleRemoveUser("ownerUsernames", username);
    }

    handleTransfer() {
        let model = this.props.model;
        model.userId = this.state.transfer.username;
        let accessLevel = this.state.transfer.retainAccess;
        switch (accessLevel) {
            case "write":
                model.writeUsernames = [
                    ...model.writeUsernames,
                    this.props.auth.user.username,
                ];
                break;
            case "read":
                model.readUsernames = [
                    ...model.readUsernames,
                    this.props.auth.user.username,
                ];
                break;
            default:
                break;
        }
        this.props.dispatch(updateModel(model["id"], model));
        this.props.onHide();
    }

    handleChangeWriteValue = (event) => {
        this.setState({ writeAdd: event.target.value });
    };

    handleChangeReadValue = (event) => {
        this.setState({ readAdd: event.target.value });
    };

    handleChangeOwnerValue = (event) => {
        this.setState({ ownerAdd: event.target.value });
    };
    handleClose = () =>
        this.setState({
            error: undefined,
        });

    handleKeypressOwner = e => {
        if (e.key === 'Enter') {
            this.btnOwner.click();
        }
    };
    handleKeypressEdit = e => {
        if (e.key === 'Enter') {
            this.btnEdit.click();
        }
    };
    handleKeypressView = e => {
        if (e.key === 'Enter') {
            this.btnView.click();
        }
    };

    render() {
        let { model } = this.props;
        let lastChar = window.env.API_PATH.slice(-1);
        let windowEnv = window.env.API_PATH;
        if (lastChar !== "/") {
            windowEnv = windowEnv + "/"
        }

        let writeUrl = window.location.origin +
            windowEnv +
            "models/" +
            this.state.updatedAuthz.writeUrl +
            "/edit";
        let readUrl = window.location.origin +
            windowEnv +
            "models/" +
            this.state.updatedAuthz.readUrl +
            "/read";

        return (
            <Modal {...this.props}>
                <Modal.Header closeButton>
                    <Modal.Title>Share Model - {model.name}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div className="share-model">
                        <h4>Share model with other users</h4>
                        <div>
                            {this.state.error ? (
                                <Alert bsStyle="danger" dismissible>
                                    <div className="input-form">
                                        <h4>
                                            {this.state.error.message}
                                        </h4>
                                        <Button
                                            bsStyle="primary"
                                            onClick={this.handleClose}
                                        >
                                            OK
                                        </Button>
                                    </div>
                                </Alert>
                            ) : (
                                <div></div>
                            )}
                            <div>
                                <hr />
                                <span className="text-bold">Owner Access:</span>
                                <ul className="list-group">
                                    {this.state.updatedAuthz.ownerUsernames.map(
                                        (ownerUsername, index) => (
                                            <li
                                                key={index}
                                                className="list-group-item borderless"
                                            >
                                                <div className="username-items">
                                                    <MenuItem
                                                        // className = "username-item"
                                                        eventKey="4"
                                                        onClick={() =>
                                                            this.handleRemoveOwnerUser(
                                                                ownerUsername
                                                            )
                                                        }
                                                    >
                                                        <span className="delete fa fa-trash"></span>
                                                    </MenuItem>
                                                    <span className="username-item">
                                                        {ownerUsername}
                                                    </span>
                                                </div>
                                            </li>
                                        )
                                    )}
                                </ul>
                                <Row>
                                    <Col xs={7} md={7}>
                                        <div className="input-group">
                                            <input
                                                type="text"
                                                className="form-control"
                                                value={this.state.ownerAdd}
                                                onChange={
                                                    this.handleChangeOwnerValue
                                                }
                                                onKeyPress={this.handleKeypressOwner}
                                            />
                                            <span className="input-group-btn">
                                                <button
                                                    className="btn btn-primary"
                                                    type="button"
                                                    ref={node => (this.btnOwner = node)}
                                                    onClick={
                                                        this.handleAddOwnerUser
                                                    }
                                                >
                                                    Add User
                                                </button>
                                            </span>
                                        </div>
                                    </Col>
                                </Row>
                            </div>
                            <div>
                                <hr />
                                <span className="text-bold">Edit Access:</span>
                                <ul className="list-group">
                                    {this.state.updatedAuthz.writeUsernames.map(
                                        (writeUsername, index) => (
                                            <li
                                                key={index}
                                                className="list-group-item borderless"
                                            >
                                                <div className="username-items">
                                                    <MenuItem
                                                        // className = "username-item"
                                                        eventKey="4"
                                                        onClick={() =>
                                                            this.handleRemoveWriteUser(
                                                                writeUsername
                                                            )
                                                        }
                                                    >
                                                        <span className="delete fa fa-trash"></span>
                                                    </MenuItem>
                                                    <span className="username-item">
                                                        {writeUsername}
                                                    </span>
                                                </div>
                                            </li>
                                        )
                                    )}
                                </ul>
                                <Row>
                                    <Col xs={7} md={7}>
                                        <div className="input-group">
                                            <input
                                                type="text"
                                                className="form-control"
                                                value={this.state.writeAdd}
                                                onChange={
                                                    this.handleChangeWriteValue
                                                }
                                                onKeyPress={this.handleKeypressEdit}
                                            />

                                            <span className="input-group-btn">
                                                <button
                                                    className="btn btn-primary"
                                                    type="button"
                                                    ref={node => (this.btnEdit = node)}
                                                    onClick={
                                                        this.handleAddWriteUser
                                                    }
                                                >
                                                    Add User
                                                </button>
                                            </span>
                                        </div>
                                    </Col>
                                </Row>
                            </div>
                            <div>
                                <hr />
                                <span className="text-bold">
                                    View-only Access:
                                </span>
                                <ul className="list-group">
                                    {this.state.updatedAuthz.readUsernames.map(
                                        (readUsername, index) => (
                                            <li
                                                key={index}
                                                className="list-group-item borderless"
                                            >
                                                <div className="username-items">
                                                    <MenuItem
                                                        // className = "username-item"
                                                        eventKey="4"
                                                        onClick={() =>
                                                            this.handleRemoveReadUser(
                                                                readUsername
                                                            )
                                                        }
                                                    >
                                                        <span className="delete fa fa-trash"></span>
                                                    </MenuItem>
                                                    <span className="username-item">
                                                        {readUsername}
                                                    </span>
                                                </div>
                                            </li>
                                        )
                                    )}
                                </ul>
                                <Row>
                                    <Col xs={7} md={7}>
                                        <div className="input-group">
                                            <input
                                                type="text"
                                                className="form-control"
                                                value={this.state.readAdd}
                                                onChange={
                                                    this.handleChangeReadValue
                                                }
                                                onKeyPress={this.handleKeypressView}
                                            />
                                            <span className="input-group-btn">
                                                <button
                                                    className="btn btn-primary"
                                                    type="button"
                                                    ref={node => (this.btnView = node)}
                                                    onClick={
                                                        this.handleAddReadUser
                                                    }
                                                >
                                                    Add User
                                                </button>
                                            </span>
                                        </div>
                                    </Col>
                                </Row>
                            </div>
                        </div>
                        <hr />
                        <div>
                            <div style={{ paddingBottom: "10px" }}>
                                <h4>
                                    Share model with people who are not logged
                                    in
                                </h4>
                                <span className="delete">
                                    WARNING: anyone with one of these links can
                                    see your model
                                </span>
                            </div>
                            <div className="url-container">
                                <span className="text-bold">Edit Access:</span>
                                <div className="input-group">
                                    <input
                                        className="form-control url-share-text"
                                        value={writeUrl}
                                        type="text"
                                        readOnly
                                    />
                                    <span className="input-group-btn">
                                        <MenuItem
                                            className="copy-url"
                                            onClick={() => {
                                                navigator.clipboard.writeText(writeUrl);
                                            }}
                                        >
                                            <i className="fa fa-copy"></i>
                                        </MenuItem>
                                    </span>
                                </div>
                            </div>
                            <div className="url-container">
                                <span className="text-bold url-header">
                                    View-only Access:
                                </span>
                                <div className="input-group">
                                    <input
                                        className="form-control url-share-text"
                                        value={readUrl}
                                        type="text"
                                        readOnly
                                    />
                                    <span className="input-group-btn">
                                        <MenuItem
                                            className="copy-url"
                                            onClick={() => {
                                                navigator.clipboard.writeText(readUrl);
                                            }}
                                        >
                                            <i className="fa fa-copy"></i>
                                        </MenuItem>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </Modal.Body>
            </Modal>
        );
    }
}

let mapStateToProps = function (state) {
    return {
        auth: state.auth,
    };
};

export default connect(mapStateToProps)(ShareModelModal);
